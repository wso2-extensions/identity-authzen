# Decision model

The connector has no policies of its own. It decides from the API resources, roles and application
authorizations configured in the identity server, with the same rules the identity server uses to issue tokens.

## Mapping

| AuthZEN | Identity server |
|---|---|
| `subject` `user`, `agent` | A user or an agent, by user id. Usernames are not accepted. |
| `subject` `application` | An application, by application id |
| `resource` `api_resource` | An API resource, by its identifier |
| `action.name` | A scope of that API resource |
| `resource.properties.app_id` | The application the access is for |

Other properties and `context` are ignored.

## Which application

An evaluation runs for one application, the first of:

1. the subject, when it is an application;
2. `resource.properties.app_id`;
3. the application of the caller's access token.

Without any of these the decision is a deny, because access in the identity server is always granted through an
application. Rule 3 means a PEP that omits `app_id` gets decisions for its own application; see
[PEP integration](pep-integration.md#choosing-the-application).

## Rules

The subject, resource and application must exist, the action must be a scope of the resource, and the
application must be authorized for that scope. A locked or disabled account and a disabled application get a deny,
as they get no tokens. Then:

| Case | Permitted when |
|---|---|
| The subject is an application | Always |
| `No Policy` | Always |
| `RBAC`, application role audience | A role of the subject **in that application** grants the scope |
| `RBAC`, organization role audience | An **organization** role of the subject grants the scope |

Roles assigned through groups count. An entity that does not exist gives a deny, not an error. An invalid request
gives `400`.

## Examples

alice has the organization role `reader` (`orders:read`). bob has the role `writer` of Portal (`orders:write`).
Portal uses application roles; Reports uses organization roles. Both are authorized for both scopes.

| Subject | Action | `app_id` | Decision |
|---|---|---|---|
| alice | `orders:read` | Reports | permit |
| alice | `orders:read` | Portal | deny: Portal only counts its own roles |
| bob | `orders:write` | Portal | permit |
| bob | `orders:write` | Reports | deny: bob's role belongs to Portal |
| alice | `orders:read` | none, basic authentication | deny: no application |

## Reasons

Each decision carries `context.reason_admin`, keyed `200` for a permit or `403` for a deny:

| Reason | Meaning |
|---|---|
| `Permitted by the <policy> policy.` | Permit |
| `Subject not found.`, `Resource not found.`, `Application not found.` | The entity does not exist in the tenant |
| `The subject's account is locked.`, `The subject's account is disabled.` | The user or agent may not sign in |
| `The application is disabled.` | The application is disabled |
| `Action <action> is not defined for resource <resource>.` | The action is not a scope of the resource |
| `No application to evaluate for. ...` | No application from the subject, `app_id` or the caller's token |
| `The application is not authorized for resource <resource>.` | No authorization for the resource |
| `The application is not authorized for action <action> on resource <resource>.` | The authorization lacks the scope |
| `The <policy> policy does not permit action <action> on resource <resource>.` | No eligible role grants the scope |

Reasons are for administrators; don't show them to end users.

## Audit log

Every decision is written to `repository/logs/audit.log` as `AuthZEN-Evaluate`, with the caller's user id, the
subject, resource, action, application and `X-Request-ID`, and the result.
