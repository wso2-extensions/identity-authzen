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

package org.wso2.carbon.identity.authzen.core.exception;

public enum AuthZENErrorCode {

    INVALID_REQUEST("AZ-40000", "Invalid request."),
    MISSING_SUBJECT("AZ-40001", "Subject is required."),
    MISSING_ACTION("AZ-40002", "Action is required."),
    MISSING_RESOURCE("AZ-40003", "Resource is required."),
    UNSUPPORTED_SUBJECT_TYPE("AZ-40004", "Unsupported subject type."),
    UNSUPPORTED_RESOURCE_TYPE("AZ-40005", "Unsupported resource type."),
    SUBJECT_INACTIVE("AZ-40301", "The subject's account is not active."),
    APPLICATION_DISABLED("AZ-40302", "The application is disabled."),
    SUBJECT_NOT_FOUND("AZ-40401", "Subject not found."),
    RESOURCE_NOT_FOUND("AZ-40402", "Resource not found."),
    APPLICATION_NOT_FOUND("AZ-40403", "Application not found."),
    EVALUATION_FAILED("AZ-50001", "Evaluation failed."),
    INTERNAL_ERROR("AZ-50002", "Internal server error.");

    private final String code;
    private final String defaultMessage;

    AuthZENErrorCode(String code, String defaultMessage) {

        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {

        return code;
    }

    public String getDefaultMessage() {

        return defaultMessage;
    }

    /**
     * Whether the error means an entity named in the request does not exist or may not be granted access, as a
     * locked account or a disabled application. Such errors are answered with a deny decision instead of an error
     * response, so that callers cannot probe which entities exist.
     */
    public boolean isDenial() {

        return this == SUBJECT_NOT_FOUND || this == RESOURCE_NOT_FOUND || this == APPLICATION_NOT_FOUND
                || this == SUBJECT_INACTIVE || this == APPLICATION_DISABLED;
    }
}
