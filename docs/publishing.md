# Publishing releases

The `build` workflow compiles the mod on every push and pull request using Java 25
and the Gradle Wrapper, then saves the build artifacts. Gradle dependencies are
cached between runs.

The `release` workflow runs when you **publish a GitHub Release**. It checks out
that release's tag, validates its version and notes, builds the mod, and passes
the installable JAR to a separate publishing job. That job uses
[mc-publish v3.3.1](https://github.com/Kira-NT/mc-publish/tree/v3.3.1) to upload the
same JAR and release notes to Modrinth, CurseForge, and the existing GitHub Release.
Only the publishing job has permission to write to GitHub Releases.

## One-time repository setup

In GitHub, open **Settings → Secrets and variables → Actions**.

Under **Variables**, add these repository variables:

| Name | Value |
| --- | --- |
| `MODRINTH_PROJECT_ID` | Your Modrinth project's ID |
| `CURSEFORGE_PROJECT_ID` | Your CurseForge project's numeric ID |

Under **Secrets**, add these repository secrets:

| Name | Value |
| --- | --- |
| `MODRINTH_TOKEN` | Your Modrinth personal access token with permission to create versions and modify versions on this project |
| `CURSEFORGE_TOKEN` | Your CurseForge author API token with permission to upload files |

You can manage tokens in [Modrinth settings](https://modrinth.com/settings/pats)
and the [CurseForge author API tokens page](https://authors.curseforge.com/#/api-token).
The Modrinth action also updates which versions are featured, so version editing
permission is needed in addition to version creation permission.

GitHub automatically supplies `GITHUB_TOKEN`; do not create a separate personal
access token for it. Do not put API tokens in project files or release notes.
The publishing job fails before uploading anything if any required setting is empty.

## Creating a release

1. Set `mod_version` in `gradle.properties`, for example `1.0.0`.
2. Confirm `minecraft_version` is the exact version supported by this release.
3. Commit and push your changes, including the release workflow. Wait for the
   build workflow to succeed and try the packaged mod in Minecraft.
4. In GitHub's **Releases** page, create a draft release targeting that commit.
   Choose a tag matching `v` plus `mod_version`: `v1.0.0` for `1.0.0`.
5. Write the release title and notes. Notes are required and become the changelog
   on both mod hosting platforms.
6. Publish the release and monitor **Actions → release**.
7. Check the uploaded file and compatibility information on all three platforms.
   Hosting platform moderation may still be required after upload.

The workflow must exist at the tagged commit. Pushing a tag or saving a draft
does not publish files. Publishing the GitHub Release starts the workflow; its
downloadable JAR is attached after the build succeeds.

For prereleases, use a version such as `1.1.0-alpha.1` or `1.1.0-beta.1`, a matching
tag, and select **Set as a pre-release** in GitHub. Alpha suffixes map to `alpha`;
other GitHub prereleases map to `beta`. Ordinary releases map to `release`.
Versions with a suffix are rejected unless GitHub marks them as prereleases.

Only `fishing-bow-<version>.jar` is published. Sources and development JARs remain
available as CI build artifacts. If the Gradle archive name changes, update the
release workflow's `jar` output too.

Compatibility comes from `minecraft_version`; the workflow explicitly declares
Fabric, Java 25, client and server support, and Fabric API as a required dependency
using its platform IDs. If the mod's loader, Java requirement, or dependencies
change, update the workflow accordingly.

## Failed uploads

A build failure prevents the publishing job from running. After a configuration
failure, fix the repository variables or secrets and rerun the failed job.

Uploads to different platforms are independent; an upload failure does not roll
back files already published elsewhere. Before rerunning a failed publishing job,
check all three destinations for files from this release. Remove partial uploads
if you want to rerun the entire publishing job, or upload the saved `release-jar`
artifact manually to the missing destination. An unchanged rerun may otherwise
encounter duplicate-version errors.

If a workflow or code fix is required, create a new version and matching tag that
contains the fix. Rerunning the old release still uses its original tagged files.

## Before the first public release

Replace the template description, author, homepage, and source links in
`src/main/resources/fabric.mod.json` with this project's real details. Confirm
the icon and license, and verify installation on a clean client and a server.
