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

package org.wso2.carbon.identity.authzen.evaluation;

import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;

import java.util.List;

/**
 * Policy decision point.
 * <p>
 * Every evaluation runs for at most one application, chosen in this order: the subject itself when it is an
 * application, the {@code app_id} resource property, then {@code defaultApplicationId} (the calling client's
 * application). With an application, a decision matches what that application's access tokens would carry for
 * the user. Without one, only organization roles count.
 */
public interface EvaluationService {

    /**
     * Evaluates one access request. A subject, resource or application that does not exist results in a deny
     * decision.
     *
     * @param request              Evaluation request.
     * @param tenantDomain         Tenant domain.
     * @param defaultApplicationId Application to evaluate for when the request names none. May be {@code null}.
     * @return Decision.
     * @throws AuthZENException If the request is invalid or the evaluation fails.
     */
    EvaluationDecision evaluate(EvaluationRequest request, String tenantDomain, String defaultApplicationId)
            throws AuthZENException;

    /**
     * Evaluates a batch. Invalid entries and failures are returned as deny decisions that carry an error.
     *
     * @param requests             Evaluation requests with defaults already applied.
     * @param semantic             {@code execute_all} (default), {@code deny_on_first_deny} or
     *                             {@code permit_on_first_permit}.
     * @param tenantDomain         Tenant domain.
     * @param defaultApplicationId Application to evaluate for when a request names none. May be {@code null}.
     * @return Decisions in request order, stopping where the semantic short-circuits.
     * @throws AuthZENException If the semantic is invalid.
     */
    List<EvaluationDecision> evaluate(List<EvaluationRequest> requests, String semantic, String tenantDomain,
                                      String defaultApplicationId) throws AuthZENException;
}
