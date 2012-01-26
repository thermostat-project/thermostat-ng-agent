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

package com.redhat.thermostat.client.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;

public class SimpleTable {

    public static class Section {
        private final String sectionName;
        private final List<TableEntry> tableEntries = new ArrayList<TableEntry>();

        public Section(String name) {
            this.sectionName = name;
        }

        public String getText() {
            return sectionName;
        }

        public void add(TableEntry entry) {
            tableEntries.add(entry);
        }

        public void add(Key key, List<Value> values) {
            tableEntries.add(new TableEntry(key, values));
        }

        public void add(Key key, Value value) {
            tableEntries.add(new TableEntry(key, value));
        }

        public TableEntry[] getEntries() {
            return tableEntries.toArray(new TableEntry[0]);
        }
    }

    public static class TableEntry {
        private final Key key;
        private final List<Value> values;

        public TableEntry(String key, String value) {
            this(new Key(key), new Value(value));
        }

        public TableEntry(Key key, Value value) {
            this.key = key;
            this.values = new ArrayList<Value>();
            this.values.add(value);
        }

        public TableEntry(Key key, List<Value> values) {
            this.key = key;
            this.values = new ArrayList<Value>(values);
        }

        public Key getKey() {
            return key;
        }

        public Value[] getValues() {
            return values.toArray(new Value[0]);
        }

    }

    public static class Key {
        private final String text;

        public Key(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class Value {
        private final String text;
        private final Component actualComponent;

        public Value(String text) {
            this.text = text;
            this.actualComponent = null;
        }

        public Value(Component component) {
            this.actualComponent = component;
            this.text = null;
        }

        public String getText() {
            return text;
        }
        public Component getComponent() {
            return actualComponent;
        }

    }

    public static JPanel createTable(List<Section> sections) {
        final int SECTION_TOP_GAP = 10;
        final int ROW_VERTICAL_GAP = 0;
        final int ROW_HORIZONTAL_GAP = 10;

        Insets sectionHeaderInsets = new Insets(SECTION_TOP_GAP, 0, 0, 0);
        Insets rowInsets = new Insets(ROW_VERTICAL_GAP, ROW_HORIZONTAL_GAP, ROW_VERTICAL_GAP, ROW_HORIZONTAL_GAP);

        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        GridBagConstraints keyConstraints = new GridBagConstraints();
        GridBagConstraints valueConstraints = new GridBagConstraints();
        GridBagConstraints sectionHeaderConstraints = new GridBagConstraints();

        keyConstraints.insets = valueConstraints.insets = rowInsets;
        keyConstraints.gridy = valueConstraints.gridy = 0;
        keyConstraints.gridx = 0;
        keyConstraints.anchor = GridBagConstraints.FIRST_LINE_END;
        valueConstraints.gridx = 1;
        keyConstraints.fill = valueConstraints.fill = GridBagConstraints.HORIZONTAL;

        sectionHeaderConstraints.gridx = 0;
        sectionHeaderConstraints.gridwidth = GridBagConstraints.REMAINDER;
        sectionHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
        sectionHeaderConstraints.insets = sectionHeaderInsets;

        for (Section section : sections) {
            sectionHeaderConstraints.gridy = keyConstraints.gridy = ++valueConstraints.gridy;
            container.add(Components.header(section.getText()), sectionHeaderConstraints);
            for (TableEntry tableEntry : section.getEntries()) {
                keyConstraints.gridy = ++valueConstraints.gridy;
                container.add(Components.label(tableEntry.getKey().getText()), keyConstraints);

                for (Value value : tableEntry.getValues()) {
                    if (value.getComponent() == null) {
                        container.add(Components.value(value.getText()), valueConstraints);
                    } else {
                        container.add(value.getComponent(), valueConstraints);
                    }
                    keyConstraints.gridy = ++valueConstraints.gridy;
                }
            }
        }

        GridBagConstraints glueConstraints = new GridBagConstraints();
        glueConstraints.gridy = keyConstraints.gridy + 1;
        glueConstraints.gridx = 0;
        glueConstraints.weightx = 1;
        glueConstraints.weighty = 1;
        glueConstraints.fill = GridBagConstraints.BOTH;
        glueConstraints.gridheight = GridBagConstraints.REMAINDER;
        glueConstraints.gridwidth = GridBagConstraints.REMAINDER;
        Component filler = Box.createGlue();
        container.add(filler, glueConstraints);

        return container;
    }

}
