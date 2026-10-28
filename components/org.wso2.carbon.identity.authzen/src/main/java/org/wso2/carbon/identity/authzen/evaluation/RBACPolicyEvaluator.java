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

import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;
import org.wso2.carbon.identity.authzen.pip.ApplicationContext;
import org.wso2.carbon.identity.authzen.pip.PIPContext;
import org.wso2.carbon.identity.authzen.pip.SubjectContext;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.util.AuthzUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

/**
 * Role-based access control, with the same role selection the identity server applies when it issues tokens:
 * <ul>
 *     <li>for an application whose roles have application audience, only roles of that application count;</li>
 *     <li>for an application whose roles have organization audience, or without an application, only
 *     organization roles count.</li>
 * </ul>
 * Roles assigned directly and through groups are both considered.
 */
public class RBACPolicyEvaluator implements PolicyEvaluator {

    private static final String AUDIENCE_APPLICATION = "application";
    private static final String AUDIENCE_ORGANIZATION = "organization";

    @Override
    public String getName() {

        return AuthZENConstants.POLICY_RBAC;
    }

    @Override
    public int getPriority() {

        return 10;
    }

    @Override
    public Collection<String> getGrantedActions(PIPContext pipContext) throws AuthZENException {

        SubjectContext subject = pipContext.getSubjectContext();
        if (subject == null || AuthZENConstants.SUBJECT_TYPE_APPLICATION.equals(subject.getSubjectType())) {
            // Applications hold no roles.
            return Collections.emptyList();
        }
        String tenantDomain = subject.getTenantDomain();
        List<String> eligibleRoleIds = new ArrayList<>();
        for (String roleId : new LinkedHashSet<>(getRoleIds(subject.getSubjectId(), tenantDomain))) {
            RoleBasicInfo role = getRole(roleId, tenantDomain);
            if (role != null && isEligible(role, pipContext.getApplicationContext())) {
                eligibleRoleIds.add(roleId);
            }
        }
        if (eligibleRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return getScopesOfRoles(eligibleRoleIds, tenantDomain);
    }

    private static boolean isEligible(RoleBasicInfo role, ApplicationContext applicationContext) {

        if (applicationContext != null && AUDIENCE_APPLICATION.equalsIgnoreCase(applicationContext.getRoleAudience())) {
            return AUDIENCE_APPLICATION.equalsIgnoreCase(role.getAudience())
                    && Objects.equals(applicationContext.getApplicationId(), role.getAudienceId());
        }
        return AUDIENCE_ORGANIZATION.equalsIgnoreCase(role.getAudience());
    }

    List<String> getRoleIds(String userId, String tenantDomain) throws AuthZENServerException {

        try {
            List<String> roleIds = AuthzUtil.getRoles(userId, tenantDomain);
            return roleIds != null ? roleIds : Collections.emptyList();
        } catch (IdentityOAuth2Exception e) {
            throw new AuthZENServerException(AuthZENErrorCode.EVALUATION_FAILED,
                    "Failed to resolve the roles of the subject.", e);
        }
    }

    RoleBasicInfo getRole(String roleId, String tenantDomain) throws AuthZENServerException {

        try {
            return getRoleManagementService().getRoleBasicInfoById(roleId, tenantDomain);
        } catch (IdentityRoleManagementException e) {
            throw new AuthZENServerException(AuthZENErrorCode.EVALUATION_FAILED,
                    "Failed to resolve a role of the subject.", e);
        }
    }

    List<String> getScopesOfRoles(List<String> roleIds, String tenantDomain) throws AuthZENServerException {

        try {
            List<String> scopes = getRoleManagementService().getPermissionListOfRoles(roleIds, tenantDomain);
            return scopes != null ? scopes : Collections.emptyList();
        } catch (IdentityRoleManagementException e) {
            throw new AuthZENServerException(AuthZENErrorCode.EVALUATION_FAILED,
                    "Failed to resolve the scopes of the subject's roles.", e);
        }
    }

    private static RoleManagementService getRoleManagementService() throws AuthZENServerException {

        RoleManagementService roleManagementService = AuthZENDataHolder.getRoleManagementService();
        if (roleManagementService == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "RoleManagementService is not available.");
        }
        return roleManagementService;
    }
}
