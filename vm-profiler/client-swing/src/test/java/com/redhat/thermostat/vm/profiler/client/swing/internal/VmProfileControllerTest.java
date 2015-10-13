/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.model.VmInfo.AliveStatus;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.Profile;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.ProfileAction;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileInfo;
import com.redhat.thermostat.vm.profiler.common.ProfileRequest;
import com.redhat.thermostat.vm.profiler.common.ProfileStatusChange;

public class VmProfileControllerTest {

    private static final String AGENT_ID = "some-agent-id";
    private static final String AGENT_HOST = "foo";
    private static final int AGENT_PORT = 10;
    private static final InetSocketAddress AGENT_ADDRESS = new InetSocketAddress(AGENT_HOST, AGENT_PORT);
    private static final String VM_ID = "some-vm-id";
    private static final String PROFILE_ID = "some-profile-id";

    private static final long SOME_TIMESTAMP = 1000000000;
    private static final long PROFILE_TIMESTAMP = SOME_TIMESTAMP - 100;

    private Timer timer;
    private ApplicationService appService;
    private ProgressNotifier notifier;
    private AgentInfoDAO agentInfoDao;
    private VmInfoDAO vmInfoDao;
    private ProfileDAO profileDao;
    private RequestQueue queue;
    private Clock clock;
    private VmProfileView view;
    private VmRef vm;

    private VmProfileController controller;
    private AgentId agentId;
    private VmInfo vmInfo;

    @Before
    public void setUp() {
        timer = mock(Timer.class);

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);

        appService = mock(ApplicationService.class);
        when(appService.getTimerFactory()).thenReturn(timerFactory);

        notifier = mock(ProgressNotifier.class);

        agentInfoDao = mock(AgentInfoDAO.class);
        vmInfoDao = mock(VmInfoDAO.class);
        vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive(any(AgentInformation.class))).thenReturn(AliveStatus.RUNNING);
        when(vmInfoDao.getVmInfo(isA(VmRef.class))).thenReturn(vmInfo);
        profileDao = mock(ProfileDAO.class);
        queue = mock(RequestQueue.class);

        clock = mock(Clock.class);
        view = mock(VmProfileView.class);

        agentId = new AgentId(AGENT_ID);
        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(AGENT_ID);

        vm = mock(VmRef.class);
        when(vm.getHostRef()).thenReturn(hostRef);
        when(vm.getVmId()).thenReturn(VM_ID);

        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAlive(true);
        agentInfo.setConfigListenAddress(AGENT_HOST + ":" + AGENT_PORT);
        when(agentInfoDao.getAgentInformation(agentId)).thenReturn(agentInfo);
    }

    @Test
    public void timerRunsWhenVisible() throws Exception {
        controller = createController();

        verify(timer, never()).start();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, Action.VISIBLE));
        verify(timer).start();
    }


    @Test
    public void timerStopsWhenHidden() throws Exception {
        controller = createController();

        verify(timer, never()).start();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<Enum<?>>(view, Action.HIDDEN));
        verify(timer).stop();
    }

    @Test
    public void timerUpdatesView() throws Exception {
        when(clock.getRealTimeMillis()).thenReturn(SOME_TIMESTAMP);
        controller = createController();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        ProfileInfo profile = new ProfileInfo(AGENT_ID, VM_ID, PROFILE_TIMESTAMP, PROFILE_ID);

        when(profileDao.getAllProfileInfo(vm,
                new Range<>(SOME_TIMESTAMP - TimeUnit.DAYS.toMillis(1), SOME_TIMESTAMP)))
            .thenReturn(Arrays.asList(profile));

        ProfileStatusChange status = new ProfileStatusChange(AGENT_ID, VM_ID, PROFILE_TIMESTAMP, false);
        when(profileDao.getLatestStatus(vm)).thenReturn(status);

        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(view).setAvailableProfilingRuns(listCaptor.capture());
        List<Profile> resultList = listCaptor.getValue();
        assertEquals(1, resultList.size());
        assertEquals(PROFILE_TIMESTAMP, resultList.get(0).timeStamp);

        verify(view, times(2)).setViewControlsEnabled(true);
    }

    @Test
    public void timerDisablesViewActionsForDeadVMs() throws Exception {
        when(clock.getRealTimeMillis()).thenReturn(SOME_TIMESTAMP);
        controller = createController();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        when(vmInfo.isAlive(isA(AgentInformation.class))).thenReturn(AliveStatus.EXITED);

        Runnable runnable = runnableCaptor.getValue();
        runnable.run();

    }

    @Test
    public void startProfilingWorks() throws Exception {
        controller = createController();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addProfileActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, ProfileAction.START_PROFILING));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(queue).putRequest(requestCaptor.capture());
        Request expectedRequest = ProfileRequest.create(AGENT_ADDRESS, VM_ID, ProfileRequest.START_PROFILING);
        Request actualRequest = requestCaptor.getValue();
        assertRequestEquals(actualRequest, expectedRequest);

        verify(view).setProfilingState(VmProfileView.ProfilingState.STARTING);
    }

    @Test
    public void startProfilingWaitsForDaoResultToEnableViewControls() {
        controller = createController();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addProfileActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, ProfileAction.START_PROFILING));

        ProfileStatusChange status = new ProfileStatusChange(AGENT_ID, VM_ID, PROFILE_TIMESTAMP, true);
        when(profileDao.getLatestStatus(vm)).thenReturn(status);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        runnableCaptor.getValue().run();

        verify(view, times(2)).setProfilingState(VmProfileView.ProfilingState.STARTING);
    }

    @Test
    public void stopProfilingWorks() throws Exception {
        controller = createController();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addProfileActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, ProfileAction.STOP_PROFILING));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(queue).putRequest(requestCaptor.capture());

        Request expectedRequest = ProfileRequest.create(AGENT_ADDRESS, VM_ID, ProfileRequest.STOP_PROFILING);
        Request actualRequest = requestCaptor.getValue();

        assertRequestEquals(actualRequest, expectedRequest);

        verify(view).setProfilingState(VmProfileView.ProfilingState.STOPPING);
    }

    @Test
    public void stopProfilingWaitsForDaoResultToEnableViewControls() {
        controller = createController();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addProfileActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, ProfileAction.STOP_PROFILING));

        verify(view).setProfilingState(VmProfileView.ProfilingState.STOPPING);

        ProfileStatusChange status = new ProfileStatusChange(AGENT_ID, VM_ID, PROFILE_TIMESTAMP, false);
        when(profileDao.getLatestStatus(vm)).thenReturn(status);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        runnableCaptor.getValue().run();

        verify(view, times(2)).setProfilingState(VmProfileView.ProfilingState.STOPPING);
    }

    @Test
    public void selectingAProfileShowsDetails() throws Exception {
        final String PROFILE_DATA = "1 foo";

        controller = createController();

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addProfileActionListener(listenerCaptor.capture());

        Profile PROFILE = new Profile(PROFILE_ID, 10);

        when(view.getSelectedProfile()).thenReturn(PROFILE);
        when(profileDao.loadProfileDataById(vm, PROFILE_ID)).thenReturn(new ByteArrayInputStream(PROFILE_DATA.getBytes(StandardCharsets.UTF_8)));

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, ProfileAction.PROFILE_SELECTED));

        verify(view).setProfilingDetailData(isA(ProfilingResult.class));
    }

    private VmProfileController createController() {
        return new VmProfileController(appService, notifier, agentInfoDao, vmInfoDao, profileDao, queue, clock, view, vm);
    }

    private void assertRequestEquals(Request actual, Request expected) {
        assertEquals(expected.getParameterNames(), actual.getParameterNames());
        assertEquals(expected.getReceiver(), actual.getReceiver());
        assertEquals(expected.getType(), actual.getType());
    }

}
