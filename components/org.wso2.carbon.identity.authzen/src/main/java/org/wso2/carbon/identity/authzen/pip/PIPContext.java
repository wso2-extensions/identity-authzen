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
 * Everything the policy information point resolved for one evaluation.
 */
public class PIPContext {

    private SubjectContext subjectContext;
    private ResourceContext resourceContext;
    private ApplicationContext applicationContext;

    public PIPContext() {
    }

    public PIPContext(SubjectContext subjectContext, ResourceContext resourceContext) {

        this(subjectContext, resourceContext, null);
    }

    /**
     * @param subjectContext     Resolved subject.
     * @param resourceContext    Resolved resource.
     * @param applicationContext Application the evaluation runs for, or {@code null} for none.
     */
    public PIPContext(SubjectContext subjectContext, ResourceContext resourceContext,
                      ApplicationContext applicationContext) {

        this.subjectContext = subjectContext;
        this.resourceContext = resourceContext;
        this.applicationContext = applicationContext;
    }

    public SubjectContext getSubjectContext() {

        return subjectContext;
    }

    public void setSubjectContext(SubjectContext subjectContext) {

        this.subjectContext = subjectContext;
    }

    public ResourceContext getResourceContext() {

        return resourceContext;
    }

    public void setResourceContext(ResourceContext resourceContext) {

        this.resourceContext = resourceContext;
    }

    public ApplicationContext getApplicationContext() {

        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {

        this.applicationContext = applicationContext;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof PIPContext)) {
            return false;
        }
        PIPContext that = (PIPContext) o;
        return Objects.equals(subjectContext, that.subjectContext) &&
                Objects.equals(resourceContext, that.resourceContext) &&
                Objects.equals(applicationContext, that.applicationContext);
    }

    @Override
    public int hashCode() {

        return Objects.hash(subjectContext, resourceContext, applicationContext);
    }

    @Override
    public String toString() {

        return "PIPContext{" +
                "subjectContext=" + subjectContext +
                ", resourceContext=" + resourceContext +
                ", applicationContext=" + applicationContext +
                '}';
    }
}
