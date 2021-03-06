/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import com.redhat.thermostat.agent.dao.AgentInfoDAO;
import com.redhat.thermostat.agent.internal.AgentInfoDAOImpl.AgentInformationUpdate;
import com.redhat.thermostat.agent.internal.AgentInfoDAOImpl.HttpHelper;
import com.redhat.thermostat.agent.internal.AgentInfoDAOImpl.JsonHelper;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.model.AgentInformation;

public class AgentInfoDAOTest {

    private static final String URL = "http://localhost:26000/api/v100/agent-config/systems/*/agents/1234";
    private static final String SOME_JSON = "{\"some\" : \"json\"}";
    private static final String SOME_OTHER_JSON = "{\"some\" : {\"other\" : \"json\"}}";
    private static final String CONTENT_TYPE = "application/json";

    private AgentInformation info;
    private JsonHelper jsonHelper;
    private HttpHelper httpHelper;
    private StringContentProvider contentProvider;
    private Request request;
    private ContentResponse response;

    @Before
    public void setUp() throws Exception {
        info = new AgentInformation("1234");
        info.setAlive(true);
        info.setStartTime(100);
        info.setStopTime(10);
        
        httpHelper = mock(HttpHelper.class);
        contentProvider = mock(StringContentProvider.class);
        when(httpHelper.createContentProvider(anyString())).thenReturn(contentProvider);
        request = mock(Request.class);
        when(httpHelper.newRequest(anyString())).thenReturn(request);
        response = mock(ContentResponse.class);
        when(response.getStatus()).thenReturn(HttpStatus.OK_200);
        when(request.send()).thenReturn(response);
        
        jsonHelper = mock(JsonHelper.class);
        when(jsonHelper.toJson(anyListOf(AgentInformation.class))).thenReturn(SOME_JSON);
        when(jsonHelper.toJson(any(AgentInformationUpdate.class))).thenReturn(SOME_OTHER_JSON);
    }

    @Test
    public void verifyAddAgentInformation() throws Exception {
        AgentInfoDAO dao = new AgentInfoDAOImpl(httpHelper, jsonHelper);

        dao.addAgentInformation(info);

        verify(httpHelper).newRequest(URL);
        verify(request).method(HttpMethod.POST);
        verify(jsonHelper).toJson(eq(Arrays.asList(info)));
        verify(httpHelper).createContentProvider(SOME_JSON);
        verify(request).content(contentProvider, CONTENT_TYPE);
        verify(request).send();
        verify(response).getStatus();
    }

    @Test
    public void verifyUpdateAgentInformation() throws Exception {
        AgentInfoDAO dao = new AgentInfoDAOImpl(httpHelper, jsonHelper);

        dao.updateAgentInformation(info);

        verify(httpHelper).newRequest(URL);
        verify(request).method(HttpMethod.PUT);
        
        ArgumentCaptor<AgentInformationUpdate> updateCaptor = ArgumentCaptor.forClass(AgentInformationUpdate.class);
        verify(jsonHelper).toJson(updateCaptor.capture());
        AgentInformationUpdate update = updateCaptor.getValue();
        assertEquals(info, update.getInfo());
                
        verify(httpHelper).createContentProvider(SOME_OTHER_JSON);
        verify(request).content(contentProvider, CONTENT_TYPE);
        verify(request).send();
        verify(response).getStatus();
    }

    @Test
    public void verifyRemoveAgentInformation() throws Exception {
        AgentInfoDAO dao = new AgentInfoDAOImpl(httpHelper, jsonHelper);

        dao.removeAgentInformation(info);

        verify(httpHelper).newRequest(URL);
        verify(request).method(HttpMethod.DELETE);
        verify(request).send();
        verify(response).getStatus();
    }

}

