/*
 * Copyright 2012 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.client.internal;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.InvalidSyntaxException;

import com.redhat.thermostat.client.internal.MainView.Action;
import com.redhat.thermostat.client.internal.ui.swing.AboutDialog;
import com.redhat.thermostat.client.osgi.service.HostDecorator;
import com.redhat.thermostat.client.osgi.service.HostFilter;
import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.osgi.service.VmDecorator;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.osgi.service.VmFilter;
import com.redhat.thermostat.client.ui.AgentConfigurationController;
import com.redhat.thermostat.client.ui.AgentConfigurationModel;
import com.redhat.thermostat.client.ui.AgentConfigurationView;
import com.redhat.thermostat.client.ui.ClientConfigurationController;
import com.redhat.thermostat.client.ui.ClientConfigurationView;
import com.redhat.thermostat.client.ui.HostInformationController;
import com.redhat.thermostat.client.ui.HostVmFilter;
import com.redhat.thermostat.client.ui.MainWindowController;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.DefaultHostsVMsLoader;
import com.redhat.thermostat.common.HostsVMsLoader;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.utils.keyring.Keyring;

public class MainWindowControllerImpl implements MainWindowController {

    private static final Logger logger = LoggingUtils.getLogger(MainWindowControllerImpl.class);

    private final CopyOnWriteArrayList<HostFilter> hostFilters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<VmFilter> vmFilters = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<VmDecorator> vmTreeDecorators = new CopyOnWriteArrayList<>();

    private Timer backgroundUpdater;

    private MainView view;

    private final HostInfoDAO hostsDAO;
    private final VmInfoDAO vmsDAO;

    private ApplicationInfo appInfo;

    private UiFacadeFactory facadeFactory;

    private MenuRegistry menuRegistry;
    private ActionListener<ThermostatExtensionRegistry.Action> menuListener =
            new ActionListener<ThermostatExtensionRegistry.Action>()
    {
        @Override
        public void actionPerformed(
            ActionEvent<ThermostatExtensionRegistry.Action> actionEvent) {
            MenuAction action = (MenuAction) actionEvent.getPayload();

            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED:
                view.addMenu(action);
                break;

            case SERVICE_REMOVED:
                view.removeMenu(action);
                break;

            default:
                logger.log(Level.WARNING, "received unknown event from MenuRegistry: " +
                                           actionEvent.getActionId());
                break;
            }
        }
    };

    private HostVmFilter searchFilter;
    private VmFilterRegistry vmFilterRegistry;
    private HostFilterRegistry hostFilterRegistry;

    private ActionListener<ThermostatExtensionRegistry.Action> hostFilterListener = new UpdateListAndTree<>(HostFilter.class, hostFilters);
    private ActionListener<ThermostatExtensionRegistry.Action> vmFilterListener = new UpdateListAndTree<>(VmFilter.class, vmFilters);

    private VMTreeDecoratorRegistry decoratorRegistry;

    private ActionListener<ThermostatExtensionRegistry.Action> vmDecoratorListener = new UpdateListAndTree<>(VmDecorator.class, vmTreeDecorators);

    private VMInformationRegistry vmInfoRegistry;
    private ActionListener<ThermostatExtensionRegistry.Action> vmInfoRegistryListener =
            new ActionListener<ThermostatExtensionRegistry.Action> ()
    {
        public void actionPerformed(com.redhat.thermostat.common.ActionEvent<ThermostatExtensionRegistry.Action>
                                    actionEvent)
        {
            updateView();
        };
    };
            
    private boolean showHistory;

    private VmInformationControllerProvider vmInfoControllerProvider;

    public MainWindowControllerImpl(UiFacadeFactory facadeFactory, MainView view, RegistryFactory registryFactory)
    {
        try {
            vmFilterRegistry = registryFactory.createVmFilterRegistry();
            hostFilterRegistry = registryFactory.createHostFilterRegistry();
            decoratorRegistry = registryFactory.createVMTreeDecoratorRegistry();
            menuRegistry = registryFactory.createMenuRegistry();
            vmInfoRegistry = registryFactory.createVMInformationRegistry();
            
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }

        searchFilter = new HostVmFilter();
        hostFilters.add(searchFilter);
        vmFilters.add(searchFilter);
        
        this.facadeFactory = facadeFactory;

        ApplicationContext ctx = ApplicationContext.getInstance();
        DAOFactory daoFactory = ctx.getDAOFactory();
        hostsDAO = daoFactory.getHostInfoDAO();
        vmsDAO = daoFactory.getVmInfoDAO();

        initView(view);

        vmInfoControllerProvider = new VmInformationControllerProvider();

        appInfo = new ApplicationInfo();
        view.setWindowTitle(appInfo.getName());
        initializeTimer();

        updateView();

        menuRegistry.addActionListener(menuListener);
        menuRegistry.start();

        hostFilterRegistry.addActionListener(hostFilterListener);
        hostFilterRegistry.start();

        vmFilterRegistry.addActionListener(vmFilterListener);
        vmFilterRegistry.start();

        decoratorRegistry.addActionListener(vmDecoratorListener);
        decoratorRegistry.start();
        
        vmInfoRegistry.addActionListener(vmInfoRegistryListener);
        vmInfoRegistry.start();
    }

    /**
     * This method is for testing purposes only
     */
    HostVmFilter getSearchFilter() {
        return searchFilter;
    }
    
    /**
     * This method is for testing purposes only
     */ 
    List<VmDecorator> getVmTreeDecorators() {
        return vmTreeDecorators;
    }
    
    /**
     * This method is for testing purposes only
     */
    ActionListener<ThermostatExtensionRegistry.Action> getMenuListener() {
        return menuListener;
    }
    
    private void initializeTimer() {
        ApplicationContext ctx = ApplicationContext.getInstance();
        backgroundUpdater = ctx.getTimerFactory().createTimer();
        backgroundUpdater.setAction(new Runnable() {
            @Override
            public void run() {
                doUpdateTreeAsync();
            }
        });
        backgroundUpdater.setInitialDelay(0);
        backgroundUpdater.setDelay(3);
        backgroundUpdater.setTimeUnit(TimeUnit.SECONDS);
        backgroundUpdater.setSchedulingType(SchedulingType.FIXED_RATE);
    }

    private void startBackgroundUpdates() {
        backgroundUpdater.start();
    }

    public void stopBackgroundUpdates() {
        backgroundUpdater.stop();
    }

    @Override
    public void setHostVmTreeFilter(String filter) {
        this.searchFilter.setFilter(filter);
        doUpdateTreeAsync();
    }

    public void doUpdateTreeAsync() {
        HostsVMsLoader loader = new DefaultHostsVMsLoader(hostsDAO, vmsDAO, !showHistory);
        view.updateTree(hostFilters, vmFilters, Collections.<HostDecorator>emptyList(), vmTreeDecorators, loader);
    }

    private void initView(MainView mainView) {
        this.view = mainView;
        mainView.addActionListener(new ActionListener<MainView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<MainView.Action> evt) {
                MainView.Action action = evt.getActionId();
                switch (action) {
                case VISIBLE:
                    startBackgroundUpdates();
                    break;
                case HIDDEN:
                    stopBackgroundUpdates();
                    break;
                case HOST_VM_SELECTION_CHANGED:
                    updateView();
                    break;
                case HOST_VM_TREE_FILTER:
                    String filter = view.getHostVmTreeFilterText();
                    setHostVmTreeFilter(filter);
                    break;
                case SHOW_AGENT_CONFIG:
                    showAgentConfiguration();
                    break;
                case SHOW_CLIENT_CONFIG:
                    showConfigureClientPreferences();
                    break;
                case SWITCH_HISTORY_MODE:
                    switchHistoryMode();
                    break;
                case SHOW_ABOUT_DIALOG:
                    showAboutDialog();
                    break;
                case SHOW_VM_CONTEXT_MENU:
                    showContextMenu(evt);
                    break;
                case VM_CONTEXT_ACTION:
                    handleVMHooks(evt);
                    break;
                case SHUTDOWN:
                    shutdownApplication();
                    break;
                default:
                    throw new IllegalStateException("unhandled action");
                }
            }

        });
    }

    private void shutdownApplication() {
        menuRegistry.removeActionListener(menuListener);
        menuListener = null;
        menuRegistry.stop();

        view.hideMainWindow();
        ApplicationContext.getInstance().getTimerFactory().shutdown();
        shutdownOSGiFramework();
    }

    private void shutdownOSGiFramework() {
        facadeFactory.shutdown();
    }

    private void showContextMenu(ActionEvent<Action> evt) {
        List<VMContextAction> toShow = new ArrayList<>();
        VmRef vm = (VmRef) view.getSelectedHostOrVm();

        logger.log(Level.INFO, "registering applicable VMContextActions actions to show");

        for (VMContextAction action : facadeFactory.getVMContextActions()) {
            if (action.getFilter().matches(vm)) {
                toShow.add(action);
            }
        }

        view.showVMContextActions(toShow, (MouseEvent)evt.getPayload());
    }

    private void handleVMHooks(ActionEvent<MainView.Action> event) {
        Object payload = event.getPayload();
        if (payload instanceof VMContextAction) { 
            try {
                VMContextAction action = (VMContextAction) payload;
                action.execute((VmRef) view.getSelectedHostOrVm());
            } catch (Throwable error) {
                logger.log(Level.SEVERE, "");
            }
        }
    }
    
    @Override
    public void showMainMainWindow() {
        view.showMainWindow();
    }

    private void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(appInfo);
        aboutDialog.setModal(true);
        aboutDialog.pack();
        aboutDialog.setVisible(true);
    }

    private void showAgentConfiguration() {
        AgentConfigurationSource agentPrefs = new AgentConfigurationSource();
        AgentConfigurationModel model = new AgentConfigurationModel(agentPrefs);
        AgentConfigurationView view = ApplicationContext.getInstance().getViewFactory().getView(AgentConfigurationView.class);
        AgentConfigurationController controller = new AgentConfigurationController(model, view);
        controller.showView();
    }

    private void showConfigureClientPreferences() {
        ClientPreferences prefs = new ClientPreferences(OSGIUtils.getInstance().getService(Keyring.class));
        ClientConfigurationView view = ApplicationContext.getInstance().getViewFactory().getView(ClientConfigurationView.class);
        ClientConfigurationController controller = new ClientConfigurationController(prefs, view);
        controller.showDialog();
    }

    private void switchHistoryMode() {
        showHistory = !showHistory;
        doUpdateTreeAsync();
    }

    private void updateView() {
        // this is quite an ugly method. there must be a cleaner way to do this
        Ref ref = view.getSelectedHostOrVm();

        if (ref == null) {
            SummaryController controller = new SummaryController();
            view.setSubView(controller.getView());
        } else if (ref instanceof HostRef) {
            HostRef hostRef = (HostRef) ref;
            HostInformationController hostController = facadeFactory.getHostController(hostRef);
            view.setSubView(hostController.getView());
            view.setStatusBarPrimaryStatus("host: " + hostRef.getHostName() + ", id: " + hostRef.getAgentId());
        } else if (ref instanceof VmRef) {
            VmRef vmRef = (VmRef) ref;
            VmInformationController vmInformation =
                    vmInfoControllerProvider.getVmInfoController(vmRef);
            view.setSubView(vmInformation.getView());
            view.setStatusBarPrimaryStatus("vm: " + vmRef.getName() + ", pid: " + vmRef.getStringID() +
                                           ", host: " + vmRef.getAgent().getHostName());
        } else {
            throw new IllegalArgumentException("unknown type of ref");
        }
    }

    private class UpdateListAndTree<T> implements ActionListener<ThermostatExtensionRegistry.Action> {

        private final Class<T> extensionClass;
        private final CopyOnWriteArrayList<T> extensionList;

        public UpdateListAndTree(Class<T> extensionClass, CopyOnWriteArrayList<T> addRemoveExtensionsFrom) {
            this.extensionClass = extensionClass;
            this.extensionList = addRemoveExtensionsFrom;
        }

        @Override
        public void actionPerformed(ActionEvent<com.redhat.thermostat.client.internal.ThermostatExtensionRegistry.Action> actionEvent) {

            Object payload = actionEvent.getPayload();
            if (!extensionClass.isInstance(payload)) {
                throw new IllegalArgumentException("invalid payload type");
            }

            T filter = (T) payload;

            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED:
                extensionList.add(filter);
                doUpdateTreeAsync();
                break;

            case SERVICE_REMOVED:
                extensionList.remove(filter);
                doUpdateTreeAsync();
                break;

            default:
                logger.log(Level.WARNING, "received unknown event from ExtensionRegistry: " +
                                           actionEvent.getActionId());
                break;
            }
        }
    }

    private class VmInformationControllerProvider {
        private VmInformationController lastSelectedVM;
        private Map<VmRef, Integer> selectedForVM = new ConcurrentHashMap<>();
        
        VmInformationController getVmInfoController(VmRef vmRef) {
            int id = 0;
            if (lastSelectedVM != null) {
                id = lastSelectedVM.getSelectedChildID();
            }
            
            lastSelectedVM = facadeFactory.getVmController(vmRef);
            if (!lastSelectedVM.selectChildID(id)) {
                Integer _id = selectedForVM.get(vmRef);
                id = _id != null? _id : 0;
                lastSelectedVM.selectChildID(id);
            }

            selectedForVM.put(vmRef, id);
            
            return lastSelectedVM;
        }
    }

}
