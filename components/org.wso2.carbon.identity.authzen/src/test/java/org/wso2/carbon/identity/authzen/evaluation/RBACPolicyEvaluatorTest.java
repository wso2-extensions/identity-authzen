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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;
import org.wso2.carbon.identity.authzen.pip.ApplicationContext;
import org.wso2.carbon.identity.authzen.pip.PIPContext;
import org.wso2.carbon.identity.authzen.pip.ResourceContext;
import org.wso2.carbon.identity.authzen.pip.SubjectContext;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RBACPolicyEvaluatorTest {

    private static final String TENANT = "carbon.super";
    private static final Map<String, RoleBasicInfo> ROLES = Map.of(
            "org-reader", role("org-reader", "organization", "org-id"),
            "app-a-writer", role("app-a-writer", "application", "app-a"),
            "app-b-admin", role("app-b-admin", "application", "app-b"));
    private static final Map<String, List<String>> ROLE_SCOPES = Map.of(
            "org-reader", List.of("courses:read"),
            "app-a-writer", List.of("courses:write"),
            "app-b-admin", List.of("courses:delete"));

    @Test
    public void shouldExposeRbacMetadata() {

        RBACPolicyEvaluator evaluator = new RBACPolicyEvaluator();

        Assert.assertEquals(evaluator.getName(), "RBAC");
        Assert.assertEquals(evaluator.getPriority(), 10);
    }

    @Test
    public void shouldUseOnlyOrganizationRolesWithoutApplication() throws Exception {

        Collection<String> granted = evaluatorWithAllRoles().getGrantedActions(pipContext("user", null));

        Assert.assertEquals(granted, List.of("courses:read"));
    }

    @Test
    public void shouldUseOnlyOrganizationRolesForOrganizationAudienceApplication() throws Exception {

        Collection<String> granted = evaluatorWithAllRoles().getGrantedActions(pipContext("user",
                new ApplicationContext("app-a", "organization", "RBAC", List.of("courses:write"))));

        Assert.assertEquals(granted, List.of("courses:read"));
    }

    @Test
    public void shouldUseOnlyTheApplicationsRolesForApplicationAudienceApplication() throws Exception {

        Collection<String> granted = evaluatorWithAllRoles().getGrantedActions(pipContext("agent",
                new ApplicationContext("app-a", "APPLICATION", "RBAC", List.of("courses:write"))));

        Assert.assertEquals(granted, List.of("courses:write"));
    }

    @Test
    public void shouldGrantNothingToApplicationSubjects() throws Exception {

        Collection<String> granted = evaluatorWithAllRoles().getGrantedActions(pipContext("application", null));

        Assert.assertTrue(granted.isEmpty());
    }

    @Test
    public void shouldPropagateRoleResolutionFailures() {

        RBACPolicyEvaluator evaluator = new TestRBACPolicyEvaluator(List.of(), new AuthZENServerException(
                AuthZENErrorCode.EVALUATION_FAILED, "Failed to resolve the roles of the subject."));

        AuthZENServerException exception = Assert.expectThrows(AuthZENServerException.class,
                () -> evaluator.getGrantedActions(pipContext("user", null)));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.EVALUATION_FAILED);
    }

    private static RBACPolicyEvaluator evaluatorWithAllRoles() {

        return new TestRBACPolicyEvaluator(List.of("org-reader", "app-a-writer", "app-b-admin", "org-reader"),
                null);
    }

    private static PIPContext pipContext(String subjectType, ApplicationContext applicationContext) {

        return new PIPContext(new SubjectContext(subjectType, "subject-1", TENANT),
                new ResourceContext("/api/courses", "api-1", "api_resource",
                        List.of("courses:read", "courses:write", "courses:delete")),
                applicationContext);
    }

    private static RoleBasicInfo role(String id, String audience, String audienceId) {

        RoleBasicInfo role = new RoleBasicInfo(id, id);
        role.setAudience(audience);
        role.setAudienceId(audienceId);
        return role;
    }

    private static final class TestRBACPolicyEvaluator extends RBACPolicyEvaluator {

        private final List<String> roleIds;
        private final AuthZENServerException roleFailure;

        private TestRBACPolicyEvaluator(List<String> roleIds, AuthZENServerException roleFailure) {

            this.roleIds = roleIds;
            this.roleFailure = roleFailure;
        }

        @Override
        List<String> getRoleIds(String userId, String tenantDomain) throws AuthZENServerException {

            if (roleFailure != null) {
                throw roleFailure;
            }
            return roleIds;
        }

        @Override
        RoleBasicInfo getRole(String roleId, String tenantDomain) {

            return ROLES.get(roleId);
        }

        @Override
        List<String> getScopesOfRoles(List<String> roleIds, String tenantDomain) {

            List<String> scopes = new ArrayList<>();
            roleIds.forEach(roleId -> scopes.addAll(ROLE_SCOPES.get(roleId)));
            return scopes;
        }
    }
}
