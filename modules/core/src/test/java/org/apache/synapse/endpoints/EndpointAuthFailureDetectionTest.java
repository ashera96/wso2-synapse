/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.oauth.OAuthHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Edge cases for {@link AbstractEndpoint#isEndpointAuthFailure(MessageContext)}, which failover and load balance
 * endpoints use to decide that a token failure (303003) of a child must not be retried.
 */
public class EndpointAuthFailureDetectionTest {

    /**
     * The auth failure error code is detected on every endpoint type that can act as a parent or a child.
     */
    @Test
    public void testAuthFailureErrorCodeIsDetected() {

        MessageContext synCtx = createMessageContext();
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.ENDPOINT_AUTH_FAILURE);

        Assert.assertTrue(new FailoverEndpoint().isEndpointAuthFailure(synCtx));
        Assert.assertTrue(new LoadbalanceEndpoint().isEndpointAuthFailure(synCtx));
        Assert.assertTrue(new HTTPEndpoint().isEndpointAuthFailure(synCtx));
        Assert.assertTrue(new OAuthConfiguredHTTPEndpoint(Mockito.mock(OAuthHandler.class)).isEndpointAuthFailure(synCtx));
    }

    /**
     * A message without an error code is not an auth failure.
     */
    @Test
    public void testMissingErrorCodeIsNotAuthFailure() {

        Assert.assertFalse(new FailoverEndpoint().isEndpointAuthFailure(createMessageContext()));
    }

    /**
     * Other endpoint error codes, such as transport faults and timeouts, are not auth failures, so failover and
     * load balancing still retry them.
     */
    @Test
    public void testOtherErrorCodesAreNotAuthFailure() {

        FailoverEndpoint endpoint = new FailoverEndpoint();
        MessageContext synCtx = createMessageContext();
        int[] otherCodes = {SynapseConstants.NHTTP_CONNECTION_FAILED, SynapseConstants.NHTTP_CONNECTION_TIMEOUT,
                SynapseConstants.NHTTP_CONNECT_TIMEOUT, SynapseConstants.ENDPOINT_FO_NONE_READY,
                SynapseConstants.ENDPOINT_LB_NONE_READY, SynapseConstants.ENDPOINT_AUTH_FAILURE + 1, 0, -1};
        for (int code : otherCodes) {
            synCtx.setProperty(SynapseConstants.ERROR_CODE, code);
            Assert.assertFalse("Error code " + code + " must not be an auth failure",
                    endpoint.isEndpointAuthFailure(synCtx));
        }
    }

    /**
     * Only an Integer error code is matched. A value of another type, such as a String or Long with the same digits,
     * is not treated as an auth failure and does not cause a ClassCastException.
     */
    @Test
    public void testNonIntegerErrorCodeIsNotAuthFailure() {

        FailoverEndpoint endpoint = new FailoverEndpoint();
        MessageContext synCtx = createMessageContext();

        synCtx.setProperty(SynapseConstants.ERROR_CODE, String.valueOf(SynapseConstants.ENDPOINT_AUTH_FAILURE));
        Assert.assertFalse(endpoint.isEndpointAuthFailure(synCtx));

        synCtx.setProperty(SynapseConstants.ERROR_CODE, (long) SynapseConstants.ENDPOINT_AUTH_FAILURE);
        Assert.assertFalse(endpoint.isEndpointAuthFailure(synCtx));
    }

    /**
     * The check reads the current error code only. When a later transport fault overwrites an earlier auth failure
     * code, the failure is no longer treated as an auth failure, so failover is not blocked by a stale code.
     */
    @Test
    public void testOverwrittenAuthErrorCodeIsNotAuthFailure() {

        FailoverEndpoint endpoint = new FailoverEndpoint();
        MessageContext synCtx = createMessageContext();
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.ENDPOINT_AUTH_FAILURE);
        Assert.assertTrue(endpoint.isEndpointAuthFailure(synCtx));

        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_FAILED);
        Assert.assertFalse(endpoint.isEndpointAuthFailure(synCtx));

        synCtx.getPropertyKeySet().remove(SynapseConstants.ERROR_CODE);
        Assert.assertFalse(endpoint.isEndpointAuthFailure(synCtx));
    }

    private MessageContext createMessageContext() {

        return new Axis2MessageContext(new org.apache.axis2.context.MessageContext(), new SynapseConfiguration(),
                null);
    }
}
