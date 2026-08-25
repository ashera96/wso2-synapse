/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Tests for {@link SourceResponseFactory}'s handling of methods that may declare a body
 * (Content-Length/Transfer-Encoding) without one being guaranteed to be consumed downstream.
 * Regression coverage for the request-smuggling fix (issue #18660): OPTIONS must be treated
 * the same way as GET/HEAD/DELETE so that keep-alive is disabled when such a request declares
 * a body, preventing an undrained body from being reparsed as a smuggled request.
 */
public class SourceResponseFactoryTest {

    private boolean isPayloadOptionalMethod(String httpMethod) throws Exception {
        Method method = SourceResponseFactory.class.getDeclaredMethod("isPayloadOptionalMethod", String.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, httpMethod);
    }

    @Test
    public void testOptionsIsPayloadOptional() throws Exception {
        Assert.assertTrue("OPTIONS must be treated as a payload-optional method so a declared but " +
                "undrained body disables keep-alive", isPayloadOptionalMethod("OPTIONS"));
    }

    @Test
    public void testGetHeadDeleteArePayloadOptional() throws Exception {
        Assert.assertTrue(isPayloadOptionalMethod("GET"));
        Assert.assertTrue(isPayloadOptionalMethod("HEAD"));
        Assert.assertTrue(isPayloadOptionalMethod("DELETE"));
    }

    @Test
    public void testEntityEnclosingMethodsAreNotPayloadOptional() throws Exception {
        Assert.assertFalse(isPayloadOptionalMethod("POST"));
        Assert.assertFalse(isPayloadOptionalMethod("PUT"));
        Assert.assertFalse(isPayloadOptionalMethod("PATCH"));
    }
}
