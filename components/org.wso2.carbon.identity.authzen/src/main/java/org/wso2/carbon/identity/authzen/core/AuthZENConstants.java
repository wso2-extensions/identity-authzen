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

package org.wso2.carbon.identity.authzen.core;

public final class AuthZENConstants {

    public static final String ACCESS_EVALUATION_PATH = "/access/v1/evaluation";
    public static final String ACCESS_EVALUATIONS_PATH = "/access/v1/evaluations";
    public static final String PDP_METADATA_PATH = "/.well-known/authzen-configuration";

    public static final String SUBJECT_TYPE_USER = "user";
    public static final String SUBJECT_TYPE_AGENT = "agent";
    public static final String SUBJECT_TYPE_APPLICATION = "application";

    public static final String RESOURCE_TYPE_API_RESOURCE = "api_resource";

    /**
     * Resource property naming the application on whose behalf the access is evaluated.
     */
    public static final String RESOURCE_PROPERTY_APP_ID = "app_id";

    public static final String POLICY_RBAC = "RBAC";
    public static final String POLICY_NO_POLICY = "NO POLICY";

    public static final String CONTEXT_REASON_ADMIN = "reason_admin";
    public static final String CONTEXT_ERROR = "error";

    public static final String OPTION_EVALUATIONS_SEMANTIC = "evaluations_semantic";
    public static final String SEMANTIC_EXECUTE_ALL = "execute_all";
    public static final String SEMANTIC_DENY_ON_FIRST_DENY = "deny_on_first_deny";
    public static final String SEMANTIC_PERMIT_ON_FIRST_PERMIT = "permit_on_first_permit";

    public static final String HEADER_X_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String SCOPE_INTERNAL_AUTHZEN_EVALUATE = "internal_authzen_evaluate";

    private AuthZENConstants() {
    }
}
