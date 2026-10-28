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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.core.util.AuthZENUtils;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * PDP metadata: {@code GET /.well-known/authzen-configuration}, and
 * {@code GET /.well-known/authzen-configuration/t/{tenant}} for a tenant's PDP at {@code https://host/t/{tenant}}.
 */
public class MetadataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(MetadataServlet.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SUPER_TENANT_DOMAIN = "carbon.super";
    private static final String TENANT_PATH_PREFIX = "/t/";
    private static final int INVALID_TENANT_ID = -1;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String tenantDomain = resolveTenantDomain(req.getPathInfo());
        if (tenantDomain == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        try {
            metadata.put("policy_decision_point", buildUrl(tenantDomain, null));
            metadata.put("access_evaluation_endpoint",
                    buildUrl(tenantDomain, AuthZENConstants.ACCESS_EVALUATION_PATH));
            metadata.put("access_evaluations_endpoint",
                    buildUrl(tenantDomain, AuthZENConstants.ACCESS_EVALUATIONS_PATH));
        } catch (URLBuilderException e) {
            LOG.error(AuthZENUtils.sanitizeForLog(
                    "Error while building the AuthZEN metadata URLs for tenant: " + tenantDomain), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        OBJECT_MAPPER.writeValue(resp.getWriter(), metadata);
    }

    /**
     * Returns the tenant the metadata is requested for, or {@code null} if the path does not name an existing
     * tenant.
     */
    private static String resolveTenantDomain(String pathInfo) {

        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return SUPER_TENANT_DOMAIN;
        }
        if (!pathInfo.startsWith(TENANT_PATH_PREFIX)) {
            return null;
        }
        String tenantDomain = pathInfo.substring(TENANT_PATH_PREFIX.length());
        if (tenantDomain.endsWith("/")) {
            tenantDomain = tenantDomain.substring(0, tenantDomain.length() - 1);
        }
        if (tenantDomain.isEmpty() || tenantDomain.contains("/")) {
            return null;
        }
        try {
            return IdentityTenantUtil.getTenantId(tenantDomain) == INVALID_TENANT_ID ? null : tenantDomain;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String buildUrl(String tenantDomain, String path) throws URLBuilderException {

        ServiceURLBuilder builder = ServiceURLBuilder.create().setTenant(tenantDomain);
        if (path != null) {
            builder.addPath(path);
        }
        return builder.build().getAbsolutePublicURL();
    }
}
