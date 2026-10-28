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


package org.wso2.carbon.identity.authzen.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.wso2.carbon.identity.api.resource.mgt.APIResourceManager;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService;
import org.wso2.carbon.identity.authzen.api.rest.EvaluationServlet;
import org.wso2.carbon.identity.authzen.api.rest.EvaluationsServlet;
import org.wso2.carbon.identity.authzen.api.rest.MetadataServlet;
import org.wso2.carbon.identity.authzen.core.AuthZENConstants;
import org.wso2.carbon.identity.authzen.evaluation.EvaluationServiceImpl;
import org.wso2.carbon.identity.authzen.evaluation.RBACPolicyEvaluator;
import org.wso2.carbon.identity.authzen.pip.PIPServiceImpl;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.List;

import javax.servlet.ServletException;

/**
 * Wires the connector together and registers the AuthZEN endpoints.
 */
@Component(
        name = "org.wso2.carbon.identity.authzen.component",
        immediate = true
)
public class AuthZENServiceComponent {

    private static final List<String> SERVLET_ALIASES = List.of(
            AuthZENConstants.ACCESS_EVALUATION_PATH,
            AuthZENConstants.ACCESS_EVALUATIONS_PATH,
            AuthZENConstants.PDP_METADATA_PATH);

    private HttpService httpService;

    @Activate
    protected void activate() throws ServletException, NamespaceException {

        HttpService httpService = this.httpService;
        if (httpService == null) {
            throw new IllegalStateException("HttpService is not available.");
        }
        AuthZENDataHolder.setEvaluationService(
                new EvaluationServiceImpl(new PIPServiceImpl(), List.of(new RBACPolicyEvaluator())));
        httpService.registerServlet(AuthZENConstants.ACCESS_EVALUATION_PATH, new EvaluationServlet(), null, null);
        httpService.registerServlet(AuthZENConstants.ACCESS_EVALUATIONS_PATH, new EvaluationsServlet(), null, null);
        httpService.registerServlet(AuthZENConstants.PDP_METADATA_PATH, new MetadataServlet(), null, null);
    }

    @Deactivate
    protected void deactivate() {

        if (httpService != null) {
            SERVLET_ALIASES.forEach(httpService::unregister);
        }
        AuthZENDataHolder.setEvaluationService(null);
    }

    @Reference(
            name = "org.osgi.service.http.HttpService",
            service = HttpService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetHttpService"
    )
    protected void setHttpService(HttpService httpService) {

        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {

        this.httpService = null;
    }

    @Reference(
            name = "org.wso2.carbon.user.core.service.RealmService",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        AuthZENDataHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        AuthZENDataHolder.setRealmService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.api.resource.mgt.APIResourceManager",
            service = APIResourceManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAPIResourceManager"
    )
    protected void setAPIResourceManager(APIResourceManager apiResourceManager) {

        AuthZENDataHolder.setApiResourceManager(apiResourceManager);
    }

    protected void unsetAPIResourceManager(APIResourceManager apiResourceManager) {

        AuthZENDataHolder.setApiResourceManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.mgt.ApplicationManagementService",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService"
    )
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        AuthZENDataHolder.setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        AuthZENDataHolder.setApplicationManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService",
            service = AuthorizedAPIManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAuthorizedAPIManagementService"
    )
    protected void setAuthorizedAPIManagementService(
            AuthorizedAPIManagementService authorizedAPIManagementService) {

        AuthZENDataHolder.setAuthorizedAPIManagementService(authorizedAPIManagementService);
    }

    protected void unsetAuthorizedAPIManagementService(
            AuthorizedAPIManagementService authorizedAPIManagementService) {

        AuthZENDataHolder.setAuthorizedAPIManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService",
            service = RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementService"
    )
    protected void setRoleManagementService(RoleManagementService roleManagementService) {

        AuthZENDataHolder.setRoleManagementService(roleManagementService);
    }

    protected void unsetRoleManagementService(RoleManagementService roleManagementService) {

        AuthZENDataHolder.setRoleManagementService(null);
    }
}
