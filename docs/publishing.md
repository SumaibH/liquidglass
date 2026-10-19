# Publishing

Maintainer notes. Consumers only need the [install instructions](getting-started.md#install).

One tag publishes one version everywhere:

| Registry | Packages | Secrets |
| :-- | :-- | :-- |
| Maven Central | `liquidglass`, `liquidglass-material3`, `liquidglass-bom`, and the Gradle plugin `dev.shayxo.liquidglass` (artifact `liquidglass-gradle-plugin` plus its plugin marker) | `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_KEY_PASSWORD` (set up since 0.1.0) |
| npm | `liquidglass`, the `<liquid-glass>` web component; `liquidglass-react-native` | `NPM_TOKEN` |
| NuGet | `LiquidGlass`, `LiquidGlass.WinForms`, `LiquidGlass.Wpf`, `LiquidGlass.Avalonia` | `NUGET_API_KEY` |
| GitHub | a release with the notes in `.github/release-notes/vX.Y.Z.md` | none (the workflow's token) |

The Gradle plugin is on Maven Central rather than the Gradle Plugin Portal, under the same signing key.
`plugins { id("dev.shayxo.liquidglass") }` finds it through its plugin marker in any build that lists
`mavenCentral()` in `pluginManagement`, which the Android Studio and Kotlin Multiplatform templates
already do. Listing it on the Plugin Portal as well is optional and not set up.

## The version

`VERSION_NAME` in the root `gradle.properties` is the one number. The Gradle builds read it, the
NuGet packages read it (`ports/dotnet/Package.props`), and the npm job stamps it into `package.json`
at publish time. The repository keeps both npm packages (`ports/npm`, `ports/react-native`) at
`"private": true` and `0.0.0`, so a stray `npm publish` from a checkout cannot go out. Change it with the script, which also rewrites the
.NET lock files that record it:

```bash
python .github/scripts/set_version.py 0.2.0
```

## Locally

```bash
./gradlew publishToMavenLocal                      # the library, Material 3 and the BOM
./gradlew -p gradle-plugin publishToMavenLocal     # the Gradle plugin and its marker
```

puts every artifact in `~/.m2/repository/dev/shayxo/liquidglass/`. A consuming project adds
`mavenLocal()` to its repositories (and to `pluginManagement` for the plugin), restricted to this
group so that a stale `~/.m2` cannot shadow anything else:

```kotlin
// settings.gradle.kts, in pluginManagement.repositories and dependencyResolutionManagement.repositories
mavenLocal { content { includeGroupAndSubgroups("dev.shayxo") } }
```

No signing key is needed for this. Signing switches on only when a key is present.

## Each release

1. `python .github/scripts/set_version.py X.Y.Z`.
2. Write `.github/release-notes/vX.Y.Z.md` (the GitHub release body) and give `CHANGELOG.md` the
   dated `## X.Y.Z` heading.
3. Update the install snippets that name a version or say "unreleased":
   `grep -rn "unreleased\|0\.1\.0" README.md docs/*.md`.
4. Open a pull request. Its `Publish` checks run the whole release as a dry run: every package is
   built, packed and checked, nothing is uploaded.
5. Merge, wait for CI on `main`, then tag the merge commit:
   ```bash
   git tag vX.Y.Z <merge commit> && git push origin vX.Y.Z
   ```
6. `.github/workflows/publish.yml` refuses unless the tag matches `VERSION_NAME`, the version is not a
   SNAPSHOT, the commit is on `main` and passed CI there, and the release notes exist. Then each
   registry is its own job, and the GitHub release follows them. Maven Central shows the artifacts
   within about half an hour.
7. If one job fails (a missing secret, an outage), fix the cause and run
   `gh run rerun <id> --failed`. The others stay published; npm and NuGet skip a version that is
   already there.
8. `python .github/scripts/set_version.py X.Y+1.0-SNAPSHOT` on `main`.

A version with a pre-release suffix (`0.3.0-beta.1`) goes to npm under the `next` dist-tag and is
marked as a pre-release on GitHub; NuGet and Maven treat it as a pre-release by its suffix.

## npm, once

1. An account at [npmjs.com](https://www.npmjs.com) with two-factor authentication.
2. The names `liquidglass` and `liquidglass-react-native` were free on 19 October 2026; the first
   publish claims them.
3. A granular access token (Account, Access Tokens, Generate New Token, Granular): read and write
   on packages, with two-factor bypass allowed so CI can publish. Before the first publish the token
   cannot be limited to a package that does not exist yet, so give it all packages and narrow it to
   `liquidglass` afterwards.
4. `gh secret set NPM_TOKEN --repo shayann07/liquidglass` and paste it.

Packages are published with [provenance](https://docs.npmjs.com/generating-provenance-statements):
npm shows that each version was built by this workflow from this repository.

## NuGet, once

1. An account at [nuget.org](https://www.nuget.org) (a Microsoft account).
2. The IDs `LiquidGlass`, `LiquidGlass.WinForms`, `LiquidGlass.Wpf` and `LiquidGlass.Avalonia` were
   free on 19 October 2026; the first push claims them. Optionally reserve the `LiquidGlass` prefix
   afterwards (nuget.org ID prefix reservation), which marks the packages as verified.
3. An API key (Account, API Keys, Create): scope "Push new packages and package versions", glob
   pattern `LiquidGlass*`.
4. `gh secret set NUGET_API_KEY --repo shayann07/liquidglass` and paste it.

## Maven Central

Publishing goes through Sonatype's Central Portal, via the
[vanniktech maven-publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin), for the
library build and for the separate `gradle-plugin` build alike.

### Once (done for 0.1.0)

1. **An account** at [central.sonatype.com](https://central.sonatype.com).
2. **A verified namespace.** The group is `dev.shayxo.liquidglass`, under the namespace
   `dev.shayxo`, which is verified once by proving ownership of `shayxo.dev`: in the portal,
   Namespaces, Add Namespace, enter `dev.shayxo`; it hands you a verification key to publish as
   a DNS TXT record on the apex domain, then a Verify button. Every group under a verified
   namespace is covered, so future artifacts need nothing more. The Kotlin package remains
   `com.wexpa.liquidglass`; Central checks the group, not the package. A group cannot be
   renamed after the first release without orphaning consumers, which is why this was settled
   first.
3. **A user token** from the portal (Account, then Generate User Token). This is the username
   and password the plugin uses, not the account login.
4. **A GPG key.** `gpg --full-generate-key`, then publish it:
   `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`. Export the secret key in the
   form the plugin reads: `gpg --export-secret-keys --armor <KEY_ID>`.

### Credentials

Never in the repository. Locally, in `~/.gradle/gradle.properties`:

```
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
signingInMemoryKey=<the armored secret key, newlines as \n>
signingInMemoryKeyPassword=<key passphrase>
```

In GitHub Actions, the same four as repository secrets named `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`; the publish workflow maps
them onto `ORG_GRADLE_PROJECT_*` variables, which Gradle reads as project properties.

To publish from a machine instead of CI:

```bash
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
./gradlew -p gradle-plugin publishAndReleaseToMavenCentral --no-configuration-cache
```

`publishToMavenCentral` (without `AndRelease`) uploads and stops, leaving the deployment for a
manual release in the portal, which is the safer choice for a first attempt.

## Notes from the first release

Each of these cost time once, on 2026-09-10, and should not cost it again.

- **The signing passphrase must be plain ASCII.** Gradle's signing plugin hands the passphrase
  to BouncyCastle as Latin-1 while GnuPG derives the key from UTF-8, so a passphrase with any
  character outside ASCII fails at `signAndroidPublication` with
  `PGPException: checksum mismatch in checksum of 20 bytes`, even though gpg itself accepts it.
  Letters and digits only. Before pushing a tag, run the exact CI code path locally:
  ```powershell
  $env:ORG_GRADLE_PROJECT_signingInMemoryKey = (gpg --export-secret-keys --armor KEY_ID | Out-String)
  $env:ORG_GRADLE_PROJECT_signingInMemoryKeyPassword = [System.Net.NetworkCredential]::new('', (Read-Host 'passphrase' -AsSecureString)).Password
  .\gradlew.bat :liquidglass:signAndroidPublication --no-configuration-cache
  ```
  and once it passes, feed the secrets from those same variables so nothing is retyped:
  `$env:ORG_GRADLE_PROJECT_signingInMemoryKey | gh secret set SIGNING_KEY --repo shayann07/liquidglass`.
- **Never export a key with PowerShell's `>`.** It writes UTF-16, which gpg cannot read back.
  Use `gpg --export-secret-keys --armor --output key.asc KEY_ID`.
- **On Windows, gpg ships inside Git**: `Set-Alias gpg 'C:\Program Files\Git\usr\bin\gpg.exe'`,
  and `gpg-connect-agent` is in the same folder. GnuPG 2.4's `keyboxd` cannot start when gpg is
  launched from PowerShell, so a fresh `~/.gnupg/common.conf` needs its `use-keyboxd` line
  commented out before the first key can be generated.
- **Namespace verification is a button, not a poll.** Once the TXT record exists, use the
  namespace row's menu, Verify Namespace. Check the record on *both* authoritative servers:
  Dynadot's `ns1` served it within minutes while `ns2` kept the old zone for over an hour on
  some of its anycast nodes, without bumping the zone serial, so any resolver that happened to
  ask `ns2` saw nothing.
- **A failed publish run can be rerun on the same tag** with `gh run rerun <id> --failed`.
  Nothing reaches Central until signing passes, so a signing failure leaves no partial
  deployment behind.
