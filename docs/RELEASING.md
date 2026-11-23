# Releasing

Releases go to Maven Central through the [Central Portal](https://central.sonatype.com) and are
**permanent**: a published version can never be changed or removed. Read this before tagging.

## One-time setup

1. Sign in to <https://central.sonatype.com> with the `CeyDrew` GitHub account. The namespace
   `io.github.ceydrew` is then verified automatically.
2. Create a signing key and publish its public part:

   ```bash
   gpg --quick-generate-key "CeyDrew <106634959+CeyDrew@users.noreply.github.com>" ed25519 sign 3y
   gpg --list-secret-keys --keyid-format long            # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
   ```

3. In the Central Portal, *View Account → Generate User Token*.
4. Add repository secrets (*Settings → Secrets and variables → Actions*):

   | Secret | Value |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | token username |
   | `MAVEN_CENTRAL_PASSWORD` | token password |
   | `GPG_PRIVATE_KEY` | output of `gpg --armor --export-secret-keys <KEY_ID>` |
   | `GPG_PASSPHRASE` | the key's passphrase |

   Secrets never go into the repository, `settings.xml` in the repo, or the POM.

## Releasing a version

1. On `main`, with CI green, prepare the release commit:

   ```bash
   ./mvnw versions:set -DnewVersion=0.1.0 -DprocessAllModules -DgenerateBackupPoms=false
   # CHANGELOG.md: rename "## [Unreleased]" to "## [0.1.0] - YYYY-MM-DD"
   git commit -am "chore: release 0.1.0"
   ```

2. Tag and push: `git tag v0.1.0 && git push origin main v0.1.0`.
   `.github/workflows/release.yml` checks that the tag matches the version and the CHANGELOG, runs the
   tests, signs and uploads `tr-validation-bom`, `-core`, `-jakarta` and `-spring-boot-starter`
   (and the parent POM), then creates the GitHub Release from the CHANGELOG section.
3. `central.autoPublish` is `false`: the upload stops at *Validated* in the Central Portal
   (*Deployments*). Check the files there, then press **Publish** — or **Drop** to discard it; a dropped
   deployment can be uploaded again under the same version.
4. Start the next development version:

   ```bash
   ./mvnw versions:set -DnewVersion=0.1.1-SNAPSHOT -DprocessAllModules -DgenerateBackupPoms=false
   # CHANGELOG.md: add an empty "## [Unreleased]" section
   git commit -am "chore: prepare next development version"
   ```

Publishing usually takes 10–30 minutes to reach Maven Central, and longer to show up in search.
