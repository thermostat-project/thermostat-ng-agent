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

package com.redhat.thermostat.launcher.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandGroupMetadata;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.junit.Test;

import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.Configurations;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;
import com.redhat.thermostat.shared.locale.Translate;

public class PluginConfigurationParserTest {

    @Test(expected = PluginConfigurationParseException.class)
    public void testEmptyConfigurationThrowsException() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n";
        PluginConfigurationParser parser = new PluginConfigurationParser();
        parser.parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));
        fail("should not reach here");
    }

    @Test
    public void testMinimalConfiguration() throws UnsupportedEncodingException {
        PluginConfigurationParser parser = new PluginConfigurationParser();
        String config = "" +
                "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "</plugin>";
        PluginConfiguration result = parser.parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());
        assertEquals(0, result.getNewCommands().size());
    }

    @Test
    public void testConfigurationThatExtendsExistingCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <extensions>\n" +
                "    <extension>\n" +
                "      <name>test</name>\n" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>foo</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>bar</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name><version>1.0</version></bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </extension>\n" +
                "  </extensions>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getNewCommands().size());

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(1, extensions.size());

        CommandExtensions first = extensions.get(0);
        assertEquals("test", first.getCommandName());
        BundleInformation[] expectedBundles = new BundleInformation[] {
                new BundleInformation("foo", "1.0"), new BundleInformation("bar", "1.0"), new BundleInformation("baz", "1.0"),
        };
        assertEquals(Arrays.asList(expectedBundles), first.getBundles());    }

    @Test
    public void testConfigurationThatAddsNewCommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>description</description>\n" +
                "      <command-groups>\n" +
                "        <command-group>group</command-group>\n" +
                "      </command-groups>\n" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>foo</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>bar</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name><version>1.0</version></bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(0, extensions.size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand newCommand = newCommands.get(0);
        assertEquals("test", newCommand.getCommandName());
        assertEquals("summary", newCommand.getSummary());
        assertEquals("description", newCommand.getDescription());
        assertEquals(Collections.singletonList("group"), newCommand.getCommandGroups());
        Options opts = newCommand.getOptions();
        assertTrue(opts.getOptions().isEmpty());
        assertTrue(opts.getRequiredOptions().isEmpty());
        BundleInformation[] expectedBundles = new BundleInformation[] {
                new BundleInformation("foo", "1.0"), new BundleInformation("bar", "1.0"), new BundleInformation("baz", "1.0"),
        };
        assertEquals(Arrays.asList(expectedBundles), newCommand.getBundles());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConfigurationWithSubcommand() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>description</description>\n" +
                "      <subcommands>" +
                "        <subcommand>" +
                "          <name>subcommand</name>" +
                "          <description>subcommand description</description>" +
                "          <options>" +
                "            <option>" +
                "              <long>foo</long>" +
                "              <short>f</short>" +
                "              <argument>bar</argument>" +
                "              <required>true</required>" +
                "              <description>foo argument</description>" +
                "            </option>" +
                "          </options>" +
                "        </subcommand>" +
                "      </subcommands>" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>foo</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>bar</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name><version>1.0</version></bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(0, extensions.size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand newCommand = newCommands.get(0);
        List<PluginConfiguration.Subcommand> subcommands = newCommand.getSubcommands();
        assertThat(subcommands.size(), is(1));
        PluginConfiguration.Subcommand subcommand = subcommands.get(0);
        assertThat(subcommand.getName(), is("subcommand"));
        assertThat(subcommand.getDescription(), is("subcommand description"));
        assertThat(subcommand.getOptions().getOptions().size(), is(1));
        Option option = ((Collection<Option>) subcommand.getOptions().getOptions()).iterator().next();
        assertThat(option.getOpt(), is("f"));
        assertThat(option.getLongOpt(), is("foo"));
        assertThat(option.getArgName(), is("bar"));
        assertThat(option.isRequired(), is(true));
        assertThat(option.getDescription(), is("foo argument"));
    }

    @Test
    public void testConfigurationThatAddsNewCommandWithCommandGroupAndMetadata() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>description</description>\n" +
                "      <command-groups>\n" +
                "        <command-group>group</command-group>\n" +
                "      </command-groups>\n" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>foo</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>bar</symbolic-name><version>1.0</version></bundle>\n" +
                "        <bundle><symbolic-name>baz</symbolic-name><version>1.0</version></bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n" +
                "        <dependency>thermostat-foo</dependency>\n" +
                "      </dependencies>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "  <command-group-metadatas>\n" +
                "    <command-group-metadata>\n" +
                "      <name>group</name>\n" +
                "      <description>Group Name</description>\n" +
                "      <sort-order>5</sort-order>\n" +
                "    </command-group-metadata>\n" +
                "    <command-group-metadata>\n" +
                "      <name>foo</name>\n" +
                "      <description>FooGroup</description>\n" +
                "      <sort-order>7</sort-order>\n" +
                "    </command-group-metadata>\n" +
                "  </command-group-metadatas>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        List<CommandGroupMetadata> metadata = result.getCommandGroupMetadata();
        assertThat(metadata, is(equalTo(Arrays.asList(
                new CommandGroupMetadata("group", "Group Name", 5),
                new CommandGroupMetadata("foo", "FooGroup", 7)
        ))));
    }

    @Test
    public void testSpacesAtStartAndEndAreTrimmed() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <extensions>" +
                "    <extension>\n" +
                "      <name>\ntest   \n</name>\n" +
                "      <bundles>\n" +
                "        <bundle><symbolic-name>\n \t  \nfoo\t \n</symbolic-name><version>ignore</version>\n</bundle>\n" +
                "        <bundle><symbolic-name>\tbar  baz\n</symbolic-name><version>ignore</version></bundle>\n" +
                "        <bundle><symbolic-name>buzz</symbolic-name><version>ignore</version></bundle>\n" +
                "      </bundles>\n" +
                "      <dependencies>\n\t\n\t \t\t\n" +
                "        <dependency>\t\t\t  thermostat-foo\n\t\t\n</dependency>\n" +
                "      </dependencies>\n" +
                "    </extension>\n" +
                "  </extensions>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getNewCommands().size());

        List<CommandExtensions> extensions = result.getExtendedCommands();
        assertEquals(1, extensions.size());

        CommandExtensions first = extensions.get(0);
        assertEquals("test", first.getCommandName());
        BundleInformation[] expectedBundles = new BundleInformation[] {
                new BundleInformation("foo", "ignore"), new BundleInformation("bar  baz", "ignore"), new BundleInformation("buzz", "ignore"),
        };
        assertEquals(Arrays.asList(expectedBundles), first.getBundles());
    }

    @Test
    public void testConfigurationParsePluginID() throws IOException {
        String pluginID = "com.redhat.thermostat.simple";
        String configName = "config.conf";
        String config = "<?xml version=\"1.0\"?>\n"
                + "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n"
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n"
                + "  <id>" + pluginID + "</id>\n"
                + "  <configurations>"
                + "    <configuration>" + configName + "</configuration>\n"
                + "  </configurations>"
                + "</plugin>\n";

        PluginConfiguration result = new PluginConfigurationParser().parse("test",
                new ByteArrayInputStream(config.getBytes("UTF-8")));

        String resPluginID = result.getPluginID().getPluginID();

        assertTrue(pluginID.equals(resPluginID));
    }

    @Test
    public void testConfigurationParseConfigurations() throws IOException {
        String pluginID = "com.redhat.thermostat.simple";
        String configName = "config.conf";
        String config = "<?xml version=\"1.0\"?>\n"
                + "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n"
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n"
                + "  <id>" + pluginID + "</id>\n"
                + "  <configurations>"
                + "    <configuration>" + configName + "</configuration>\n"
                + "  </configurations>"
                + "</plugin>\n";

        PluginConfiguration result = new PluginConfigurationParser().parse("test",
                new ByteArrayInputStream(config.getBytes("UTF-8")));
        Configurations resConf = result.getConfigurations();

        assertTrue(resConf.containsFile(configName));
    }

    @Test
    public void testConfigurationParseMultipleConfigurations() throws IOException {
        String pluginID = "com.redhat.thermostat.simple";
        String configNameOne = "a.conf";
        String configNameTwo = "b.conf";
        String configNameThree = "c.conf";
        String config = "<?xml version=\"1.0\"?>\n"
                + "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n"
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n"
                + "  <id>" + pluginID + "</id>\n"
                + "  <configurations>"
                + "    <configuration>" + configNameOne + "</configuration>\n"
                + "    <configuration>" + configNameTwo + "</configuration>\n"
                + "    <configuration>" + configNameThree + "</configuration>\n"
                + "  </configurations>"
                + "</plugin>\n";

        PluginConfiguration result = new PluginConfigurationParser().parse("test",
                new ByteArrayInputStream(config.getBytes("UTF-8")));
        Configurations resConf = result.getConfigurations();

        assertTrue(resConf.containsFile(configNameOne));
        assertTrue(resConf.containsFile(configNameTwo));
        assertTrue(resConf.containsFile(configNameThree));
    }

    @Test
    public void testConfigurationWithNoPluginID() throws IOException {
        String configName = "d.conf";
        String config = "<?xml version=\"1.0\"?>\n"
                + "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n"
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n"
                + "  <configurations>"
                + "    <configuration>" + configName + "</configuration>\n"
                + "  </configurations>"
                + "</plugin>\n";

        PluginConfiguration result = new PluginConfigurationParser().parse("test",
                new ByteArrayInputStream(config.getBytes("UTF-8")));
        Configurations resConf = result.getConfigurations();
        assertTrue(!resConf.containsFile(configName));

    }

    @Test
    public void testSummaryIsReadCorrectly() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>some summary</summary>\n" +
                "      <description>some description</description>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("some summary", command.getSummary());
    }

    @Test
    public void testNewLinesAreRemovedFromDescription() throws UnsupportedEncodingException {
        String newLine = System.lineSeparator();
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>  Line 1.  " + newLine + "Line 2. Line 3." + newLine + "Line 4.</description>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("Line 1. Line 2. Line 3. Line 4.", command.getDescription());
    }

    @Test
    public void testArgumentParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>just a test</description>\n" +
                "      <arguments>\n" +
                "        <argument>file</argument>\n" +
                "      </arguments>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("just a test", command.getDescription());
        assertEquals(null, command.getUsage());
        Options opts = command.getOptions();
        assertTrue(opts.getOptions().isEmpty());

        List<String> args = command.getPositionalArguments();
        assertEquals(1, args.size());
        assertEquals("file", args.get(0));
    }

    @Test
    public void testOptionParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <group>\n" +
                "          <required>true</required>\n" +
                "          <option>\n" +
                "            <long>exclusive-a</long>\n" +
                "            <short>a</short>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option a</description>\n" +
                "          </option>\n" +
                "          <option>\n" +
                "            <long>exclusive-b</long>\n" +
                "            <short>b</short>\n" +
                "            <required>false</required>\n" +
                "            <description>exclusive option b</description>\n" +
                "          </option>\n" +
                "        </group>\n" +
                "        <option>\n" +
                "          <long>long</long>\n" +
                "          <short>l</short>\n" +
                "          <argument>name</argument>\n" +
                "          <required>true</required>\n" +
                "          <description>some required and long option</description>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
        assertEquals("just a test", command.getDescription());
        Options opts = command.getOptions();
        assertNull(opts.getOption("foobarbaz"));

        Option requiredOption = opts.getOption("l");
        assertNotNull(requiredOption);

        Option exclusiveOptionA = opts.getOption("a");
        assertNotNull(exclusiveOptionA);
        assertEquals("exclusive-a", exclusiveOptionA.getLongOpt());
        assertFalse(exclusiveOptionA.hasArg());
        assertFalse(exclusiveOptionA.isRequired());
        assertEquals("exclusive option a", exclusiveOptionA.getDescription());

        Option exclusiveOptionB = opts.getOption("b");
        assertNotNull(exclusiveOptionB);
        assertEquals("exclusive-b", exclusiveOptionB.getLongOpt());
        assertFalse(exclusiveOptionB.hasArg());
        assertFalse(exclusiveOptionB.isRequired());
        assertEquals("exclusive option b", exclusiveOptionB.getDescription());

        OptionGroup group = opts.getOptionGroup(exclusiveOptionA);
        assertTrue(group.isRequired());
    }

    @Test
    public void testCommonOptionParsing() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <option common=\"true\">\n" +
                "          <long>dbUrl</long>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8"))    );

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);

        Options opts = command.getOptions();
        assertTrue(opts.getRequiredOptions().isEmpty());
    }

    @Test
    public void testFakeCommonOptionIsIgnored() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\">\n" +
                "  <commands>\n" +
                "    <command>\n" +
                "      <name>test</name>\n" +
                "      <summary>summary</summary>\n" +
                "      <description>just a test</description>\n" +
                "      <options>\n" +
                "        <option common=\"true\">\n" +
                "          <long>foobarbaz</long>\n" +
                "        </option>\n" +
                "      </options>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);

        Options opts = command.getOptions();
        assertTrue(opts.getRequiredOptions().isEmpty());

        Option dbUrlOption = opts.getOption("foobarbaz");
        assertNull(dbUrlOption);
    }

    @Test
    public void testUsesFileWithTrueEntry() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>some summary</summary>\n" +
                "      <description>some description</description>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
    }

    @Test
    public void testUsesFileWithFalseEntry() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>some summary</summary>\n" +
                "      <description>some description</description>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
    }

    @Test
    public void testUsesFileWithInvalidEntry() throws UnsupportedEncodingException {
        String config = "<?xml version=\"1.0\"?>\n" +
                "<plugin>\n" +
                "  <commands>\n" +
                "    <command type='provides'>\n" +
                "      <name>test</name>\n" +
                "      <summary>some summary</summary>\n" +
                "      <description>some description</description>\n" +
                "    </command>\n" +
                "  </commands>\n" +
                "</plugin>";

        PluginConfiguration result = new PluginConfigurationParser()
                .parse("test", new ByteArrayInputStream(config.getBytes("UTF-8")));

        assertEquals(0, result.getExtendedCommands().size());

        List<NewCommand> newCommands = result.getNewCommands();
        assertEquals(1, newCommands.size());

        NewCommand command = newCommands.get(0);
        assertEquals("test", command.getCommandName());
    }

}

