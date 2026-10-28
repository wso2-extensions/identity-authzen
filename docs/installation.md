# Installation

Requires WSO2 Identity Server 7.3 on JDK 21.

## Install

1. Get `org.wso2.carbon.identity.authzen-<version>.jar` from the release, or build it with `mvn clean install`
   (it is written to `components/org.wso2.carbon.identity.authzen/target/`).
2. Copy it to `<IS_HOME>/repository/components/dropins/`.
3. Merge the [configuration](configuration.md) into `<IS_HOME>/repository/conf/deployment.toml`.
4. Restart the identity server.

To upgrade, replace the jar with the new version and restart.

## Verify

```bash
curl -k https://localhost:9443/.well-known/authzen-configuration
```

```json
{
  "policy_decision_point": "https://localhost:9443",
  "access_evaluation_endpoint": "https://localhost:9443/access/v1/evaluation",
  "access_evaluations_endpoint": "https://localhost:9443/access/v1/evaluations"
}
```

Then follow [Getting started](getting-started.md) for a first decision.

## Uninstall

1. Remove the jar from `dropins/`.
2. Remove the connector's blocks from `deployment.toml`, and `"/access/v1/(.*)"` from
   `[tenant_context.rewrite] servlets`.
3. Restart the identity server.
