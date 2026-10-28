# Getting started

Set up an API, a user, an application and a PEP with the management APIs, then ask for decisions. You need the
connector [installed](installation.md), `curl` and `jq`.

```bash
export IS=https://localhost:9443
```

## 1. Check the PDP

```bash
curl -sk $IS/.well-known/authzen-configuration | jq
```

```json
{
  "policy_decision_point": "https://localhost:9443",
  "access_evaluation_endpoint": "https://localhost:9443/access/v1/evaluation",
  "access_evaluations_endpoint": "https://localhost:9443/access/v1/evaluations"
}
```

## 2. Register an API resource

```bash
API_ID=$(curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/api/server/v1/api-resources -d '{
    "identifier": "https://api.example.com/orders",
    "name": "Orders API",
    "requiresAuthorization": true,
    "scopes": [
      { "name": "orders:read",  "displayName": "Read orders" },
      { "name": "orders:write", "displayName": "Write orders" }
    ]
  }' | jq -r .id)
```

## 3. Create a user and a role

```bash
ALICE_ID=$(curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/scim2/Users -d '{
    "userName": "alice",
    "password": "Alice@12345",
    "emails": [{ "primary": true, "value": "alice@example.com" }]
  }' | jq -r .id)

curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/scim2/v2/Roles -d "{
    \"displayName\": \"order-reader\",
    \"permissions\": [{ \"value\": \"orders:read\" }],
    \"users\": [{ \"value\": \"$ALICE_ID\" }]
  }"
```

AuthZEN requests name alice by her user id, `$ALICE_ID`.

## 4. Create the application alice uses

```bash
curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/api/server/v1/applications -d '{
    "name": "Orders Portal",
    "associatedRoles": { "allowedAudience": "ORGANIZATION", "roles": [] },
    "inboundProtocolConfiguration": { "oidc": {
      "grantTypes": ["authorization_code"],
      "callbackURLs": ["https://portal.example.com/callback"] } }
  }'
APP_ID=$(curl -sk -u admin:admin "$IS/api/server/v1/applications?filter=name%20eq%20Orders%20Portal" \
  | jq -r '.applications[0].id')

curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/api/server/v1/applications/$APP_ID/authorized-apis -d "{
    \"id\": \"$API_ID\",
    \"policyIdentifier\": \"RBAC\",
    \"scopes\": [\"orders:read\", \"orders:write\"]
  }"
```

## 5. Create the PEP application

```bash
AUTHZEN_API_ID=$(curl -sk -u admin:admin \
  "$IS/api/server/v1/api-resources?filter=identifier%20eq%20/access/v1" | jq -r '.apiResources[0].id')

curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/api/server/v1/applications -d '{
    "name": "Orders Gateway",
    "inboundProtocolConfiguration": { "oidc": { "grantTypes": ["client_credentials"] } }
  }'
PEP_ID=$(curl -sk -u admin:admin "$IS/api/server/v1/applications?filter=name%20eq%20Orders%20Gateway" \
  | jq -r '.applications[0].id')

curl -sk -u admin:admin -H 'Content-Type: application/json' \
  -X POST $IS/api/server/v1/applications/$PEP_ID/authorized-apis -d "{
    \"id\": \"$AUTHZEN_API_ID\",
    \"policyIdentifier\": \"RBAC\",
    \"scopes\": [\"internal_authzen_evaluate\"]
  }"

PEP_CREDENTIALS=$(curl -sk -u admin:admin $IS/api/server/v1/applications/$PEP_ID/inbound-protocols/oidc \
  | jq -r '.clientId + ":" + .clientSecret')
TOKEN=$(curl -sk -u "$PEP_CREDENTIALS" -X POST $IS/oauth2/token \
  -d 'grant_type=client_credentials&scope=internal_authzen_evaluate' \
  | jq -r .access_token)
```

## 6. Ask for a decision

```bash
curl -sk -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -X POST $IS/access/v1/evaluation -d "{
    \"subject\":  { \"type\": \"user\", \"id\": \"$ALICE_ID\" },
    \"action\":   { \"name\": \"orders:read\" },
    \"resource\": { \"type\": \"api_resource\", \"id\": \"https://api.example.com/orders\",
                  \"properties\": { \"app_id\": \"$APP_ID\" } }
  }" | jq
```

```json
{
  "decision": true,
  "context": {
    "reason_admin": {
      "200": "Permitted by the RBAC policy."
    }
  }
}
```

With `orders:write` the decision is `false`: no role of alice's grants it. Without `app_id` it is also `false`,
because the evaluation then runs for the Orders Gateway, which is not authorized for the Orders API; see
[choosing the application](pep-integration.md#choosing-the-application).

## 7. Clean up

```bash
ROLE_ID=$(curl -sk -u admin:admin "$IS/scim2/v2/Roles?filter=displayName%20eq%20order-reader" \
  | jq -r '.Resources[0].id')
curl -sk -u admin:admin -X DELETE $IS/api/server/v1/applications/$PEP_ID
curl -sk -u admin:admin -X DELETE $IS/api/server/v1/applications/$APP_ID
curl -sk -u admin:admin -X DELETE $IS/scim2/v2/Roles/$ROLE_ID
curl -sk -u admin:admin -X DELETE $IS/scim2/Users/$ALICE_ID
curl -sk -u admin:admin -X DELETE $IS/api/server/v1/api-resources/$API_ID
```
