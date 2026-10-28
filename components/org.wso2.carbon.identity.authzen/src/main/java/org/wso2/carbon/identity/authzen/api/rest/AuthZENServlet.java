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

package org.wso2.carbon.identity.authzen.api.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENServerException;
import org.wso2.carbon.identity.authzen.core.model.EvaluationDecision;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;
import org.wso2.carbon.identity.authzen.core.util.AuthZENUtils;
import org.wso2.carbon.identity.authzen.evaluation.EvaluationService;
import org.wso2.carbon.identity.core.bean.context.MessageContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

/**
 * Base for the AuthZEN POST endpoints: JSON handling, request id echo, error responses, caller resolution and
 * audit logging.
 */
abstract class AuthZENServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(AuthZENServlet.class);
    private static final Log AUDIT_LOG = LogFactory.getLog("AUDIT_LOG");
    private static final String AUDIT_MESSAGE =
            "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result : %s";

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final int MAX_REQUEST_ID_LENGTH = 256;

    // Request attribute and parameters set by the identity server's authentication valve.
    private static final String AUTH_CONTEXT_ATTRIBUTE = "auth-context";
    private static final String SERVICE_PROVIDER_UUID = "serviceProviderUUID";
    private static final String SERVICE_PROVIDER_TENANT_DOMAIN = "serviceProviderTenantDomain";

    /**
     * Handles a parsed request body and returns the response body.
     */
    protected abstract Object handle(byte[] body, CallContext callContext) throws AuthZENException, IOException;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        prepareResponse(req, resp);
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains(CONTENT_TYPE_JSON)) {
            writeError(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type",
                    "Content-Type must be application/json", "AZ-41500");
            return;
        }
        byte[] body = readBody(req);
        if (body.length > MAX_REQUEST_BYTES) {
            writeError(resp, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "request_too_large",
                    "Request body exceeds " + MAX_REQUEST_BYTES + " bytes", "AZ-41300");
            return;
        }

        try {
            Object response = handle(body, new CallContext(req));
            resp.setStatus(HttpServletResponse.SC_OK);
            OBJECT_MAPPER.writeValue(resp.getWriter(), response);
        } catch (JsonProcessingException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid_request",
                    "Request body is not valid JSON", AuthZENErrorCode.INVALID_REQUEST.getCode());
        } catch (AuthZENClientException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e);
        } catch (AuthZENException e) {
            LOG.error(AuthZENUtils.sanitizeForLog("Error while processing an AuthZEN request: " + e.getDescription()),
                    e);
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        prepareResponse(req, resp);
        writeError(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method_not_allowed", "Only POST is supported",
                "AZ-40500");
    }

    protected static void requireBody(Object request) throws AuthZENClientException {

        if (request == null) {
            throw new AuthZENClientException(AuthZENErrorCode.INVALID_REQUEST, "Request body is required.");
        }
    }

    protected static EvaluationService getEvaluationService() throws AuthZENServerException {

        EvaluationService evaluationService = AuthZENDataHolder.getEvaluationService();
        if (evaluationService == null) {
            throw new AuthZENServerException(AuthZENErrorCode.INTERNAL_ERROR, "EvaluationService is not available.");
        }
        return evaluationService;
    }

    protected static void auditDecision(CallContext callContext, String operation, SubjectEntity subject,
                                        String action, ResourceEntity resource, EvaluationDecision decision) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resource", resource != null ? resource.getId() : null);
        data.put("action", action);
        data.put("app_id", resource != null && resource.getProperties() != null
                ? resource.getProperties().get(AuthZENConstants.RESOURCE_PROPERTY_APP_ID) : null);
        data.put("caller_app_id", callContext.getCallerApplicationId());
        data.put("request_id", callContext.getRequestId());
        String result = decision == null ? "Error" : decision.isDecision() ? "Permit" : "Deny";
        audit(callContext, operation, subject, data, result);
    }

    protected static void audit(CallContext callContext, String operation, SubjectEntity subject,
                                Map<String, Object> data, String result) {

        if (!AUDIT_LOG.isInfoEnabled()) {
            return;
        }
        String target = subject != null ? subject.getType() + ":" + subject.getId() : null;
        StringBuilder dataString = new StringBuilder();
        data.forEach((key, value) -> {
            if (value != null) {
                dataString.append(dataString.length() > 0 ? ", " : "").append(key).append(" : ").append(value);
            }
        });
        AUDIT_LOG.info(AuthZENUtils.sanitizeForLog(String.format(AUDIT_MESSAGE, callContext.getInitiator(), operation,
                target, dataString, result)));
    }

    private static void prepareResponse(HttpServletRequest req, HttpServletResponse resp) {

        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding("UTF-8");
        String requestId = readRequestId(req);
        if (requestId != null) {
            resp.setHeader(AuthZENConstants.HEADER_X_REQUEST_ID, requestId);
        }
    }

    private static byte[] readBody(HttpServletRequest req) throws IOException {

        if (req.getContentLengthLong() > MAX_REQUEST_BYTES) {
            return new byte[MAX_REQUEST_BYTES + 1];
        }
        try (InputStream inputStream = req.getInputStream()) {
            return inputStream.readNBytes(MAX_REQUEST_BYTES + 1);
        }
    }

    private static String readRequestId(HttpServletRequest req) {

        String requestId = req.getHeader(AuthZENConstants.HEADER_X_REQUEST_ID);
        if (requestId == null || requestId.isEmpty() || requestId.length() > MAX_REQUEST_ID_LENGTH
                || !requestId.chars().allMatch(c -> c >= 0x20 && c < 0x7f)) {
            return null;
        }
        return requestId;
    }

    private static void writeError(HttpServletResponse resp, int status, AuthZENException e) throws IOException {

        writeError(resp, status, e.getErrorCode().name().toLowerCase(Locale.ROOT), e.getDescription(),
                e.getErrorCode().getCode());
    }

    private static void writeError(HttpServletResponse resp, int status, String error, String description,
                                   String code) throws IOException {

        Map<String, String> errorBody = new LinkedHashMap<>();
        errorBody.put("error", error);
        errorBody.put("error_description", description);
        errorBody.put("error_code", code);
        resp.setStatus(status);
        OBJECT_MAPPER.writeValue(resp.getWriter(), errorBody);
    }

    /**
     * Tenant and caller of a request.
     */
    protected static final class CallContext {

        private final String tenantDomain;
        private final String callerApplicationId;
        private final String initiator;
        private final String requestId;

        private CallContext(HttpServletRequest req) {

            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            this.tenantDomain = carbonContext.getTenantDomain();
            this.callerApplicationId = resolveCallerApplicationId(req, tenantDomain);
            // Audit logs identify the caller by id, never by username.
            String userId = carbonContext.getUserId();
            this.initiator = userId != null ? userId
                    : callerApplicationId != null ? "application:" + callerApplicationId : "unknown";
            this.requestId = readRequestId(req);
        }

        public String getTenantDomain() {

            return tenantDomain;
        }

        /**
         * Application of the OAuth client that called the endpoint, if it belongs to the request's tenant.
         */
        public String getCallerApplicationId() {

            return callerApplicationId;
        }

        public String getInitiator() {

            return initiator;
        }

        public String getRequestId() {

            return requestId;
        }

        private static String resolveCallerApplicationId(HttpServletRequest req, String tenantDomain) {

            Object authContext = req.getAttribute(AUTH_CONTEXT_ATTRIBUTE);
            if (!(authContext instanceof MessageContext)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            MessageContext<Object, Object> messageContext = (MessageContext<Object, Object>) authContext;
            Object applicationId = messageContext.getParameter(SERVICE_PROVIDER_UUID);
            Object applicationTenant = messageContext.getParameter(SERVICE_PROVIDER_TENANT_DOMAIN);
            if (!(applicationId instanceof String) || !Objects.equals(applicationTenant, tenantDomain)) {
                return null;
            }
            return (String) applicationId;
        }
    }
}
