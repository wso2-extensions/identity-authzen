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

import org.wso2.carbon.identity.api.resource.mgt.APIResourceManager;
import org.wso2.carbon.identity.api.resource.mgt.APIResourceMgtException;
import org.wso2.carbon.identity.application.common.model.APIResource;
import org.wso2.carbon.identity.application.common.model.Scope;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

final class DefaultResourceResolver {

    ResourceContext resolve(ResourceEntity resource, String tenantDomain) throws AuthZENException {

        if (resource == null) {
            throw new AuthZENClientException(AuthZENErrorCode.MISSING_RESOURCE,
                    "Resource is required for resource resolution.");
        }
        if (!AuthZENConstants.RESOURCE_TYPE_API_RESOURCE.equals(resource.getType())) {
            throw new AuthZENClientException(AuthZENErrorCode.UNSUPPORTED_RESOURCE_TYPE,
                    "Unsupported resource type: " + resource.getType());
        }
        if (resource.getId() == null || resource.getId().isBlank()) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "Resource id is required for resource resolution.");
        }

        APIResourceManager apiResourceManager = AuthZENDataHolder.getApiResourceManager();
        if (apiResourceManager == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "APIResourceManager is not available.");
        }
        try {
            APIResource apiResource = apiResourceManager.getAPIResourceByIdentifier(resource.getId(), tenantDomain);
            if (apiResource == null) {
                throw new AuthZENClientException(AuthZENErrorCode.RESOURCE_NOT_FOUND, "Resource not found.");
            }
            return new ResourceContext(
                    apiResource.getIdentifier() != null ? apiResource.getIdentifier() : resource.getId(),
                    apiResource.getId(),
                    AuthZENConstants.RESOURCE_TYPE_API_RESOURCE,
                    resolveScopes(apiResource.getScopes()));
        } catch (APIResourceMgtException e) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR,
                    "Error while resolving the API resource.", e);
        }
    }

    static List<String> resolveScopes(List<Scope> scopes) {

        if (scopes == null) {
            return Collections.emptyList();
        }
        return scopes.stream()
                .map(Scope::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
