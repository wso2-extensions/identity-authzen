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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Access Evaluations (batch) request. Top-level subject, action, resource and context act as defaults that each
 * entry in {@code evaluations} may override.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationsRequest {

    private SubjectEntity subject;
    private ActionEntity action;
    private ResourceEntity resource;
    private ContextEntity context;
    private List<EvaluationRequest> evaluations;
    private Map<String, Object> options;

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

    public List<EvaluationRequest> getEvaluations() {

        return evaluations;
    }

    public void setEvaluations(List<EvaluationRequest> evaluations) {

        this.evaluations = evaluations;
    }

    public Map<String, Object> getOptions() {

        return options;
    }

    public void setOptions(Map<String, Object> options) {

        this.options = options;
    }

    /**
     * Whether this request carries a batch. Without one the request behaves as a single Access Evaluation.
     */
    public boolean isBatch() {

        return evaluations != null && !evaluations.isEmpty();
    }

    /**
     * Returns the top-level fields as a single evaluation request.
     */
    public EvaluationRequest toSingleRequest() {

        return new EvaluationRequest(subject, action, resource, context);
    }

    /**
     * Returns each batch entry with the top-level defaults applied. A key set on an entry replaces the whole
     * top-level value for that key.
     */
    public List<EvaluationRequest> resolveEvaluations() {

        List<EvaluationRequest> entries = evaluations;
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<EvaluationRequest> resolved = new ArrayList<>(entries.size());
        for (EvaluationRequest entry : entries) {
            EvaluationRequest item = entry != null ? entry : new EvaluationRequest();
            resolved.add(new EvaluationRequest(
                    item.getSubject() != null ? item.getSubject() : subject,
                    item.getAction() != null ? item.getAction() : action,
                    item.getResource() != null ? item.getResource() : resource,
                    item.getContext() != null ? item.getContext() : context));
        }
        return resolved;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationsRequest)) {
            return false;
        }
        EvaluationsRequest that = (EvaluationsRequest) o;
        return Objects.equals(subject, that.subject) &&
                Objects.equals(action, that.action) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(context, that.context) &&
                Objects.equals(evaluations, that.evaluations) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {

        return Objects.hash(subject, action, resource, context, evaluations, options);
    }
}
