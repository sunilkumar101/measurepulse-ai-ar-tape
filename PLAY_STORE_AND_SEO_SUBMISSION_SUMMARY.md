# MeasurePulse AI — Play Store & SEO Submission Summary

_Last updated 1 Nov 2026 · app version 1.0.0 (versionCode 1)_

## Status at a glance

| Item | Status |
|---|---|
| GitHub repo | ✅ Public, pushed: https://github.com/sunilkumar101/measurepulse-ai-ar-tape |
| Landing page | ✅ Live (HTTP 200): https://sunilkumar101.github.io/measurepulse-ai-ar-tape/ |
| Privacy policy | ✅ Live (HTTP 200): https://sunilkumar101.github.io/measurepulse-ai-ar-tape/privacy-policy.html |
| Terms of use | ✅ Live: https://sunilkumar101.github.io/measurepulse-ai-ar-tape/terms.html |
| sitemap.xml / robots.txt | ✅ Live: `/sitemap.xml`, `/robots.txt` |
| app-ads.txt | ⚠️ Template live at `/measurepulse-ai-ar-tape/app-ads.txt`, but it needs your AdMob publisher ID **and** a copy at the domain root (see below) |
| Search Console | ⏳ **Not verified yet**: it needs you to sign in to Google (see the steps below) |
| Signed release AAB | ✅ `app/build/outputs/bundle/release/app-release.aab` (34.2 MB) |
| Play Console upload | ⏳ **Not uploaded**: the app must first be created in Play Console and the first bundle uploaded by hand |

## Release build

- `./gradlew clean testDebugUnitTest bundleRelease assembleRelease`: BUILD SUCCESSFUL, **63/63 unit tests pass**.
- AAB: `app/build/outputs/bundle/release/app-release.aab`. APK (for sideload testing): `app/build/outputs/apk/release/app-release.apk`.
- `targetSdkVersion 36`, `compileSdk 36` (Android 16), `minSdk 26`.
- 16 KB page size: all 14 arm64-v8a / x86_64 `.so` files in the AAB have LOAD segments aligned ≥ 16 KB, and the APK passes `zipalign -c -P 16`.
- Signed with the **upload key** `keystore/measurepulse-upload.jks` (alias `measurepulse-upload`, RSA 4096, 30 years).
  - Certificate SHA-256: `23:86:08:27:EA:04:96:C2:4D:D5:BD:6E:15:88:A8:9F:A6:75:2E:04:D4:1B:B0:A9:F0:45:BE:02:A3:7A:99:1D`.
  - Passwords are in `keystore/KEYSTORE_CREDENTIALS.txt`. Both files are git-ignored. **Back them up now** (password manager plus an offline copy).
- ⚠️ **AdMob IDs:** this release build still uses Google's **test** ad IDs, because no real IDs are set. That is fine for internal testing. Before production, put the real IDs in `local.properties`:
  - `measurepulse.admob.appId=ca-app-pub-…~…`
  - `measurepulse.admob.banner=…`
  - `measurepulse.admob.interstitial=…`
  - `measurepulse.admob.rewarded=…`
  - `measurepulse.admob.native=…`

  Then rebuild and bump `versionCode`.

## Google Search Console — what's left (≈5 minutes)

The Search Console tools available here can't add or verify a new property, and Google sign-in can't be automated. So:

1. Open https://search.google.com/search-console and sign in.
2. Click **Add property**, choose **URL prefix**, and enter `https://sunilkumar101.github.io/measurepulse-ai-ar-tape/`.
3. Choose the **HTML tag** method and copy the `<meta name="google-site-verification" …>` tag.
4. Paste it into `docs/index.html` where the comment `Google Search Console: paste the HTML-tag verification meta here` sits. Then commit and push. Pages redeploys in about a minute.
5. Click **Verify**.
6. Go to **Sitemaps**, submit `sitemap.xml`, and confirm that the status turns to **Success** with 3 URLs discovered.
7. Go to **URL Inspection** and click **Request indexing** for the home page and `privacy-policy.html`.

Verification log: Pages build `built` · index 200 · privacy-policy 200 · sitemap valid XML (3 URLs, image + video extensions) · JSON-LD parses (SoftwareApplication, VideoObject, WebSite) · canonical, OpenGraph and Twitter tags present. Search Console property: **pending owner sign-in**.

## app-ads.txt (AdMob)

AdMob only reads `app-ads.txt` at the **root of the developer-website domain**. For a site at `sunilkumar101.github.io/...` that is `https://sunilkumar101.github.io/app-ads.txt`, which is served from your existing repo `sunilkumar101/sunilkumar101.github.io`.

1. In AdMob, open **Settings → Account information** and copy your publisher ID (`pub-…`).
2. Replace `pub-XXXXXXXXXXXXXXXX` in `docs/app-ads.txt`.
3. Add the same file to the root of the `sunilkumar101.github.io` repo.
4. In Play Console, set the store listing **Website** to `https://sunilkumar101.github.io/measurepulse-ai-ar-tape/`.
5. In AdMob, open **Apps → app-ads.txt → Check for updates**.

## Google Play Console — publishing steps

The upload couldn't be automated:
- Fastlane and Ruby aren't installed here.
- There is no Play service-account key.
- Google only accepts API uploads for an app that already exists in Play Console and has had a first bundle uploaded by hand.

Everything else is ready.

1. **Create the app.** In Play Console, click **Create app**:
   - name `AR Tape Measure: 3D Room Plan`
   - default language English (US)
   - App, Free
   - accept the declarations
2. **Set up the internal test.** Go to **Testing → Internal testing → Create release**. Accept Play App Signing, upload `app-release.aab`, and paste the release notes from `fastlane/metadata/android/en-US/changelogs/1.txt`.
3. **Store listing.** Copy these in, or let fastlane do it (step 7):
   - `fastlane/metadata/android/en-US/title.txt`, `short_description.txt` and `full_description.txt`
   - the 8 images in `images/phoneScreenshots/`
   - `images/icon.png` (512×512) and `images/featureGraphic.png` (1024×500)
   - Optional promo video: upload `playstore_assets/measurepulse_promo_16x9.mp4` to YouTube and paste the link.
4. **App content:**
   - Privacy policy: `https://sunilkumar101.github.io/measurepulse-ai-ar-tape/privacy-policy.html`
   - Ads: **Yes, my app contains ads**
   - App access: **All functionality is available without special access**
   - Advertising ID: **Yes**, used for **Advertising** and **Analytics** (the AdMob SDK adds `AD_ID`)
   - Content rating: fill in the IARC questionnaire as a **Utility**. There's no violence, user content, gambling or location sharing, so expect *Everyone / PEGI 3*.
   - Target audience: **13+**. Picking 13–17 and 18+ only keeps you out of the Families ad requirements.
   - Data safety: see below.
   - Government, financial, health and news declarations: **No**.
5. **In-app product.** Once a build with the billing permission is uploaded, go to **Monetize → Products → In-app products** and create `measurepulse_pro_lifetime`:
   - Name "MeasurePulse AI Pro (Lifetime)"
   - Price **USD 2.99**
   - Activate it

   The app shows "not on sale yet" until this exists. You need a payments profile.
6. **Production access.** Personal developer accounts created after Nov 2023 must run a **closed test with at least 12 testers for 14 days** before Play allows production. Plan for this.
7. **Fastlane, for later uploads.**
   1. Install Ruby and run `bundle install`.
   2. Create a Google Cloud service account, download its JSON key to `fastlane/play-service-account.json` (git-ignored), and invite that account in **Play Console → Users and permissions** with release rights.
   3. Run `bundle exec fastlane internal` to upload the AAB and the full listing as a draft internal release.
   4. Run `bundle exec fastlane metadata` to push listing changes only.

## Data safety form — answers

These are based on the app's code and on Google's SDK disclosures ([AdMob](https://developers.google.com/admob/android/privacy/play-data-disclosure), [ML Kit](https://developers.google.com/ml-kit/android-data-disclosure)).

**Does your app collect or share any of the required user data types?** Yes.
**Is all user data encrypted in transit?** Yes (TLS, Google SDKs).
**Do you provide a way for users to request that their data be deleted?** The app has no server-side data. User data is on-device and deleted by uninstalling or by Projects → delete. AdMob data is managed through Google's My Ad Center. If the form insists on a mechanism, give the GitHub Issues link from the privacy policy.

| Data type | Collected | Shared | Optional? | Purposes | Source |
|---|---|---|---|---|---|
| Location → **Approximate location** (IP address) | Yes | Yes (Google) | Required (free tier) | Advertising, Analytics, Fraud prevention | AdMob |
| Device or other IDs (Advertising ID, app set ID) | Yes | Yes (Google) | Required (free tier) | Advertising, Analytics, Fraud prevention | AdMob, ML Kit |
| App activity → **App interactions** (launches, taps, ad views) | Yes | Yes (Google) | Required | Advertising, Analytics | AdMob |
| App info & performance → **Diagnostics** | Yes | Yes (Google) | Required | Analytics | AdMob, ML Kit |
| Financial info → **Purchase history** | Yes (conservative) | No | Optional (only if you buy Pro) | App functionality | Google Play Billing |
| Photos & videos, Audio, Precise location, Personal info, Messages, Contacts, Files, Calendar, Health, Web history | **No** | No | — | — | Camera, depth and voice are processed in memory only |

Notes:
- Camera frames, ARCore depth and the "Mark" voice keyword never leave the device. The voice trigger uses **only** Android's on-device recognizer; otherwise it falls back to a local sound-level trigger. This was changed in this release so the privacy policy is literally true.
- Android Auto Backup may include the rooms database in the **user's own** Google backup. Google says this isn't "collection" by the developer.
- Pro users never load the ads SDK. Declare the AdMob rows anyway, because the free tier is the default.

## Store listing text

| Field | Value | Length |
|---|---|---|
| Title | AR Tape Measure: 3D Room Plan | 29 / 30 |
| Short description | 3D AR laser tape measure, room floor plan blueprint, caliper & spirit level AI. | 79 / 80 |
| Full description | `fastlane/metadata/android/en-US/full_description.txt` | 3,739 / 4,000 |

The requested title "AR Tape Measure: 3D Room & Plan" is 31 characters (Play's limit is 30), so it was shortened to the line above.

The full description covers these keywords:
- ar ruler
- tape measure app
- smart measure distance
- room floor plan creator
- area and volume calculator
- height measure camera
- digital vernier caliper mm/inch
- bubble level
- clinometer
- furniture fit check
- ac tonnage calculator

It names no competitors (Play metadata policy). The website uses "Smart Measure alternative" with a non-affiliation note.

## What was published to GitHub

Published:
- app source and tests
- `docs/`
- `fastlane/`
- `playstore_assets/`
- `promo-video/` (without `node_modules` or `out`)
- `design_screenshots/`
- tools, README, CLAUDE.md

**Not** published:
- keystore, `keystore.properties`, `local.properties`
- service-account keys
- `reference_repos/`
- build outputs, the `.cache/` folder
- installed third-party agent skills (`.agents/`, `.claude/skills/`; reinstall from `skills-lock.json`)

The status bars of the raw phone captures were blanked first, so your notification icons aren't public. The AdMob test-device hash was removed from CLAUDE.md.
