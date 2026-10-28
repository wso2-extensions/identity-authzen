# Configuration

Merge this into `<IS_HOME>/repository/conf/deployment.toml` and restart the identity server. It is written for
WSO2 Identity Server 7.3.

```toml
[[api_resources]]
name = "AuthZEN Authorization API"
identifier = "/access/v1"
requiresAuthorization = true
description = "OpenID AuthZEN Authorization API 1.0"
type = "TENANT"
disabled = false
scopes = [
    { displayName = "Evaluate access", name = "internal_authzen_evaluate", description = "Request access decisions" },
]

# Only callers holding the scope may use the decision endpoints.
[[resource.access_control]]
context = "(.*)/access/v1/evaluations?"
secure = true
http_method = "POST"
scopes = ["internal_authzen_evaluate"]

# PDP metadata is public.
[[resource.access_control]]
context = "/.well-known/authzen-configuration(.*)"
secure = false
http_method = "GET"

# Serves the endpoints on tenant paths (/t/{tenant}/access/v1/...). This list replaces the default one, so it
# repeats the defaults of IS 7.3.
[tenant_context.rewrite]
servlets = [
    "/identity/(.*)",
    "/identity/metadata/saml2",
    "/commonauth(.*)",
    "/logincontext(.*)",
    "/oidc/",
    "/samlsso(.*)",
    "/samlartresolve",
    "/passivests",
    "/longwaitstatus(.*)",
    "/push-auth/(.*)",
    "/access/v1/(.*)",
]
```

| Block | Purpose |
|---|---|
| `[[api_resources]]` | Registers the **AuthZEN Authorization API** with the `internal_authzen_evaluate` scope in every tenant. Authorize a PEP application for it to let it call the PDP. |
| `[[resource.access_control]]` for `/access/v1/evaluations?` | Requires `internal_authzen_evaluate` on the decision endpoints. Without it, every call returns `401`. |
| `[[resource.access_control]]` for `/.well-known/authzen-configuration` | Keeps the PDP metadata public. |
| `[tenant_context.rewrite]` | Serves the decision endpoints on tenant paths, `/t/{tenant}/access/v1/...`. |

## Tenant rewrite list

`[tenant_context.rewrite] servlets` **replaces** the identity server's default list instead of adding to it, so it
must repeat the defaults of your IS version, followed by `"/access/v1/(.*)"`. A missing default breaks the tenant
paths of that feature, such as tenant login. Get the defaults with:

```bash
jq '."tenant_context.rewrite.servlets"' <IS_HOME>/repository/resources/conf/default.json
```

Check the list again whenever you upgrade the identity server.
