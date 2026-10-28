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

package org.wso2.carbon.identity.authzen.pip;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.api.resource.mgt.APIResourceManager;
import org.wso2.carbon.identity.application.common.model.APIResource;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthorizedAPI;
import org.wso2.carbon.identity.application.common.model.Scope;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENClientException;
import org.wso2.carbon.identity.authzen.core.exception.AuthZENErrorCode;
import org.wso2.carbon.identity.authzen.core.model.ResourceEntity;
import org.wso2.carbon.identity.authzen.core.model.SubjectEntity;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UniqueIDUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.List;
import java.util.Map;
import org.wso2.carbon.identity.authzen.internal.AuthZENDataHolder;

public class PIPServiceImplTest {

    private static final String TENANT = "carbon.super";
    private static final String API_IDENTIFIER = "/api/courses";
    private static final String API_ID = "api-resource-id";
    private static final String ACCOUNT_LOCKED = "http://wso2.org/claims/identity/accountLocked";
    private static final String UNLOCK_TIME = "http://wso2.org/claims/identity/unlockTime";
    private static final String ACCOUNT_DISABLED = "http://wso2.org/claims/identity/accountDisabled";

    @AfterMethod
    public void tearDown() {

        AuthZENDataHolder.setRealmService(null);
        AuthZENDataHolder.setApiResourceManager(null);
        AuthZENDataHolder.setApplicationManagementService(null);
        AuthZENDataHolder.setAuthorizedAPIManagementService(null);
    }

    @Test
    public void shouldResolveUserSubjectById() throws Exception {

        mockUserStore("user-1", "PRIMARY");

        SubjectContext subjectContext = newPipService().resolveSubject(new SubjectEntity("user", "user-1", null),
                TENANT);

        Assert.assertEquals(subjectContext, new SubjectContext("user", "user-1", TENANT));
    }

    @Test
    public void shouldReportUnknownUserAsNotFound() throws Exception {

        UniqueIDUserStoreManager userStoreManager = mockUserStore("user-1", "PRIMARY");
        Mockito.when(userStoreManager.isExistingUserWithID("alice")).thenReturn(false);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("user", "alice", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    public void shouldResolveAgentOnlyFromAgentUserStore() throws Exception {

        mockUserStore("agent-1", "AGENT");
        PIPService pipService = newPipService();

        SubjectContext agent = pipService.resolveSubject(new SubjectEntity("agent", "agent-1", null), TENANT);
        AuthZENClientException userTypeForAgent = Assert.expectThrows(AuthZENClientException.class,
                () -> pipService.resolveSubject(new SubjectEntity("user", "agent-1", null), TENANT));

        Assert.assertEquals(agent, new SubjectContext("agent", "agent-1", TENANT));
        Assert.assertEquals(userTypeForAgent.getErrorCode(), AuthZENErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    public void shouldNotResolveUserAsAgent() throws Exception {

        mockUserStore("user-1", "PRIMARY");

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("agent", "user-1", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    public void shouldReportLockedUserAsInactive() throws Exception {

        UniqueIDUserStoreManager userStoreManager = mockUserStore("user-1", "PRIMARY");
        mockAccountState(userStoreManager, "user-1", Map.of(ACCOUNT_LOCKED, "true"));

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("user", "user-1", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_INACTIVE);
        Assert.assertEquals(exception.getDescription(), "The subject's account is locked.");
    }

    @Test
    public void shouldReportUserLockedUntilLaterAsInactive() throws Exception {

        UniqueIDUserStoreManager userStoreManager = mockUserStore("user-1", "PRIMARY");
        mockAccountState(userStoreManager, "user-1", Map.of(ACCOUNT_LOCKED, "true",
                UNLOCK_TIME, String.valueOf(System.currentTimeMillis() + 3_600_000L)));

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("user", "user-1", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_INACTIVE);
    }

    @Test
    public void shouldResolveUserWhoseLockHasExpired() throws Exception {

        UniqueIDUserStoreManager userStoreManager = mockUserStore("user-1", "PRIMARY");
        mockAccountState(userStoreManager, "user-1", Map.of(ACCOUNT_LOCKED, "true",
                UNLOCK_TIME, String.valueOf(System.currentTimeMillis() - 60_000L)));

        SubjectContext subjectContext = newPipService().resolveSubject(new SubjectEntity("user", "user-1", null),
                TENANT);

        Assert.assertEquals(subjectContext, new SubjectContext("user", "user-1", TENANT));
    }

    @Test
    public void shouldReportDisabledAgentAsInactive() throws Exception {

        UniqueIDUserStoreManager userStoreManager = mockUserStore("agent-1", "AGENT");
        mockAccountState(userStoreManager, "agent-1", Map.of(ACCOUNT_DISABLED, "true"));

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("agent", "agent-1", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_INACTIVE);
        Assert.assertEquals(exception.getDescription(), "The subject's account is disabled.");
    }

    @Test
    public void shouldResolveApplicationSubject() throws Exception {

        ApplicationManagementService applicationManagementService = mockApplicationManagementService();
        Mockito.when(applicationManagementService.getApplicationBasicInfoByResourceId("app-1", TENANT))
                .thenReturn(new ApplicationBasicInfo());

        SubjectContext subjectContext = newPipService().resolveSubject(
                new SubjectEntity("application", "app-1", null), TENANT);

        Assert.assertEquals(subjectContext, new SubjectContext("application", "app-1", TENANT));
    }

    @Test
    public void shouldReportUnknownApplicationSubjectAsNotFound() throws Exception {

        mockApplicationManagementService();

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveSubject(new SubjectEntity("application", "missing", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    public void shouldResolveResourceByIdentifier() throws Exception {

        mockApiResource();

        ResourceContext resourceContext = newPipService().resolveResource(
                new ResourceEntity("api_resource", API_IDENTIFIER, null), TENANT);

        Assert.assertEquals(resourceContext,
                new ResourceContext(API_IDENTIFIER, API_ID, "api_resource", List.of("courses:read")));
    }

    @Test
    public void shouldReportUnknownResourceAsNotFound() throws Exception {

        APIResourceManager apiResourceManager = Mockito.mock(APIResourceManager.class);
        AuthZENDataHolder.setApiResourceManager(apiResourceManager);

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveResource(new ResourceEntity("api_resource", "/missing", null), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    public void shouldResolveApplicationAuthorizedForResource() throws Exception {

        ApplicationManagementService applicationManagementService = mockApplicationManagementService();
        AuthorizedAPIManagementService authorizedAPIManagementService =
                Mockito.mock(AuthorizedAPIManagementService.class);
        AuthZENDataHolder.setAuthorizedAPIManagementService(authorizedAPIManagementService);
        AuthorizedAPI authorizedAPI = Mockito.mock(AuthorizedAPI.class);
        Mockito.when(applicationManagementService.getApplicationByResourceId("app-1", TENANT))
                .thenReturn(application(true));
        Mockito.when(applicationManagementService.getAllowedAudienceForRoleAssociation("app-1", TENANT))
                .thenReturn("application");
        Mockito.when(authorizedAPIManagementService.getAuthorizedAPI("app-1", API_ID, TENANT))
                .thenReturn(authorizedAPI);
        Mockito.when(authorizedAPI.getPolicyId()).thenReturn("RBAC");
        List<Scope> scopes = List.of(scope("courses:read"));
        Mockito.when(authorizedAPI.getScopes()).thenReturn(scopes);

        ApplicationContext applicationContext = newPipService().resolveApplication("app-1", resourceContext(),
                TENANT);

        Assert.assertEquals(applicationContext,
                new ApplicationContext("app-1", "application", "RBAC", List.of("courses:read")));
        Assert.assertTrue(applicationContext.isAuthorizedForResource());
    }

    @Test
    public void shouldResolveApplicationNotAuthorizedForResource() throws Exception {

        ApplicationManagementService applicationManagementService = mockApplicationManagementService();
        AuthZENDataHolder.setAuthorizedAPIManagementService(Mockito.mock(AuthorizedAPIManagementService.class));
        Mockito.when(applicationManagementService.getApplicationByResourceId("app-1", TENANT))
                .thenReturn(application(true));
        Mockito.when(applicationManagementService.getAllowedAudienceForRoleAssociation("app-1", TENANT))
                .thenReturn("organization");

        ApplicationContext applicationContext = newPipService().resolveApplication("app-1", resourceContext(),
                TENANT);

        Assert.assertFalse(applicationContext.isAuthorizedForResource());
        Assert.assertTrue(applicationContext.getAuthorizedScopes().isEmpty());
        Assert.assertEquals(applicationContext.getRoleAudience(), "organization");
    }

    @Test
    public void shouldReportUnknownApplicationAsNotFound() throws Exception {

        mockApplicationManagementService();
        AuthZENDataHolder.setAuthorizedAPIManagementService(Mockito.mock(AuthorizedAPIManagementService.class));

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveApplication("missing", resourceContext(), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    public void shouldReportDisabledApplication() throws Exception {

        ApplicationManagementService applicationManagementService = mockApplicationManagementService();
        AuthZENDataHolder.setAuthorizedAPIManagementService(Mockito.mock(AuthorizedAPIManagementService.class));
        Mockito.when(applicationManagementService.getApplicationByResourceId("app-1", TENANT))
                .thenReturn(application(false));

        AuthZENClientException exception = Assert.expectThrows(AuthZENClientException.class,
                () -> newPipService().resolveApplication("app-1", resourceContext(), TENANT));

        Assert.assertEquals(exception.getErrorCode(), AuthZENErrorCode.APPLICATION_DISABLED);
    }

    @Test
    public void shouldSupportPipContextPojoBehavior() {

        SubjectContext subjectContext = new SubjectContext("user", "user-1", TENANT);
        ApplicationContext applicationContext = new ApplicationContext("app-1", "organization", "RBAC",
                List.of("courses:read"));
        PIPContext first = new PIPContext(subjectContext, resourceContext(), applicationContext);
        PIPContext second = new PIPContext();
        second.setSubjectContext(subjectContext);
        second.setResourceContext(resourceContext());
        second.setApplicationContext(applicationContext);

        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
        Assert.assertTrue(first.toString().contains("applicationContext"));
    }

    private static PIPService newPipService() {

        return new PIPServiceImpl(new DefaultSubjectResolver(tenantDomain -> 1, () -> "AGENT"),
                new DefaultResourceResolver(), new DefaultApplicationResolver());
    }

    private static UniqueIDUserStoreManager mockUserStore(String userId, String userStoreDomain) throws Exception {

        RealmService realmService = Mockito.mock(RealmService.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UniqueIDUserStoreManager userStoreManager = Mockito.mock(UniqueIDUserStoreManager.class);
        User user = Mockito.mock(User.class);
        AuthZENDataHolder.setRealmService(realmService);

        Mockito.when(realmService.getTenantUserRealm(1)).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.isExistingUserWithID(userId)).thenReturn(true);
        Mockito.when(userStoreManager.getUserWithID(userId, null, null)).thenReturn(user);
        Mockito.when(user.getUserStoreDomain()).thenReturn(userStoreDomain);
        return userStoreManager;
    }

    private static void mockAccountState(UniqueIDUserStoreManager userStoreManager, String userId,
                                         Map<String, String> claims) throws Exception {

        Mockito.when(userStoreManager.getUserClaimValuesWithID(Mockito.eq(userId), Mockito.any(String[].class),
                Mockito.isNull())).thenReturn(claims);
    }

    private static ServiceProvider application(boolean enabled) {

        ServiceProvider application = new ServiceProvider();
        application.setApplicationEnabled(enabled);
        return application;
    }

    private static ApplicationManagementService mockApplicationManagementService() {

        ApplicationManagementService applicationManagementService = Mockito.mock(ApplicationManagementService.class);
        AuthZENDataHolder.setApplicationManagementService(applicationManagementService);
        return applicationManagementService;
    }

    private static void mockApiResource() throws Exception {

        APIResourceManager apiResourceManager = Mockito.mock(APIResourceManager.class);
        APIResource apiResource = Mockito.mock(APIResource.class);
        AuthZENDataHolder.setApiResourceManager(apiResourceManager);
        Mockito.when(apiResourceManager.getAPIResourceByIdentifier(API_IDENTIFIER, TENANT)).thenReturn(apiResource);
        Mockito.when(apiResource.getIdentifier()).thenReturn(API_IDENTIFIER);
        Mockito.when(apiResource.getId()).thenReturn(API_ID);
        List<Scope> scopes = List.of(scope("courses:read"));
        Mockito.when(apiResource.getScopes()).thenReturn(scopes);
    }

    private static Scope scope(String name) {

        Scope scope = Mockito.mock(Scope.class);
        Mockito.when(scope.getName()).thenReturn(name);
        return scope;
    }

    private static ResourceContext resourceContext() {

        return new ResourceContext(API_IDENTIFIER, API_ID, "api_resource", List.of("courses:read"));
    }
}
