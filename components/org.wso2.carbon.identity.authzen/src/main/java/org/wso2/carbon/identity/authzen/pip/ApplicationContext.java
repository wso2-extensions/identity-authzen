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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Application on whose behalf an evaluation runs, together with what it is authorized for on the evaluated API
 * resource.
 */
public class ApplicationContext {

    private String applicationId;
    private String roleAudience;
    private String policyId;
    private List<String> authorizedScopes;

    public ApplicationContext() {
    }

    /**
     * @param applicationId    Application resource id.
     * @param roleAudience     Audience of the roles the application uses: {@code application} or
     *                         {@code organization}.
     * @param policyId         Authorization policy of the application's subscription to the API resource, or
     *                         {@code null} when the application is not authorized for the API resource.
     * @param authorizedScopes Scopes of the API resource the application is authorized for.
     */
    public ApplicationContext(String applicationId, String roleAudience, String policyId,
                              List<String> authorizedScopes) {

        this.applicationId = applicationId;
        this.roleAudience = roleAudience;
        this.policyId = policyId;
        this.authorizedScopes = authorizedScopes;
    }

    public String getApplicationId() {

        return applicationId;
    }

    public void setApplicationId(String applicationId) {

        this.applicationId = applicationId;
    }

    public String getRoleAudience() {

        return roleAudience;
    }

    public void setRoleAudience(String roleAudience) {

        this.roleAudience = roleAudience;
    }

    public String getPolicyId() {

        return policyId;
    }

    public void setPolicyId(String policyId) {

        this.policyId = policyId;
    }

    public List<String> getAuthorizedScopes() {

        return authorizedScopes != null ? authorizedScopes : Collections.emptyList();
    }

    public void setAuthorizedScopes(List<String> authorizedScopes) {

        this.authorizedScopes = authorizedScopes;
    }

    /**
     * Whether the application is authorized for the evaluated API resource.
     */
    public boolean isAuthorizedForResource() {

        return policyId != null;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationContext)) {
            return false;
        }
        ApplicationContext that = (ApplicationContext) o;
        return Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(roleAudience, that.roleAudience) &&
                Objects.equals(policyId, that.policyId) &&
                Objects.equals(authorizedScopes, that.authorizedScopes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(applicationId, roleAudience, policyId, authorizedScopes);
    }

    @Override
    public String toString() {

        return "ApplicationContext{" +
                "applicationId='" + applicationId + '\'' +
                ", roleAudience='" + roleAudience + '\'' +
                ", policyId='" + policyId + '\'' +
                ", authorizedScopes=" + authorizedScopes +
                '}';
    }
}
