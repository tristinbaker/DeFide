# Getting De Fide on F-Droid

## 1. Tag a release

F-Droid builds from a specific git tag, not just `main`.

```bash
git tag v1.0.0
git push origin v1.0.0
```

Make sure `versionName` and `versionCode` in `app/build.gradle.kts` match.

## 2. Verify a clean release build

F-Droid builds the APK themselves from source. It must build with no manual steps:

```bash
./gradlew assembleRelease
```

If it fails due to a missing signing config, that's fine — F-Droid signs with their own key.
Just make sure the build itself compiles cleanly.

## 3. Fork fdroiddata on GitLab

- Go to https://gitlab.com/fdroid/fdroiddata
- Fork the repo to your GitLab account
- Clone your fork locally

## 4. Create the app metadata file

Create the file `metadata/app.defide.yml` in your fdroiddata fork:

```yaml
Categories:
  - Religion

License: AGPL-3.0-or-later

SourceCode: https://github.com/tristinbaker/DeFide
IssueTracker: https://github.com/tristinbaker/DeFide/issues

AutoName: De Fide
Summary: Offline Catholic app — Bible, Rosary, Catechism, Prayers, Novenas
Description: |-
  De Fide is a free, open-source Catholic app for Android.

  Features include a Bible reader (Douay-Rheims 1899, Latin Vulgate, Latin
  Vulgate English Translation), the full Catechism of the Catholic Church,
  a guided Rosary with all four mysteries, traditional Catholic prayers, and
  a Novena tracker.

  Fully offline. No tracking, no accounts, no internet permission.
  All Bible translations are public domain.

Builds:
  - versionName: '1.0.0'
    versionCode: 1
    commit: v1.0.0
    gradle:
      - yes
```

Check `app/build.gradle.kts` for the exact `versionCode` integer.

## 5. Test the metadata locally (optional but recommended)

Install the F-Droid server tools and do a test build:

```bash
fdroid build app.defide -v
```

This catches issues before submitting. See https://f-droid.org/docs/Building_a_Signing_Key/
for setup instructions.

## 6. Open a merge request

- Commit the metadata file to your fdroiddata fork
- Open a merge request against `https://gitlab.com/fdroid/fdroiddata`
- Title it: `New app: De Fide`
- A volunteer reviewer will build it and verify it

## 7. Wait for review

Review typically takes days to a few weeks depending on volunteer availability.
They may ask you to fix metadata formatting or build issues — just respond to
comments on the MR.

Once merged, De Fide will appear in the F-Droid index on the next weekly update.

## Notes

- The app ID is `app.defide` — confirm this matches `applicationId` in `app/build.gradle.kts`
- F-Droid will flag any non-free network domains or proprietary libraries found in the build;
  De Fide should be clean since it has no internet permission and no third-party SDKs
- Future releases: bump `versionCode` and `versionName`, tag the commit, then add a new
  entry under `Builds:` in the metadata file via another MR
