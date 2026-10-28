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

import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.model.ActionEntity;
import org.wso2.carbon.identity.authzen.core.model.EvaluationRequest;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;

import java.util.Set;

public final class EvaluationRequestValidator {

    private static final Set<String> SUPPORTED_SUBJECT_TYPES = Set.of(AuthZENConstants.SUBJECT_TYPE_USER,
            AuthZENConstants.SUBJECT_TYPE_AGENT, AuthZENConstants.SUBJECT_TYPE_APPLICATION);
    private static final Set<String> SUPPORTED_RESOURCE_TYPES = Set.of(AuthZENConstants.RESOURCE_TYPE_API_RESOURCE);

    private EvaluationRequestValidator() {
    }

    public static void validate(EvaluationRequest request) throws AuthZENClientException {

        if (request == null) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Evaluation request is required.");
        }
        validateSubject(request.getSubject());
        validateAction(request.getAction());
        validateResource(request.getResource());
    }

    private static void validateSubject(SubjectEntity subject) throws AuthZENClientException {

        if (subject == null) {
            throw new AuthZENClientException(AuthZENErrorCode.MISSING_SUBJECT, "Subject is required.");
        }
        if (!hasText(subject.getType())) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Subject type is required.");
        }
        if (!hasText(subject.getId())) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Subject id is required.");
        }
        if (!SUPPORTED_SUBJECT_TYPES.contains(subject.getType())) {
            throw new AuthZENClientException(AuthZENErrorCode.UNSUPPORTED_SUBJECT_TYPE,
                    "Unsupported subject type: " + subject.getType());
        }
    }

    private static void validateAction(ActionEntity action) throws AuthZENClientException {

        if (action == null) {
            throw new AuthZENClientException(AuthZENErrorCode.MISSING_ACTION, "Action is required.");
        }
        if (!hasText(action.getName())) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Action name is required.");
        }
    }

    private static void validateResource(ResourceEntity resource) throws AuthZENClientException {

        if (resource == null) {
            throw new AuthZENClientException(AuthZENErrorCode.MISSING_RESOURCE, "Resource is required.");
        }
        if (!hasText(resource.getType())) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Resource type is required.");
        }
        if (!hasText(resource.getId())) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Resource id is required.");
        }
        if (!SUPPORTED_RESOURCE_TYPES.contains(resource.getType())) {
            throw new AuthZENClientException(AuthZENErrorCode.UNSUPPORTED_RESOURCE_TYPE,
                    "Unsupported resource type: " + resource.getType());
        }
        Object appId = resource.getProperties() != null
                ? resource.getProperties().get(AuthZENConstants.RESOURCE_PROPERTY_APP_ID) : null;
        if (appId != null && !(appId instanceof String && hasText((String) appId))) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST,
                    "Resource property app_id must be a non-empty string.");
        }
    }

    private static boolean hasText(String value) {

        return value != null && !value.trim().isEmpty();
    }
}
