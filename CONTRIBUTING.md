# Contributing

Build with JDK 21:

```bash
mvn clean install
```

This runs the unit tests and writes the connector jar to `components/org.wso2.carbon.identity.authzen/target/`.

- Add unit tests with your change, and check behaviour changes on a running identity server; the
  [getting started](docs/getting-started.md) steps are a quick end-to-end check.
- Follow the existing code style and keep the license header in every source file. Never log usernames.
- Update `docs/` and `CHANGELOG.md` for changes users can see.
- Open pull requests against `main` using the [template](pull_request_template.md).
- On release, add the version and its identity server versions to `.connector-store/meta.json`.
