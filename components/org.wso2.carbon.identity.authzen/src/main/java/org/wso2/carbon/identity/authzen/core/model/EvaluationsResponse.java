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

import java.util.List;
import java.util.Objects;

/**
 * Access Evaluations (batch) response. Decisions are returned in request order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationsResponse {

    private List<EvaluationResponse> evaluations;

    public EvaluationsResponse() {
    }

    public EvaluationsResponse(List<EvaluationResponse> evaluations) {

        this.evaluations = evaluations;
    }

    public List<EvaluationResponse> getEvaluations() {

        return evaluations;
    }

    public void setEvaluations(List<EvaluationResponse> evaluations) {

        this.evaluations = evaluations;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationsResponse)) {
            return false;
        }
        return Objects.equals(evaluations, ((EvaluationsResponse) o).evaluations);
    }

    @Override
    public int hashCode() {

        return Objects.hash(evaluations);
    }
}
