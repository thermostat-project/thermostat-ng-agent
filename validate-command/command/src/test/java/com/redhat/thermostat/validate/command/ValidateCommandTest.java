/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.validate.command;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.plugin.validator.PluginValidator;

public class ValidateCommandTest {
    
    private ValidateCommand cmd;
    private CommandContext ctxt;
    private Arguments mockArgs;
    private Console console;
    private String fileName;
    private List<String> list = new ArrayList<>();
    private ByteArrayOutputStream outputBaos, errorBaos;
    private PrintStream output, error;
        
    @Before
    public void setUp() {
        
        cmd = new ValidateCommand();
        ctxt = mock(CommandContext.class);
        mockArgs = mock(Arguments.class);
        console = mock(Console.class);

        outputBaos = new ByteArrayOutputStream();
        output = new PrintStream(outputBaos);
        
        errorBaos = new ByteArrayOutputStream();
        error = new PrintStream(errorBaos);
        
        when(ctxt.getArguments()).thenReturn(mockArgs);
        when(ctxt.getConsole()).thenReturn(console);
        when(console.getError()).thenReturn(error);
        when(console.getOutput()).thenReturn(output);
        when(mockArgs.getNonOptionArguments()).thenReturn(list);
        
    }
    
    @Test
    public void validateIncorrectFile() throws CommandException, MissingArgumentException {
        fileName = PluginValidator.class.getResource("/incorrectPlugin.xml").getPath().toString();
        list.add(fileName);
        
        cmd.run(ctxt);
        
        String actual = new String(errorBaos.toByteArray());
        String expected = "Validation failed for file " + fileName + "\n\n";
        assertEquals(expected, actual);

        assertEquals("", new String(outputBaos.toByteArray()));
    }

    @Test
    public void validateCorrectFile() throws CommandException, MissingArgumentException {
        fileName = PluginValidator.class.getResource("/correctPlugin.xml").getPath().toString();
        list.add(fileName);
        
        cmd.run(ctxt);
        
        String expected = "Validation successful for file " + fileName + "\n\n";
        String actual = new String(outputBaos.toByteArray());
        assertEquals(expected, actual);
        
        assertEquals("", new String(errorBaos.toByteArray()));
    }
    
    @Test
    public void validateNonExistingFile() throws CommandException, MissingArgumentException {
        fileName = "/nonExistingFile.xml";
        list.add(fileName);
        
        try {
            cmd.run(ctxt);    
        } catch(CommandLineArgumentParseException clpae) {
            // pass
        }
    }
    
    @Test
    public void missingFileAsArgument() throws CommandException, MissingArgumentException {
        
        try {
            cmd.run(ctxt);    
        } catch(CommandLineArgumentParseException clpae) {
            // pass
        }
    }
    
    @Test
    public void testStorageRequired() {
        assertFalse(cmd.isStorageRequired());
    }

}