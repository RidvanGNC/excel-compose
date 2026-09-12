# Publishing to Maven Central

The build is already wired up (`grid/build.gradle.kts` has the
`com.vanniktech.maven.publish` plugin + POM metadata). What's left are the parts that need
your own accounts and keys — nobody else can do these for you.

## One-time setup

1. **Claim your namespace.** Go to [central.sonatype.com](https://central.sonatype.com),
   sign in with (or link) your GitHub account, and claim the `io.github.ridvangnc`
   namespace. It verifies against your GitHub account automatically since it matches your
   username.

2. **Generate a Sonatype user token.** Once logged in, go to your account's
   "Generate User Token" page. You'll get a username/password pair — this is what
   authenticates publishing, not your regular Sonatype login.

3. **Create a GPG key** (Central requires every artifact to be signed):
   ```bash
   gpg --full-generate-key   # RSA, 4096 bits, no expiry is fine for this
   gpg --list-secret-keys --keyid-format LONG   # copy the key ID (after "sec   rsa4096/")
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --export-secret-keys --armor <KEY_ID> > private-key.asc
   ```

4. **Put the four secrets somewhere Gradle can read them** — either in
   `~/.gradle/gradle.properties` (never in this repo) for publishing from your own machine:
   ```properties
   mavenCentralUsername=<sonatype token username>
   mavenCentralPassword=<sonatype token password>
   signing.keyId=<last 8 chars of the GPG key ID>
   signing.password=<GPG key passphrase>
   signing.secretKeyRingFile=<path to a exported secring.gpg, or use in-memory key below>
   ```
   or as environment variables (`ORG_GRADLE_PROJECT_mavenCentralUsername`, etc.) if you'd
   rather publish via GitHub Actions — see `.github/workflows/publish.yml`, which expects
   these as repo secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`,
   `SIGNING_KEY` (armored private key, the `private-key.asc` contents), `SIGNING_PASSWORD`.

## Publishing a release

Bump `version` in `grid/build.gradle.kts`, commit, tag it, and either:

- push the tag — the `publish` workflow runs `publishAndReleaseToMavenCentral` automatically, or
- run it yourself: `./gradlew :grid:publishAndReleaseToMavenCentral --no-configuration-cache`

It usually takes a few minutes to a few hours to show up as searchable on Central after a
release; `implementation("io.github.ridvangnc:excel-compose:<version>")` works as soon as
it's synced.
