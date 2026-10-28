/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authzen.api.rest;

import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.EvaluationResponse;

import java.io.IOException;

/**
 * Access Evaluation API: {@code POST /access/v1/evaluation}.
 */
public class EvaluationServlet extends AuthZENServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected Object handle(byte[] body, CallContext callContext) throws AuthZENException, IOException {

        EvaluationRequest request = OBJECT_MAPPER.readValue(body, EvaluationRequest.class);
        requireBody(request);
        return evaluate(request, callContext);
    }

    static EvaluationResponse evaluate(EvaluationRequest request, CallContext callContext)
            throws AuthZENException {

        EvaluationDecision decision = null;
        try {
            decision = getEvaluationService().evaluate(request, callContext.getTenantDomain(),
                    callContext.getCallerApplicationId());
            return new EvaluationResponse(decision.isDecision(), decision.getContext());
        } finally {
            if (request != null) {
                auditDecision(callContext, "AuthZEN-Evaluate", request.getSubject(),
                        request.getAction() != null ? request.getAction().getName() : null,
                        request.getResource(), decision);
            }
        }
    }
}
