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

package com.redhat.thermostat.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class Thermostat {

    /**
     * the name of the launcher class
     */
    private static final String LAUNCHER_CLASSNAME = "com.redhat.thermostat.common.cli.Launcher";

    private static final String DEBUG_PREFIX = "OSGi Launcher: ";

    private File thermostatBundleHome;
    private boolean printOSGiDebugInfo = false;

    private Thermostat() { /* nothing to do */ }

    private void setUp() throws ConfigurationException {
        Configuration config = new Configuration();
        String thermostatHome = config.getThermostatHome();
        thermostatBundleHome = new File(thermostatHome, "osgi");
    }

    private List<Bundle> installBundles(Framework framework,
                                        List<String>bundleLocations)
        throws Exception
    {
        List<Bundle> bundles = new ArrayList<>();
        BundleContext bundleContext = framework.getBundleContext();
        for (String location : bundleLocations) {
            if (printOSGiDebugInfo) {
                System.out.print(DEBUG_PREFIX + "installing bundle: \"" + location + "\"");
            }
            Bundle bundle = bundleContext.installBundle(location);
            if (printOSGiDebugInfo) {
                System.out.println(" as id " + bundle.getBundleId());
            }

            bundles.add(bundle);
        }
        return bundles;
    }

    private void startBundles(List<Bundle> bundles) throws BundleException {
        for (Bundle bundle : bundles) {
            if (printOSGiDebugInfo) {
                System.out.println(DEBUG_PREFIX + "starting bundle: \"" + bundle.getBundleId() + "\"");
            }
            bundle.start();
        }
    }

    private void start(String[] args) throws Exception {
        setUp();

        ServiceLoader<FrameworkFactory> loader =
                ServiceLoader.load(FrameworkFactory.class);

        Map<String, String> bundleConfigurations = new HashMap<String, String>();
        String extraPackages = OSGiRegistry.getOSGiPublicPackages();
        bundleConfigurations.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
        bundleConfigurations.put(Constants.FRAMEWORK_STORAGE,
                                 thermostatBundleHome.getAbsolutePath());

        Iterator<FrameworkFactory> factories = loader.iterator();
        if (factories.hasNext()) {

            // we just want the first found
            final Framework framework = factories.next().newFramework(bundleConfigurations);
            framework.init();
            List<String> bundles = OSGiRegistry.getSystemBundles();
            List<Bundle> bundleList = installBundles(framework, bundles);
            framework.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        framework.stop();
                        framework.waitForStop(0);
                        if (printOSGiDebugInfo) {
                            System.out.println(DEBUG_PREFIX + "OSGi framework has shut down");
                        }
                    } catch (Exception e) {
                        System.err.println("Error stopping framework:" + e);
                    }
                }
            });

            startBundles(bundleList);

            launch(args, framework);
        } else {
            throw new InternalError("Can't find factories for ServiceLoader!");
        }
    }

    // This is our ticket into OSGi land. Unfortunately, we need the reflection to overcome
    // classpath impendance mismatch.
    private void launch(String[] args, Framework framework)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        BundleContext ctx = framework.getBundleContext();
        ServiceReference launcherRef = ctx.getServiceReference(LAUNCHER_CLASSNAME);
        if (launcherRef != null) {
            Object launcherImpl = ctx.getService(launcherRef);
            Method m = launcherImpl.getClass().getMethod("run", String[].class);
            if (printOSGiDebugInfo) {
                System.out.println(DEBUG_PREFIX + "invoking " + launcherImpl.getClass().getName() + "." + m.getName());
            }
            m.invoke(launcherImpl, (Object) args);
        } else {
            System.err.println("Severe: Could not locate launcher");
        }
    }

    private void setPrintOSGiDebugInfo(boolean newValue) {
        printOSGiDebugInfo = newValue;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Thermostat t = new Thermostat();
        List<String> toProcess = new ArrayList<>(Arrays.asList(args));
        Iterator<String> iter = toProcess.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (("--print-osgi-info").equals(arg)) {
                t.setPrintOSGiDebugInfo(true);
                iter.remove();
            }
        }

        t.start(toProcess.toArray(new String[0]));
    }


}