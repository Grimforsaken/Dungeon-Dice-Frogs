# Development signing key

`stable-debug.keystore.b64` is a development-only Android signing key used so GitHub Actions test APKs keep the same signer between builds.

It is intentionally not a production/Google Play signing key and must never be used for a store release. Before any production release, create a private release key and store it only in protected CI secrets or the appropriate app-store signing system.
