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

package org.wso2.carbon.identity.authzen.core.validation;

import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.model.ActionEntity;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class EvaluationRequestValidatorTest {

    @Test
    public void shouldRejectMissingSubject() {

        EvaluationRequest request = new EvaluationRequest(null, new ActionEntity("read", null),
                new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.MISSING_SUBJECT);
    }

    @Test
    public void shouldRejectMissingAction() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null), null,
                new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.MISSING_ACTION);
    }

    @Test
    public void shouldRejectMissingResource() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), null, null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.MISSING_RESOURCE);
    }

    @Test
    public void shouldRejectBlankSubjectType() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity(" ", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getDescription(), "Subject type is required.");
    }

    @Test
    public void shouldRejectBlankSubjectId() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", " ", null),
                new ActionEntity("read", null), new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getDescription(), "Subject id is required.");
    }

    @Test
    public void shouldRejectUnsupportedSubjectType() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("group", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.UNSUPPORTED_SUBJECT_TYPE);
    }

    @Test
    public void shouldRejectBlankActionName() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity(" ", null), new ResourceEntity("api_resource", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getDescription(), "Action name is required.");
    }

    @Test
    public void shouldRejectBlankResourceType() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), new ResourceEntity(" ", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getDescription(), "Resource type is required.");
    }

    @Test
    public void shouldRejectBlankResourceId() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("api_resource", " ", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
        Assert.assertEquals(exception.getDescription(), "Resource id is required.");
    }

    @Test
    public void shouldRejectUnsupportedResourceType() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("document", "resource-1", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.UNSUPPORTED_RESOURCE_TYPE);
    }

    @Test
    public void shouldRejectApplicationResourceType() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("application", "app-id", null), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.UNSUPPORTED_RESOURCE_TYPE);
    }

    @Test
    public void shouldRejectNonStringAppId() {

        EvaluationRequest request = new EvaluationRequest(new SubjectEntity("user", "alice", null),
                new ActionEntity("read", null), new ResourceEntity("api_resource", "resource-1",
                Map.of("app_id", 42)), null);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> EvaluationRequestValidator.validate(request));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.INVALID_REQUEST);
    }

    @Test
    public void shouldAcceptValidRequests() throws AuthZENClientException {

        for (String subjectType : new String[]{"user", "agent", "application"}) {
            EvaluationRequestValidator.validate(new EvaluationRequest(new SubjectEntity(subjectType, "id-1", null),
                    new ActionEntity("courses:read", null),
                    new ResourceEntity("api_resource", "/api/courses", Map.of("app_id", "app-1")), null));
        }
    }
}
