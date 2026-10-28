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

import java.util.List;
import java.util.Objects;

/**
 * Resolved API resource of an evaluation.
 */
public class ResourceContext {

    private String resourceId;
    private String apiResourceId;
    private String resourceType;
    private List<String> scopes;

    public ResourceContext() {
    }

    /**
     * @param resourceId    API resource identifier, as sent in the request.
     * @param apiResourceId Internal id of the API resource.
     * @param resourceType  Resource type.
     * @param scopes        Scopes defined on the API resource.
     */
    public ResourceContext(String resourceId, String apiResourceId, String resourceType, List<String> scopes) {

        this.resourceId = resourceId;
        this.apiResourceId = apiResourceId;
        this.resourceType = resourceType;
        this.scopes = scopes;
    }

    public String getResourceId() {

        return resourceId;
    }

    public void setResourceId(String resourceId) {

        this.resourceId = resourceId;
    }

    public String getApiResourceId() {

        return apiResourceId;
    }

    public void setApiResourceId(String apiResourceId) {

        this.apiResourceId = apiResourceId;
    }

    public String getResourceType() {

        return resourceType;
    }

    public void setResourceType(String resourceType) {

        this.resourceType = resourceType;
    }

    public List<String> getScopes() {

        return scopes;
    }

    public void setScopes(List<String> scopes) {

        this.scopes = scopes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceContext)) {
            return false;
        }
        ResourceContext that = (ResourceContext) o;
        return Objects.equals(resourceId, that.resourceId) &&
                Objects.equals(apiResourceId, that.apiResourceId) &&
                Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(scopes, that.scopes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(resourceId, apiResourceId, resourceType, scopes);
    }

    @Override
    public String toString() {

        return "ResourceContext{" +
                "resourceId='" + resourceId + '\'' +
                ", apiResourceId='" + apiResourceId + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", scopes=" + scopes +
                '}';
    }
}
