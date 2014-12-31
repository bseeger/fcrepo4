/**
 * Copyright 2014 DuraSpace, Inc.
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
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * <p>ValueFormatExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
 */
public class ValueFormatExceptionMapperTest {

    private ValueFormatExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new ValueFormatExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ValueFormatException input =
                new ValueFormatException("Value", PropertyType.NAME, "Test ValueFormatException");
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }

    @Test
    public void testToResponseNullMessage() {
        final ValueFormatException input =
                new ValueFormatException("Value", PropertyType.NAME, null);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}