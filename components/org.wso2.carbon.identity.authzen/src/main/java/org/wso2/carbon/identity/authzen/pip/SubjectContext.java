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

import java.util.Objects;

/**
 * Resolved subject of an evaluation.
 * <p>
 * For {@code user} and {@code agent} subjects the id is the user id in the tenant's user store. For
 * {@code application} subjects it is the application resource id.
 */
public class SubjectContext {

    private String subjectType;
    private String subjectId;
    private String tenantDomain;

    public SubjectContext() {
    }

    public SubjectContext(String subjectType, String subjectId, String tenantDomain) {

        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.tenantDomain = tenantDomain;
    }

    public String getSubjectType() {

        return subjectType;
    }

    public void setSubjectType(String subjectType) {

        this.subjectType = subjectType;
    }

    public String getSubjectId() {

        return subjectId;
    }

    public void setSubjectId(String subjectId) {

        this.subjectId = subjectId;
    }

    public String getTenantDomain() {

        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof SubjectContext)) {
            return false;
        }
        SubjectContext that = (SubjectContext) o;
        return Objects.equals(subjectType, that.subjectType) &&
                Objects.equals(subjectId, that.subjectId) &&
                Objects.equals(tenantDomain, that.tenantDomain);
    }

    @Override
    public int hashCode() {

        return Objects.hash(subjectType, subjectId, tenantDomain);
    }

    @Override
    public String toString() {

        return "SubjectContext{" +
                "subjectType='" + subjectType + '\'' +
                ", subjectId='" + subjectId + '\'' +
                ", tenantDomain='" + tenantDomain + '\'' +
                '}';
    }
}
