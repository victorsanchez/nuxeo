/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema.types;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

public class TestSimpleType {

    SimpleType simpleType;

    @Before
    public void setUp() {
        simpleType = new SimpleTypeImpl(StringType.INSTANCE, SchemaNames.BUILTIN, "type name");
    }

    @After
    public void tearDown() {
        simpleType = null;
    }

    @Test
    public void testIsPrimitive() {
        assertFalse(simpleType.isPrimitive());
    }

    @Test
    public void testIsSimpleType() {
        assertTrue(simpleType.isSimpleType());
    }

    @Test
    public void testValidateNull() throws Exception {
        assertTrue(simpleType.validate(null));
    }

}
