# Runtime dependency

- Gson 2.13.2 (`com.google.code.gson:gson:2.13.2`), Apache License 2.0.
- Purpose: strict JSON parsing/serialization of API requests, responses and plans.
- [Upstream release](https://github.com/google/gson/releases/tag/gson-parent-2.13.2)
- [Upstream license](https://github.com/google/gson/blob/gson-parent-2.13.2/LICENSE)
- Download: `https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar`
- SHA-256: `dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0`

`scripts/deps.sh` downloads and verifies the artifact. It is cached under ignored
`build/deps/`; no third-party binary is committed. If checksum verification fails,
remove the cached JAR and retry the script. Java's standard HttpClient handles API
transport; Swing handles the desktop GUI. No OpenAI SDK dependency is required.
