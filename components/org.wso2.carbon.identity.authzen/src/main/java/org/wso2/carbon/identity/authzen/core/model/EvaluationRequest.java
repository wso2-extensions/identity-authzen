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

package org.wso2.carbon.identity.authzen.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationRequest {

    private SubjectEntity subject;

    private ActionEntity action;

    private ResourceEntity resource;

    private ContextEntity context;

    public EvaluationRequest() {
    }

    public EvaluationRequest(SubjectEntity subject, ActionEntity action, ResourceEntity resource,
                             ContextEntity context) {

        this.subject = subject;
        this.action = action;
        this.resource = resource;
        this.context = context;
    }

    public SubjectEntity getSubject() {

        return subject;
    }

    public void setSubject(SubjectEntity subject) {

        this.subject = subject;
    }

    public ActionEntity getAction() {

        return action;
    }

    public void setAction(ActionEntity action) {

        this.action = action;
    }

    public ResourceEntity getResource() {

        return resource;
    }

    public void setResource(ResourceEntity resource) {

        this.resource = resource;
    }

    public ContextEntity getContext() {

        return context;
    }

    public void setContext(ContextEntity context) {

        this.context = context;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationRequest)) {
            return false;
        }
        EvaluationRequest that = (EvaluationRequest) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(action, that.action) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {

        return Objects.hash(subject, action, resource, context);
    }

    @Override
    public String toString() {

        return "EvaluationRequest{" +
                "subject=" + subject +
                ", action=" + action +
                ", resource=" + resource +
                ", context=" + context +
                '}';
    }
}
