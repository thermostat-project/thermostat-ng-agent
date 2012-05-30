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

package com.redhat.thermostat.common.cli;

import java.util.Objects;

public class SimpleArgumentSpec implements ArgumentSpec {

    private String name;
    private String description;
    private boolean required;
    private boolean usingAddionalArgument;

    public SimpleArgumentSpec() {
        this(null, null, false, false);
    }

    public SimpleArgumentSpec(String name, String description) {
        this(name, description, false, false);
    }

    public SimpleArgumentSpec(String name, String description, boolean required, boolean usingAdditionalArgument) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.usingAddionalArgument = usingAdditionalArgument;
    }

    @Override
    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public boolean isUsingAdditionalArgument() {
        return usingAddionalArgument;
    }

    public void setUsingAdditionalArgument(boolean usingAddionalArgument) {
        this.usingAddionalArgument = usingAddionalArgument;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object o) {
        if (! (o instanceof SimpleArgumentSpec)) {
            return false;
        }
        SimpleArgumentSpec other = (SimpleArgumentSpec) o;
        return Objects.equals(name, other.name)
                && Objects.equals(description, other.description)
                && usingAddionalArgument == other.usingAddionalArgument
                && required == other.required;
    }

    public int hashCode() {
        return Objects.hashCode(name) ^ Objects.hashCode(description)
                ^ Boolean.valueOf(usingAddionalArgument).hashCode()
                ^ Boolean.valueOf(required).hashCode();
    }
}