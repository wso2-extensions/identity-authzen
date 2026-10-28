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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;
import org.wso2.carbon.identity.authzen.core.util.AuthZENUtils;
import org.wso2.carbon.identity.authzen.core.validation.EvaluationRequestValidator;
import org.wso2.carbon.identity.authzen.pip.ApplicationContext;
import org.wso2.carbon.identity.authzen.pip.PIPContext;
import org.wso2.carbon.identity.authzen.pip.PIPService;
import org.wso2.carbon.identity.authzen.pip.ResourceContext;
import org.wso2.carbon.identity.authzen.pip.SubjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvaluationServiceImpl implements EvaluationService {

    private static final Log LOG = LogFactory.getLog(EvaluationServiceImpl.class);

    private static final String STATUS_GRANTED = "200";
    private static final String STATUS_DENIED = "403";
    private static final String NO_APPLICATION = "No application to evaluate for. Pass resource.properties.app_id "
            + "or call with an access token of the application.";

    private final PIPService pipService;
    private final List<PolicyEvaluator> policyEvaluators;

    /**
     * @param pipService       Resolves subjects, resources and applications.
     * @param policyEvaluators Evaluators for the authorization policies. For a policy with several evaluators, the
     *                         one with the lowest priority is used.
     */
    public EvaluationServiceImpl(PIPService pipService, List<PolicyEvaluator> policyEvaluators) {

        this.pipService = pipService;
        this.policyEvaluators = List.copyOf(policyEvaluators);
    }

    @Override
    public EvaluationDecision evaluate(EvaluationRequest request, String tenantDomain, String defaultApplicationId)
            throws AuthZENException {

        EvaluationRequestValidator.validate(request);
        String action = request.getAction().getName();
        PIPContext pipContext;
        try {
            pipContext = resolve(request.getSubject(), request.getResource(), tenantDomain, defaultApplicationId);
        } catch (AuthZENClientException e) {
            if (e.getErrorCode().isDenial()) {
                return deny(e.getDescription());
            }
            throw e;
        }

        ResourceContext resource = pipContext.getResourceContext();
        if (!resource.getScopes().contains(action)) {
            return deny("Action " + action + " is not defined for resource " + resource.getResourceId() + ".");
        }
        ApplicationContext application = pipContext.getApplicationContext();
        if (application == null) {
            // Access in the identity server is always granted to an application, as with tokens.
            return deny(NO_APPLICATION);
        }
        if (!application.isAuthorizedForResource()) {
            return deny("The application is not authorized for resource " + resource.getResourceId() + ".");
        }
        if (!application.getAuthorizedScopes().contains(action)) {
            return deny("The application is not authorized for action " + action + " on resource "
                    + resource.getResourceId() + ".");
        }
        String policy = resolvePolicy(pipContext);
        if (AuthZENConstants.POLICY_NO_POLICY.equalsIgnoreCase(policy)) {
            // The application's authorization for the action, checked above, is the grant.
            return grant("Permitted by the " + policy + " policy.");
        }
        PolicyEvaluator evaluator = getEvaluator(policy);
        if (evaluator == null) {
            return deny("Authorization policy " + policy + " is not supported.");
        }
        if (evaluator.getGrantedActions(pipContext).contains(action)) {
            return grant("Permitted by the " + policy + " policy.");
        }
        return deny("The " + policy + " policy does not permit action " + action + " on resource "
                + resource.getResourceId() + ".");
    }

    @Override
    public List<EvaluationDecision> evaluate(List<EvaluationRequest> requests, String semantic, String tenantDomain,
                                             String defaultApplicationId) throws AuthZENException {

        String resolvedSemantic = semantic != null ? semantic : AuthZENConstants.SEMANTIC_EXECUTE_ALL;
        if (!AuthZENConstants.SEMANTIC_EXECUTE_ALL.equals(resolvedSemantic)
                && !AuthZENConstants.SEMANTIC_DENY_ON_FIRST_DENY.equals(resolvedSemantic)
                && !AuthZENConstants.SEMANTIC_PERMIT_ON_FIRST_PERMIT.equals(resolvedSemantic)) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "Unsupported evaluations_semantic: " + resolvedSemantic);
        }

        List<EvaluationDecision> decisions = new ArrayList<>(requests.size());
        for (EvaluationRequest request : requests) {
            EvaluationDecision decision;
            try {
                decision = evaluate(request, tenantDomain, defaultApplicationId);
            } catch (AuthZENClientException e) {
                decision = error(400, e.getDescription());
            } catch (AuthZENException e) {
                LOG.error(AuthZENUtils.sanitizeForLog(
                        "Error while evaluating an access request in a batch: " + e.getDescription()), e);
                decision = error(500, AuthZENErrorCode.INTERNAL_ERROR.getDefaultMessage());
            }
            decisions.add(decision);
            if (AuthZENConstants.SEMANTIC_DENY_ON_FIRST_DENY.equals(resolvedSemantic) && !decision.isDecision()) {
                break;
            }
            if (AuthZENConstants.SEMANTIC_PERMIT_ON_FIRST_PERMIT.equals(resolvedSemantic) && decision.isDecision()) {
                break;
            }
        }
        return decisions;
    }

    private PIPContext resolve(SubjectEntity subject, ResourceEntity resource, String tenantDomain,
                               String defaultApplicationId) throws AuthZENException {

        SubjectContext subjectContext = pipService.resolveSubject(subject, tenantDomain);
        ResourceContext resourceContext = pipService.resolveResource(resource, subjectContext.getTenantDomain());

        String applicationId;
        if (AuthZENConstants.SUBJECT_TYPE_APPLICATION.equals(subjectContext.getSubjectType())) {
            applicationId = subjectContext.getSubjectId();
        } else if (resource.getProperties() != null
                && resource.getProperties().get(AuthZENConstants.RESOURCE_PROPERTY_APP_ID) != null) {
            applicationId = (String) resource.getProperties().get(AuthZENConstants.RESOURCE_PROPERTY_APP_ID);
        } else {
            applicationId = defaultApplicationId;
        }
        ApplicationContext applicationContext = applicationId != null
                ? pipService.resolveApplication(applicationId, resourceContext, subjectContext.getTenantDomain())
                : null;
        return new PIPContext(subjectContext, resourceContext, applicationContext);
    }

    /**
     * Applications acting for themselves get the scopes they are authorized for, whatever the policy, as with
     * client credentials tokens.
     */
    private static String resolvePolicy(PIPContext pipContext) {

        if (AuthZENConstants.SUBJECT_TYPE_APPLICATION.equals(pipContext.getSubjectContext().getSubjectType())) {
            return AuthZENConstants.POLICY_NO_POLICY;
        }
        ApplicationContext application = pipContext.getApplicationContext();
        if (application != null && application.getPolicyId() != null) {
            return application.getPolicyId();
        }
        return AuthZENConstants.POLICY_RBAC;
    }

    private PolicyEvaluator getEvaluator(String policy) {

        PolicyEvaluator selected = null;
        for (PolicyEvaluator evaluator : policyEvaluators) {
            if (policy.equalsIgnoreCase(evaluator.getName())
                    && (selected == null || evaluator.getPriority() < selected.getPriority())) {
                selected = evaluator;
            }
        }
        return selected;
    }

    private static EvaluationDecision grant(String reason) {

        return new EvaluationDecision(true, reason(STATUS_GRANTED, reason));
    }

    private static EvaluationDecision deny(String reason) {

        return new EvaluationDecision(false, reason(STATUS_DENIED, reason));
    }

    private static EvaluationDecision error(int status, String message) {

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", status);
        error.put("message", message);
        return new EvaluationDecision(false, Collections.singletonMap(AuthZENConstants.CONTEXT_ERROR, error));
    }

    private static Map<String, Object> reason(String status, String reason) {

        return Collections.singletonMap(AuthZENConstants.CONTEXT_REASON_ADMIN,
                Collections.singletonMap(status, reason));
    }
}
