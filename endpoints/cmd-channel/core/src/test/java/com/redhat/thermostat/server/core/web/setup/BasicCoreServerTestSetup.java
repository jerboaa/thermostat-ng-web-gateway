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

package com.redhat.thermostat.server.core.web.setup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.redhat.thermostat.server.core.internal.web.configuration.ServerConfiguration;
import com.redhat.thermostat.server.core.web.CoreServer;
import com.redhat.thermostat.test.util.CoreServerTestUtil;

public class BasicCoreServerTestSetup extends TimedTestSetup {
    private static CoreServer coreServer;
    protected WebSocketClient client;
    private static int port;
    protected final String baseUrl = "ws://localhost:" + port + "/commands/v1/";
    protected final String baseHttpUrl = "http://localhost:" + port
            + "/commands/v1/";

    private static Thread thread;
    private static final AtomicBoolean ready = new AtomicBoolean(false);

    @BeforeClass
    public static void setupClassProxyCoreServerTestSetup() throws Exception {
        Map<String, String> serverConfig = new HashMap<>(
                CoreServerTestUtil.serverConfiguration);
        serverConfig.put(ServerConfiguration.SECURITY_BASIC.toString(), "true");
        Map<String, String> userConfig = getUserConfig();
        coreServer = new CoreServer();
        coreServer.buildServer(serverConfig,
                Collections.<String, String> emptyMap(), userConfig);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    coreServer.getServer().start();
                    ready.getAndSet(true);
                    coreServer.getServer().join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


        port = coreServer.getPort();
    }

    @AfterClass
    public static void cleanupClassProxyCoreServerTestSetup() throws Exception {
        coreServer.finish();
        thread.join();
    }

    @Before
    public void setupProxyCoreServerTestSetup() throws Exception {
        while (!ready.get()) {
        }
        client = new WebSocketClient();
        client.start();
    }

    protected static Map<String, String> getUserConfig() {
        Map<String, String> userConfig = new HashMap<>();
        userConfig.put("foo-agent-user",
                "agent-pwd,thermostat-commands-provider-testAgent");
        userConfig.put("bar-client-user",
                "client-pwd,thermostat-commands-grant-dump-heap,thermostat-commands-grant-jvm-abc");
        userConfig.put("insufficient-roles-agent", "agent-pwd");
        userConfig.put("insufficient-roles-client",
                "client-pwd,thermostat-commands-grant-dump-heap");
        return userConfig;
    };
}
