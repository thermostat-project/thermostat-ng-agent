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

package com.redhat.thermostat.common.storage;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StorageTest {

    private Storage storage;
    private ConnectionKey connKey;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        connKey = new ConnectionKey(){};
        when(storage.createConnectionKey(any(Category.class))).thenReturn(connKey);
    }

    @After
    public void tearDown() {
        storage = null;
        connKey = null;
    }

    @Test
    public void testRegisterCategory() {
        Category category = new Category("testRegisterCategory");
        storage.registerCategory(category);

        verify(storage).createConnectionKey(category);
        assertSame(connKey, category.getConnectionKey());
    }

    @Test(expected=IllegalStateException.class)
    public void testRegisterCategoryTwice() {

        Category category = new Category("test");
        storage.registerCategory(category);
        storage.registerCategory(category);
    }

    @Test(expected=IllegalStateException.class)
    public void testRegisterCategorySameName() {

        Category category1 = new Category("test");
        storage.registerCategory(category1);
        Category category2 = new Category("test");
        storage.registerCategory(category2);
    }
}