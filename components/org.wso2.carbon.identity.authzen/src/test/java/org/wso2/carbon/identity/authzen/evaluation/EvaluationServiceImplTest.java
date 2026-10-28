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

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.model.ActionEntity;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;
import org.wso2.carbon.identity.authzen.pip.ApplicationContext;
import org.wso2.carbon.identity.authzen.pip.PIPContext;
import org.wso2.carbon.identity.authzen.pip.PIPService;
import org.wso2.carbon.identity.authzen.pip.ResourceContext;
import org.wso2.carbon.identity.authzen.pip.SubjectContext;

import java.util.List;
import java.util.Map;

public class EvaluationServiceImplTest {

    private static final String TENANT = "carbon.super";
    private static final String API = "/api/courses";
    private static final String PORTAL = "portal";
    private static final ResourceContext RESOURCE = new ResourceContext(API, "api-1", "api_resource",
            List.of("courses:read", "courses:write", "courses:delete"));

    private PIPService pipService;
    private EvaluationServiceImpl evaluationService;

    @BeforeMethod
    public void setUp() throws Exception {

        pipService = Mockito.mock(PIPService.class);
        Mockito.when(pipService.resolveSubject(Mockito.any(), Mockito.eq(TENANT))).thenAnswer(invocation -> {
            SubjectEntity subject = invocation.getArgument(0);
            if ("missing".equals(subject.getId())) {
                throw new AuthZENClientException(AuthZENErrorCode.SUBJECT_NOT_FOUND, "Subject not found.");
            }
            if ("locked".equals(subject.getId())) {
                throw new AuthZENClientException(AuthZENErrorCode.SUBJECT_INACTIVE,
                        "The subject's account is locked.");
            }
            return new SubjectContext(subject.getType(), subject.getId(), TENANT);
        });
        Mockito.when(pipService.resolveResource(Mockito.any(), Mockito.eq(TENANT))).thenReturn(RESOURCE);
        mockApplication(PORTAL, new ApplicationContext(PORTAL, "organization", "RBAC",
                List.of("courses:read", "courses:write", "courses:delete")));
        evaluationService = new EvaluationServiceImpl(pipService,
                List.of(rbacGranting("courses:read", "courses:write")));
    }

    @Test
    public void shouldPermitWhenPolicyGrantsAction() throws Exception {

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", PORTAL), TENANT,
                null);

        Assert.assertTrue(decision.isDecision());
        Assert.assertEquals(decision.getContext(),
                Map.of("reason_admin", Map.of("200", "Permitted by the RBAC policy.")));
    }

    @Test
    public void shouldDenyWithoutApplication() throws Exception {

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", null), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
        Assert.assertEquals(decision.getContext(), Map.of("reason_admin", Map.of("403",
                "No application to evaluate for. Pass resource.properties.app_id or call with an access token of "
                        + "the application.")));
        Mockito.verify(pipService, Mockito.never()).resolveApplication(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldDenyActionNotDefinedOnResource() throws Exception {

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:export", null), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
    }

    @Test
    public void shouldDenyUnknownSubjectWithoutError() throws Exception {

        EvaluationDecision decision = evaluationService.evaluate(request("missing", "courses:read", null), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
        Assert.assertEquals(decision.getContext(), Map.of("reason_admin", Map.of("403", "Subject not found.")));
    }

    @Test
    public void shouldDenyInactiveSubject() throws Exception {

        EvaluationDecision decision = evaluationService.evaluate(request("locked", "courses:read", PORTAL), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
        Assert.assertEquals(decision.getContext(),
                Map.of("reason_admin", Map.of("403", "The subject's account is locked.")));
    }

    @Test
    public void shouldDenyDisabledApplication() throws Exception {

        Mockito.when(pipService.resolveApplication("app-1", RESOURCE, TENANT)).thenThrow(
                new AuthZENClientException(AuthZENErrorCode.APPLICATION_DISABLED, "The application is disabled."));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", "app-1"), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
        Assert.assertEquals(decision.getContext(),
                Map.of("reason_admin", Map.of("403", "The application is disabled.")));
    }

    @Test
    public void shouldDenyWhenApplicationIsNotAuthorizedForAction() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", "RBAC", List.of("courses:read")));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:write", "app-1"), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
    }

    @Test
    public void shouldDenyWhenApplicationIsNotAuthorizedForResource() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", null, List.of()));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", "app-1"), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
    }

    @Test
    public void shouldUseCallerApplicationWhenRequestNamesNone() throws Exception {

        mockApplication("caller-app", new ApplicationContext("caller-app", "organization", "RBAC",
                List.of("courses:write")));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", null), TENANT,
                "caller-app");

        Assert.assertFalse(decision.isDecision());
        Mockito.verify(pipService).resolveApplication("caller-app", RESOURCE, TENANT);
    }

    @Test
    public void shouldPreferRequestApplicationOverCallerApplication() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", "RBAC", List.of("courses:read")));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", "app-1"), TENANT,
                "caller-app");

        Assert.assertTrue(decision.isDecision());
        Mockito.verify(pipService, Mockito.never()).resolveApplication(Mockito.eq("caller-app"), Mockito.any(),
                Mockito.any());
    }

    @Test
    public void shouldUseApplicationPolicy() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", "NO POLICY",
                List.of("courses:delete")));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:delete", "app-1"),
                TENANT, null);

        Assert.assertTrue(decision.isDecision());
        Assert.assertEquals(decision.getContext(),
                Map.of("reason_admin", Map.of("200", "Permitted by the NO POLICY policy.")));
    }

    @Test
    public void shouldDenyUnsupportedPolicy() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", "ABAC", List.of("courses:read")));

        EvaluationDecision decision = evaluationService.evaluate(request("alice", "courses:read", "app-1"), TENANT,
                null);

        Assert.assertFalse(decision.isDecision());
    }

    @Test
    public void shouldEvaluateApplicationSubjectAgainstItsAuthorizedScopes() throws Exception {

        mockApplication("app-1", new ApplicationContext("app-1", "organization", "RBAC", List.of("courses:delete")));
        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("application", "app-1", null),
                new ActionEntity("courses:delete", null), new ResourceEntity("api_resource", API, null), null);

        Assert.assertTrue(evaluationService.evaluate(request, TENANT, "caller-app").isDecision());
    }

    @Test
    public void shouldRejectInvalidRequest() {

        EvaluationRequest request = new EvaluationRequest(null, new ActionEntity("courses:read", null),
                new ResourceEntity("api_resource", API, null), null);

        Assert.expectThrows(AuthZENClientException.class, () -> evaluationService.evaluate(request, TENANT, null));
    }

    @Test
    public void shouldStopBatchOnFirstDeny() throws Exception {

        List<EvaluationDecision> decisions = evaluationService.evaluate(List.of(
                request("alice", "courses:read", PORTAL),
                request("alice", "courses:delete", PORTAL),
                request("alice", "courses:write", PORTAL)), "deny_on_first_deny", TENANT, null);

        Assert.assertEquals(decisions.size(), 2);
        Assert.assertTrue(decisions.get(0).isDecision());
        Assert.assertFalse(decisions.get(1).isDecision());
    }

    @Test
    public void shouldStopBatchOnFirstPermit() throws Exception {

        List<EvaluationDecision> decisions = evaluationService.evaluate(List.of(
                request("alice", "courses:delete", PORTAL),
                request("alice", "courses:read", PORTAL),
                request("alice", "courses:write", PORTAL)), "permit_on_first_permit", TENANT, null);

        Assert.assertEquals(decisions.size(), 2);
        Assert.assertTrue(decisions.get(1).isDecision());
    }

    @Test
    public void shouldReportInvalidBatchEntriesAsErrors() throws Exception {

        EvaluationRequest invalid = new EvaluationRequest(new SubjectEntity("user", "alice", null), null,
                new ResourceEntity("api_resource", API, null), null);

        List<EvaluationDecision> decisions = evaluationService.evaluate(List.of(invalid,
                request("alice", "courses:read", PORTAL)), null, TENANT, null);

        Assert.assertEquals(decisions.size(), 2);
        Assert.assertFalse(decisions.get(0).isDecision());
        Assert.assertEquals(decisions.get(0).getContext(),
                Map.of("error", Map.of("status", 400, "message", "Action is required.")));
        Assert.assertTrue(decisions.get(1).isDecision());
    }

    @Test
    public void shouldRejectUnknownBatchSemantic() {

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> evaluationService.evaluate(List.of(), "first_wins", TENANT, null));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
    }

    private void mockApplication(String applicationId, ApplicationContext applicationContext) throws Exception {

        Mockito.when(pipService.resolveApplication(applicationId, RESOURCE, TENANT)).thenReturn(applicationContext);
    }

    private static EvaluationRequest request(String subjectId, String action, String applicationId) {

        return new EvaluationRequest(new SubjectEntity("user", subjectId, null), new ActionEntity(action, null),
                new ResourceEntity("api_resource", API,
                        applicationId != null ? Map.of("app_id", applicationId) : null), null);
    }

    private static PolicyEvaluator rbacGranting(String... actions) {

        return new PolicyEvaluator() {

            @Override
            public String getName() {

                return "RBAC";
            }

            @Override
            public List<String> getGrantedActions(PIPContext pipContext) {

                return List.of(actions);
            }
        };
    }
}
