# PEP integration

A policy enforcement point (PEP), such as an API gateway or a backend, asks the PDP before it allows a request.

## Credentials

1. Create an application for the PEP. In the console, a **Machine-to-Machine (M2M) Application**.
2. On its **API Authorization** tab, authorize the **AuthZEN Authorization API** with `internal_authzen_evaluate`.
3. Get a token with its client id and secret, and reuse it until it expires:

   ```bash
   curl -k -u '<client id>:<client secret>' https://localhost:9443/oauth2/token \
     -d 'grant_type=client_credentials&scope=internal_authzen_evaluate'
   ```

Read the endpoint URLs from `/.well-known/authzen-configuration`
(`/.well-known/authzen-configuration/t/{tenant}` for a tenant).

## Naming the subject

Use the user id, which is the `sub` claim of the user's token: `{ "type": "user", "id": "<sub>" }`. For an agent use
`type: agent`, and for an application acting on its own, `type: application` with the application id.

## Choosing the application

- **The request comes through a known application:** pass its id as `resource.properties.app_id`. The decision then
  matches that application's token for the user. To find the id from a client id:

  ```bash
  curl -sk -u admin:admin "https://localhost:9443/api/server/v1/applications?filter=clientId%20eq%20<client id>" \
    | jq -r '.applications[0].id'
  ```

- **The PEP is the application:** omit `app_id`. The evaluation runs for the PEP's own application, so authorize it
  for the API resources it protects.

## Handling responses

- Treat anything but a `200` with `"decision": true` as a deny, including errors and timeouts.
- A deny is a `200` with `"decision": false`.
- Log `context.reason_admin`; don't show it to users.
- Use [`/access/v1/evaluations`](api-reference.md#post-accessv1evaluations) to decide several checks in one call.

## Example (Node.js 18 or later)

```js
async function isPermitted(token, userId, action, resource, appId) {
  try {
    const response = await fetch("https://localhost:9443/access/v1/evaluation", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        subject: { type: "user", id: userId },
        action: { name: action },
        resource: { type: "api_resource", id: resource, properties: { app_id: appId } },
      }),
    });
    return response.ok && (await response.json()).decision === true;
  } catch {
    return false;
  }
}
```
