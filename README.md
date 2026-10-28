# Identity AuthZEN

An [OpenID AuthZEN Authorization API 1.0](https://openid.net/specs/authorization-api-1_0.html) policy decision
point (PDP) for WSO2 Identity Server 7.3.

It answers *"may this subject perform this action on this resource?"* from the roles, API resources and
application authorizations already configured in the identity server. A decision for a user and an application
matches whether that application's access token for the user would carry the scope.

```bash
curl -s -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  https://localhost:9443/access/v1/evaluation -d '{
    "subject":  { "type": "user", "id": "7c02403e-bf0d-452d-ab2a-388083da9216" },
    "action":   { "name": "orders:read" },
    "resource": { "type": "api_resource", "id": "https://api.example.com/orders",
                  "properties": { "app_id": "6538ec89-9529-46f6-94e9-08d7328b0573" } }
  }'
```

```json
{ "decision": true, "context": { "reason_admin": { "200": "Permitted by the RBAC policy." } } }
```

## Features

- Access Evaluation and Access Evaluations APIs, and PDP metadata.
- Users, agents and applications as subjects.
- The identity server's token rules: role audiences, roles through groups, `RBAC` and `No Policy`.
- Per-tenant endpoints, secured with the `internal_authzen_evaluate` scope.
- Audit log of every decision.

Not supported: sub-organizations (B2B), federated users that are not provisioned locally, the search APIs, and
conditions from `context`.

## Documentation

- [Installation](docs/installation.md) and [configuration](docs/configuration.md)
- [Getting started](docs/getting-started.md)
- [Decision model](docs/decision-model.md)
- [PEP integration](docs/pep-integration.md)
- [API reference](docs/api-reference.md) ([OpenAPI](docs/openapi.yaml))

## Building

```bash
mvn clean install
```

Requires JDK 21. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE).
