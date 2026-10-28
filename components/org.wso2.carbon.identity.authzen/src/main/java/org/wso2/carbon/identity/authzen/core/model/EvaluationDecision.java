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
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationDecision {

    private boolean decision;

    private Map<String, Object> context;

    public EvaluationDecision() {
    }

    public EvaluationDecision(boolean decision, Map<String, Object> context) {

        this.decision = decision;
        this.context = context;
    }

    public boolean isDecision() {

        return decision;
    }

    public void setDecision(boolean decision) {

        this.decision = decision;
    }

    public Map<String, Object> getContext() {

        return context;
    }

    public void setContext(Map<String, Object> context) {

        this.context = context;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationDecision)) {
            return false;
        }
        EvaluationDecision that = (EvaluationDecision) o;
        return decision == that.decision &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {

        return Objects.hash(decision, context);
    }

    @Override
    public String toString() {

        return "EvaluationDecision{" +
                "decision=" + decision +
                ", context=" + context +
                '}';
    }
}
