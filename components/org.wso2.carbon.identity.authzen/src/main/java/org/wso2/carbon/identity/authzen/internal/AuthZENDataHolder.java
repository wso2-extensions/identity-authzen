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

import org.wso2.carbon.identity.api.resource.mgt.APIResourceManager;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService;
import org.wso2.carbon.identity.authzen.evaluation.EvaluationService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Identity server services the connector uses, and the connector's evaluation service.
 */
public final class AuthZENDataHolder {

    private static RealmService realmService;
    private static APIResourceManager apiResourceManager;
    private static ApplicationManagementService applicationManagementService;
    private static AuthorizedAPIManagementService authorizedAPIManagementService;
    private static RoleManagementService roleManagementService;
    private static EvaluationService evaluationService;

    private AuthZENDataHolder() {

    }

    public static RealmService getRealmService() {

        return realmService;
    }

    public static void setRealmService(RealmService realmService) {

        AuthZENDataHolder.realmService = realmService;
    }

    public static APIResourceManager getApiResourceManager() {

        return apiResourceManager;
    }

    public static void setApiResourceManager(APIResourceManager apiResourceManager) {

        AuthZENDataHolder.apiResourceManager = apiResourceManager;
    }

    public static ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    public static void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        AuthZENDataHolder.applicationManagementService = applicationManagementService;
    }

    public static AuthorizedAPIManagementService getAuthorizedAPIManagementService() {

        return authorizedAPIManagementService;
    }

    public static void setAuthorizedAPIManagementService(
            AuthorizedAPIManagementService authorizedAPIManagementService) {

        AuthZENDataHolder.authorizedAPIManagementService = authorizedAPIManagementService;
    }

    public static RoleManagementService getRoleManagementService() {

        return roleManagementService;
    }

    public static void setRoleManagementService(RoleManagementService roleManagementService) {

        AuthZENDataHolder.roleManagementService = roleManagementService;
    }

    public static EvaluationService getEvaluationService() {

        return evaluationService;
    }

    public static void setEvaluationService(EvaluationService evaluationService) {

        AuthZENDataHolder.evaluationService = evaluationService;
    }
}
