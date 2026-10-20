# Submitting Always On Clock to F-Droid

Everything F-Droid needs is prepared in this repo:

| Piece | Where |
|---|---|
| Store name / summary / description / changelog | `fastlane/metadata/android/en-US/` (F-Droid reads these from the app repo) |
| fdroiddata metadata file | `fdroid/com.hana.aodclock.yml` (this folder) |
| Unsigned build for F-Droid signing | `./build.sh` with `UNSIGNED=1` |
| License | MIT (`LICENSE`) — FOSS, no ads/trackers, no proprietary deps |

## Option A — Merge request (fastest)

You need a free account on **gitlab.com** (F-Droid's repos live there).

1. Fork **fdroid/fdroiddata**: https://gitlab.com/fdroid/fdroiddata/-/forks/new
2. In your fork, create a branch, e.g. `add-always-on-clock`.
3. Add the file `metadata/com.hana.aodclock.yml` — copy the contents of
   `fdroid/com.hana.aodclock.yml` from this repo, unchanged.
4. Commit with message: `New app: Always On Clock`
5. Open a **Merge Request** from your branch to `fdroid/fdroiddata:master`.
   - Title: `New app: Always On Clock`
   - Description: link this repo, note the Gradle-free build and that
     `UNSIGNED=1 ./build.sh` was tested and produces an unsigned APK with
     versionName 3.0 / versionCode 3.
6. F-Droid CI will try to build it; maintainers review. If they ask for
   changes, edit the file in your fork branch — the MR updates automatically.

Tip: before submitting, you can self-check with the F-Droid server tools
(`pip install fdroidserver`, then `fdroid lint com.hana.aodclock` inside a
checkout of your fork). Optional — CI runs the same checks.

## Option B — Request for Packaging (no metadata work)

File an issue in F-Droid's Submission Queue and let the team create the
metadata: https://gitlab.com/fdroid/rfp/-/issues — include the app name,
this repo URL, the license (MIT) and the current version (3.0). Slower than
Option A, because volunteers do the metadata work for you. The texts they
need are all in `fastlane/metadata/android/en-US/` in this repo.

## After acceptance

- The listing appears at `https://f-droid.org/packages/com.hana.aodclock/`
  within a few days of the first successful build.
- Future releases: just tag a new version on GitHub (e.g. `v3.1`) with an
  updated changelog in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
  `UpdateCheckMode: Tags` + `AutoUpdateMode: Version` in the metadata mean
  F-Droid detects and builds new tags automatically.
- Screenshots: drop PNGs into `fastlane/metadata/android/en-US/images/phoneScreenshots/`
  and F-Droid will pick them up for the listing.

## Known review points (heads-up)

- **Build recipe is unusual** (no Gradle): covered by MaintainerNotes in the
  YAML; the recipe needs JDK 17 + build-tools 34.0.0, both standard on the
  F-Droid buildserver.
- **Weather services**: defaults to the free Open-Meteo API. There is an
  *optional* OpenWeatherMap source that needs the user's own API key; the
  app is fully functional without it, so no NonFreeNet anti-feature should
  apply — but be ready to explain this if a reviewer asks.
