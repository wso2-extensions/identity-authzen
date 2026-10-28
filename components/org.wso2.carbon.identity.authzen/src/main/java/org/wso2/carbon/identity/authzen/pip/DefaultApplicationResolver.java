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

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AuthorizedAPI;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;

import java.util.Collections;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

/**
 * Resolves an application, the audience of the roles it uses, and its authorization for an API resource.
 */
final class DefaultApplicationResolver {

    ApplicationContext resolve(String applicationId, ResourceContext resourceContext, String tenantDomain)
            throws AuthZENException {

        if (applicationId == null || applicationId.isBlank()) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Application id is required.");
        }
        ApplicationManagementService applicationManagementService =
                AuthZENDataHolder.getApplicationManagementService();
        AuthorizedAPIManagementService authorizedAPIManagementService =
                AuthZENDataHolder.getAuthorizedAPIManagementService();
        if (applicationManagementService == null || authorizedAPIManagementService == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Application management services are not available.");
        }

        try {
            ServiceProvider application =
                    applicationManagementService.getApplicationByResourceId(applicationId, tenantDomain);
            if (application == null) {
                throw new AuthZENClientException(AuthZENErrorCode.APPLICATION_NOT_FOUND, "Application not found.");
            }
            // The identity server issues no tokens for a disabled application.
            if (!application.isApplicationEnabled()) {
                throw new AuthZENClientException(AuthZENErrorCode.APPLICATION_DISABLED, "The application is disabled.");
            }
            String roleAudience =
                    applicationManagementService.getAllowedAudienceForRoleAssociation(applicationId, tenantDomain);
            AuthorizedAPI authorizedAPI = authorizedAPIManagementService.getAuthorizedAPI(applicationId,
                    resourceContext.getApiResourceId(), tenantDomain);
            if (authorizedAPI == null) {
                return new ApplicationContext(applicationId, roleAudience, null, Collections.emptyList());
            }
            return new ApplicationContext(applicationId, roleAudience, authorizedAPI.getPolicyId(),
                    DefaultResourceResolver.resolveScopes(authorizedAPI.getScopes()));
        } catch (IdentityApplicationManagementException e) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Error while resolving the application.", e);
        }
    }
}
