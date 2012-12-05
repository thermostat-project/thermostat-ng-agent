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

package com.redhat.thermostat.client.swing.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.core.HostInformationService;
import com.redhat.thermostat.client.core.VmInformationService;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.SummaryViewProvider;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.swing.MainWindow;
import com.redhat.thermostat.client.ui.HostInformationController;
import com.redhat.thermostat.client.ui.MainWindowController;
import com.redhat.thermostat.client.ui.SummaryController;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.client.ui.VmInformationController;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.OSGIUtils;

public class UiFacadeFactoryImpl implements UiFacadeFactory {

    private CountDownLatch shutdown = new CountDownLatch(1);

    private List<HostInformationService> hostInformationServices = new ArrayList<>();
    private List<VmInformationService> vmInformationServices = new ArrayList<>();
    private Collection<VMContextAction> contextAction = new ArrayList<>();

    private BundleContext context;
    private ApplicationService appSvc;

    private HostInfoDAO hostInfoDao;
    private VmInfoDAO vmInfoDao;

    private OSGIUtils serviceProvider;
    
    UiFacadeFactoryImpl(OSGIUtils serviceProvider, BundleContext context) {
        this.context = context;
        this.serviceProvider = serviceProvider;
        appSvc = serviceProvider.getService(ApplicationService.class);
    }
    
    public UiFacadeFactoryImpl(BundleContext context) {
        this(OSGIUtils.getInstance(), context);
    }

    @Override
    public void setHostInfoDao(HostInfoDAO hostInfoDao) {
        this.hostInfoDao = hostInfoDao;
    }

    public void setVmInfoDao(VmInfoDAO vmInfoDao) {
        this.vmInfoDao = vmInfoDao;
    }

    @Override
    public MainWindowController getMainWindow() {
        MainView mainView = new MainWindow();
        RegistryFactory registryFactory = new RegistryFactory(context);
        return new MainWindowControllerImpl(appSvc, this, mainView, registryFactory, hostInfoDao, vmInfoDao);
    }

    @Override
    public SummaryController getSummary() {
        SummaryViewProvider viewProvider = serviceProvider.getService(SummaryViewProvider.class);
        return new SummaryController(appSvc, hostInfoDao, vmInfoDao, viewProvider);
    }

    @Override
    public HostInformationController getHostController(HostRef ref) {
        HostInformationViewProvider viewProvider = serviceProvider.getService(HostInformationViewProvider.class);
        return new HostInformationController(this, ref, viewProvider);
    }

    @Override
    public VmInformationController getVmController(VmRef ref) {
        VmInformationViewProvider viewProvider = serviceProvider.getService(VmInformationViewProvider.class);
        return new VmInformationController(this, ref, viewProvider);
    }

    @Override
    public List<VmInformationService> getVmInformationServices() {
        return vmInformationServices;
    }

    @Override
    public void addVmInformationService(VmInformationService vmInfoService) {
        vmInformationServices.add(vmInfoService);
    }

    @Override
    public void removeVmInformationService(VmInformationService vmInfoService) {
        vmInformationServices.remove(vmInfoService);
    }

    @Override
    public Collection<VMContextAction> getVMContextActions() {
        return contextAction;
    }

    @Override
    public void addVMContextAction(VMContextAction service) {
        contextAction.add(service);
    }

    @Override
    public void shutdown() {
        shutdown.countDown();
    }

    @Override
    public void shutdown(int exitCode) {
        // TODO implement returning exit codes
        shutdown();
    }

    @Override
    public void awaitShutdown() throws InterruptedException {
        shutdown.await();
    }

    @Override
    public List<HostInformationService> getHostInformationServices() {
        return hostInformationServices;
    }

    @Override
    public void addHostInformationService(HostInformationService hostInfoService) {
        hostInformationServices.add(hostInfoService);
    }

    @Override
    public void removeHostInformationService(HostInformationService hostInfoService) {
        hostInformationServices.remove(hostInfoService);
    }

}