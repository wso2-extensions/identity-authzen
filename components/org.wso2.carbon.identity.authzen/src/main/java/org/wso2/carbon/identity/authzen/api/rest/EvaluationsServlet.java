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

import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.EvaluationResponse;
import org.wso2.carbon.identity.authzen.core.model.EvaluationsRequest;
import org.wso2.carbon.identity.authzen.core.model.EvaluationsResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Access Evaluations API: {@code POST /access/v1/evaluations}. Without an {@code evaluations} array the request
 * is a single evaluation and the response is a single decision.
 */
public class EvaluationsServlet extends AuthZENServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAX_EVALUATIONS = 100;

    @Override
    protected Object handle(byte[] body, CallContext callContext) throws AuthZENException, IOException {

        EvaluationsRequest request = OBJECT_MAPPER.readValue(body, EvaluationsRequest.class);
        requireBody(request);
        if (!request.isBatch()) {
            return EvaluationServlet.evaluate(request.toSingleRequest(), callContext);
        }
        if (request.getEvaluations().size() > MAX_EVALUATIONS) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "A request may contain at most " + MAX_EVALUATIONS + " evaluations.");
        }
        Object semantic = request.getOptions() != null
                ? request.getOptions().get(AuthZENConstants.OPTION_EVALUATIONS_SEMANTIC) : null;
        if (semantic != null && !(semantic instanceof String)) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "evaluations_semantic must be a string.");
        }

        List<EvaluationRequest> evaluations = request.resolveEvaluations();
        List<EvaluationDecision> decisions = getEvaluationService().evaluate(evaluations, (String) semantic,
                callContext.getTenantDomain(), callContext.getCallerApplicationId());
        List<EvaluationResponse> responses = new ArrayList<>(decisions.size());
        for (int i = 0; i < decisions.size(); i++) {
            EvaluationRequest evaluation = evaluations.get(i);
            EvaluationDecision decision = decisions.get(i);
            auditDecision(callContext, "AuthZEN-Evaluate", evaluation.getSubject(),
                    evaluation.getAction() != null ? evaluation.getAction().getName() : null,
                    evaluation.getResource(), decision);
            responses.add(new EvaluationResponse(decision.isDecision(), decision.getContext()));
        }
        return new EvaluationsResponse(responses);
    }
}
