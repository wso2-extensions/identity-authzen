# API reference

The connector implements the [OpenID AuthZEN Authorization API 1.0](https://openid.net/specs/authorization-api-1_0.html).
See also [openapi.yaml](openapi.yaml).

| Endpoint | Method | Scope |
|---|---|---|
| `/access/v1/evaluation` | POST | `internal_authzen_evaluate` |
| `/access/v1/evaluations` | POST | `internal_authzen_evaluate` |
| `/.well-known/authzen-configuration` | GET | none |

For a tenant, use `/t/{tenant}/access/v1/...` and `/.well-known/authzen-configuration/t/{tenant}`.

Callers authenticate with an access token or basic authentication. Requests are JSON, at most 1 MB; unknown fields
are ignored. An `X-Request-ID` header is echoed in the response.

## POST /access/v1/evaluation

```json
{
  "subject":  { "type": "user", "id": "7c02403e-bf0d-452d-ab2a-388083da9216" },
  "action":   { "name": "orders:read" },
  "resource": { "type": "api_resource", "id": "https://api.example.com/orders",
                "properties": { "app_id": "6538ec89-9529-46f6-94e9-08d7328b0573" } }
}
```

| Field | Value |
|---|---|
| `subject.type` | `user`, `agent` or `application` |
| `subject.id` | The user id or the application id |
| `action.name` | A scope of the API resource |
| `resource.type` | `api_resource` |
| `resource.id` | The identifier of the API resource |
| `resource.properties.app_id` | The application the access is for. Defaults to the application of the caller's access token; without either, the decision is a deny. |

```json
{ "decision": true, "context": { "reason_admin": { "200": "Permitted by the RBAC policy." } } }
```

A deny is a `200` with `"decision": false`. See the [decision model](decision-model.md) for the rules and reasons.

## POST /access/v1/evaluations

Top-level `subject`, `action`, `resource` and `context` are defaults for the entries of `evaluations` (at most
100). A field an entry sets replaces the default as a whole.

```json
{
  "subject":  { "type": "user", "id": "7c02403e-bf0d-452d-ab2a-388083da9216" },
  "resource": { "type": "api_resource", "id": "https://api.example.com/orders" },
  "evaluations": [
    { "action": { "name": "orders:read" } },
    { "action": { "name": "orders:write" } }
  ],
  "options": { "evaluations_semantic": "execute_all" }
}
```

```json
{
  "evaluations": [
    { "decision": true, "context": { "reason_admin": { "200": "Permitted by the RBAC policy." } } },
    { "decision": false, "context": { "reason_admin": { "403": "The RBAC policy does not permit action orders:write on resource https://api.example.com/orders." } } }
  ]
}
```

`evaluations_semantic` is `execute_all` (default), `deny_on_first_deny` or `permit_on_first_permit`; the last two
stop at the first deny or permit. An invalid entry returns `"decision": false` with `context.error`. Without
`evaluations`, the request is a single evaluation.

## GET /.well-known/authzen-configuration

```json
{
  "policy_decision_point": "https://localhost:9443",
  "access_evaluation_endpoint": "https://localhost:9443/access/v1/evaluation",
  "access_evaluations_endpoint": "https://localhost:9443/access/v1/evaluations"
}
```

## Errors

```json
{ "error": "missing_subject", "error_description": "Subject is required.", "error_code": "AZ-40001" }
```

| Status | `error_code` | When |
|---|---|---|
| 400 | `AZ-40000` | Invalid JSON, empty field, invalid `app_id` or batch options |
| 400 | `AZ-40001`, `AZ-40002`, `AZ-40003` | Missing `subject`, `action` or `resource` |
| 400 | `AZ-40004`, `AZ-40005` | Unsupported subject or resource type |
| 401, 403 | | No credentials, or no `internal_authzen_evaluate` scope |
| 413 | `AZ-41300` | Body larger than 1 MB |
| 415 | `AZ-41500` | `Content-Type` is not `application/json` |
| 500 | `AZ-50001`, `AZ-50002` | Server error; details are in the server log |
