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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class EvaluationsRequestTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void shouldApplyTopLevelDefaultsToEachEvaluation() throws Exception {

        EvaluationsRequest request = OBJECT_MAPPER.readValue("{"
                + "\"subject\":{\"type\":\"user\",\"id\":\"alice\"},"
                + "\"resource\":{\"type\":\"api_resource\",\"id\":\"/api/courses\"},"
                + "\"evaluations\":["
                + "{\"action\":{\"name\":\"courses:read\"}},"
                + "{\"action\":{\"name\":\"courses:write\"},\"subject\":{\"type\":\"user\",\"id\":\"bob\"}}"
                + "],"
                + "\"options\":{\"evaluations_semantic\":\"deny_on_first_deny\"}}", EvaluationsRequest.class);

        List<EvaluationRequest> evaluations = request.resolveEvaluations();

        Assert.assertTrue(request.isBatch());
        Assert.assertEquals(evaluations.size(), 2);
        Assert.assertEquals(evaluations.get(0).getSubject().getId(), "alice");
        Assert.assertEquals(evaluations.get(0).getResource().getId(), "/api/courses");
        Assert.assertEquals(evaluations.get(0).getAction().getName(), "courses:read");
        Assert.assertEquals(evaluations.get(1).getSubject().getId(), "bob");
        Assert.assertEquals(evaluations.get(1).getAction().getName(), "courses:write");
        Assert.assertEquals(request.getOptions().get("evaluations_semantic"), "deny_on_first_deny");
    }

    @Test
    public void shouldBehaveAsSingleEvaluationWithoutBatch() throws Exception {

        EvaluationsRequest request = OBJECT_MAPPER.readValue("{"
                + "\"subject\":{\"type\":\"user\",\"id\":\"alice\"},"
                + "\"action\":{\"name\":\"courses:read\"},"
                + "\"resource\":{\"type\":\"api_resource\",\"id\":\"/api/courses\"},"
                + "\"evaluations\":[]}", EvaluationsRequest.class);

        Assert.assertFalse(request.isBatch());
        Assert.assertTrue(request.resolveEvaluations().isEmpty());
        Assert.assertEquals(request.toSingleRequest().getAction().getName(), "courses:read");
    }

    @Test
    public void shouldOmitNullContextInResponses() throws Exception {

        String json = OBJECT_MAPPER.writeValueAsString(new EvaluationsResponse(
                List.of(new EvaluationResponse(true, null))));

        Assert.assertEquals(json, "{\"evaluations\":[{\"decision\":true}]}");
    }
}
