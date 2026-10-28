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

package org.wso2.carbon.identity.authzen.pip;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;
import org.wso2.carbon.identity.authzen.core.util.AuthZENUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UniqueIDUserStoreManager;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Map;
import java.util.function.Supplier;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

/**
 * Resolves subjects strictly by id: users and agents by their user id, applications by their resource id.
 */
final class DefaultSubjectResolver {

    @FunctionalInterface
    interface TenantIdResolver {

        int resolve(String tenantDomain);
    }

    private static final Log LOG = LogFactory.getLog(DefaultSubjectResolver.class);

    private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
    private static final String AGENT_USER_STORE_PROPERTY = "AgentIdentity.Userstore";
    private static final String DEFAULT_AGENT_USER_STORE = "AGENT";
    private static final String ACCOUNT_LOCKED_CLAIM = "http://wso2.org/claims/identity/accountLocked";
    private static final String ACCOUNT_UNLOCK_TIME_CLAIM = "http://wso2.org/claims/identity/unlockTime";
    private static final String ACCOUNT_DISABLED_CLAIM = "http://wso2.org/claims/identity/accountDisabled";

    private final TenantIdResolver tenantIdResolver;
    private final Supplier<String> agentUserStoreNameSupplier;

    DefaultSubjectResolver() {

        this(IdentityTenantUtil::getTenantId, DefaultSubjectResolver::getAgentUserStoreName);
    }

    DefaultSubjectResolver(TenantIdResolver tenantIdResolver, Supplier<String> agentUserStoreNameSupplier) {

        this.tenantIdResolver = tenantIdResolver;
        this.agentUserStoreNameSupplier = agentUserStoreNameSupplier;
    }

    SubjectContext resolve(SubjectEntity subject, String tenantDomain) throws AuthZENException {

        if (subject == null) {
            throw new AuthZENClientException(AuthZENErrorCode.MISSING_SUBJECT, "Subject is required for resolution.");
        }
        if (subject.getId() == null || subject.getId().isBlank()) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "Subject id is required for subject resolution.");
        }
        String resolvedTenantDomain = resolveTenantDomain(tenantDomain);
        String subjectType = subject.getType();
        if (AuthZENConstants.SUBJECT_TYPE_USER.equals(subjectType)
                || AuthZENConstants.SUBJECT_TYPE_AGENT.equals(subjectType)) {
            return resolveUser(subjectType, subject.getId(), resolvedTenantDomain);
        }
        if (AuthZENConstants.SUBJECT_TYPE_APPLICATION.equals(subjectType)) {
            return resolveApplication(subject.getId(), resolvedTenantDomain);
        }
        throw new AuthZENClientException(AuthZENErrorCode.UNSUPPORTED_SUBJECT_TYPE,
                "Unsupported subject type: " + subjectType);
    }

    private SubjectContext resolveUser(String subjectType, String userId, String tenantDomain)
            throws AuthZENException {

        UniqueIDUserStoreManager userStoreManager = getUserStoreManager(tenantDomain);
        try {
            if (!userStoreManager.isExistingUserWithID(userId)) {
                throw subjectNotFound(subjectType);
            }
            User user = userStoreManager.getUserWithID(userId, null, null);
            if (user == null) {
                throw subjectNotFound(subjectType);
            }
            // Agents live in their own user store. A user id is only accepted for the matching subject type.
            boolean isAgent = agentUserStoreNameSupplier.get().equalsIgnoreCase(user.getUserStoreDomain());
            if (isAgent != AuthZENConstants.SUBJECT_TYPE_AGENT.equals(subjectType)) {
                throw subjectNotFound(subjectType);
            }
            // The identity server does not authenticate locked or disabled accounts, so they get no access.
            Map<String, String> accountState = userStoreManager.getUserClaimValuesWithID(userId,
                    new String[]{ACCOUNT_LOCKED_CLAIM, ACCOUNT_UNLOCK_TIME_CLAIM, ACCOUNT_DISABLED_CLAIM}, null);
            if (accountState != null && Boolean.parseBoolean(accountState.get(ACCOUNT_DISABLED_CLAIM))) {
                throw new AuthZENClientException(AuthZENErrorCode.SUBJECT_INACTIVE,
                        "The subject's account is disabled.");
            }
            if (accountState != null && isLocked(accountState)) {
                throw new AuthZENClientException(AuthZENErrorCode.SUBJECT_INACTIVE, "The subject's account is locked.");
            }
            return new SubjectContext(subjectType, userId, tenantDomain);
        } catch (UserStoreException e) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Error while resolving the " + subjectType + " subject.", e);
        }
    }

    private SubjectContext resolveApplication(String applicationId, String tenantDomain) throws AuthZENException {

        ApplicationManagementService applicationManagementService =
                AuthZENDataHolder.getApplicationManagementService();
        if (applicationManagementService == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "ApplicationManagementService is not available.");
        }
        try {
            ApplicationBasicInfo application =
                    applicationManagementService.getApplicationBasicInfoByResourceId(applicationId, tenantDomain);
            if (application == null) {
                throw subjectNotFound(AuthZENConstants.SUBJECT_TYPE_APPLICATION);
            }
            return new SubjectContext(AuthZENConstants.SUBJECT_TYPE_APPLICATION, applicationId, tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Error while resolving the application subject.", e);
        }
    }

    private UniqueIDUserStoreManager getUserStoreManager(String tenantDomain) throws AuthZENServerException {

        RealmService realmService = AuthZENDataHolder.getRealmService();
        if (realmService == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR, "RealmService is not available.");
        }
        try {
            UserRealm userRealm = realmService.getTenantUserRealm(tenantIdResolver.resolve(tenantDomain));
            if (userRealm == null) {
                throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                        "User realm is not available for tenant: " + tenantDomain);
            }
            if (!(userRealm.getUserStoreManager() instanceof UniqueIDUserStoreManager)) {
                throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                        "The user store of tenant " + tenantDomain + " does not support unique user ids.");
            }
            return (UniqueIDUserStoreManager) userRealm.getUserStoreManager();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Error while resolving the user store manager for tenant: " + tenantDomain, e);
        }
    }

    /**
     * A lock with an unlock time that has passed no longer holds, as at sign in, where the identity server then
     * clears it.
     */
    private static boolean isLocked(Map<String, String> accountState) {

        if (!Boolean.parseBoolean(accountState.get(ACCOUNT_LOCKED_CLAIM))) {
            return false;
        }
        String unlockTime = accountState.get(ACCOUNT_UNLOCK_TIME_CLAIM);
        if (unlockTime == null || unlockTime.isBlank()) {
            return true;
        }
        try {
            long unlockAt = Long.parseLong(unlockTime.trim());
            return unlockAt <= 0 || unlockAt > System.currentTimeMillis();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static AuthZENClientException subjectNotFound(String subjectType) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(AuthZENUtils.sanitizeForLog("No " + subjectType + " subject found for the given id."));
        }
        return new AuthZENClientException(AuthZENErrorCode.SUBJECT_NOT_FOUND, "Subject not found.");
    }

    private static String resolveTenantDomain(String tenantDomain) {

        if (tenantDomain == null || tenantDomain.isBlank()) {
            return SUPER_TENANT_DOMAIN_NAME;
        }
        return tenantDomain;
    }

    private static String getAgentUserStoreName() {

        String userStoreName = IdentityUtil.getProperty(AGENT_USER_STORE_PROPERTY);
        return userStoreName == null || userStoreName.isBlank() ? DEFAULT_AGENT_USER_STORE : userStoreName;
    }
}
