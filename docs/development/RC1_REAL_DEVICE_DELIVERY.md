# Recetoria RC1 — Real Device Delivery

## Objective

Produce the first persistent Recetoria installation for Raquel using the permanent release signing identity.

The debug APK is not the delivery artifact.

## Release identity

The private keystore must never be committed to Git.

Local release signing can be supplied through a root-level `keystore.properties` file:

```properties
storeFile=<absolute-or-root-relative-path-to-keystore>
storePassword=<secret>
keyAlias=recetoria
keyPassword=<secret>
```

Alternatively, the build accepts:

- `RECETORIA_KEYSTORE_FILE`
- `RECETORIA_KEYSTORE_PASSWORD`
- `RECETORIA_KEY_ALIAS`
- `RECETORIA_KEY_PASSWORD`

The keystore and its credentials must be backed up securely outside the repository. Losing the signing identity can prevent future APKs from updating the installed application in place.

## Permanent keystore creation

Run locally with a JDK installed:

```bash
keytool -genkeypair \
  -v \
  -keystore recetoria-release.jks \
  -alias recetoria \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Do not paste the passwords into issues, pull requests, commits, chat logs, or repository files.

## Candidate build

With the signing configuration available locally:

```bash
./gradlew clean assembleRelease
```

Expected output:

```text
app/build/outputs/apk/release/app-release.apk
```

Before delivery, verify that the APK is signed and that its package is `com.rmm.recetasraquel`.

## RC1 acceptance on Raquel's Android phone

1. Install the signed release APK.
2. Open Recetoria and verify icon, splash, home and navigation.
3. Create a recipe.
4. Add a library ingredient and a custom ingredient.
5. Add quantities, units and notes.
6. Add a cover photo and a step photo.
7. Save, close and reopen the recipe.
8. Verify food-safety information where applicable.
9. Verify search and favourites.
10. Enter cooking mode and navigate through the recipe.
11. Close and reopen the app and verify that created data persists.
12. Check the main flows in both light and dark system themes.

Any crash, data-loss defect, inability to save/reopen a recipe, broken photo persistence, or failed release installation blocks RC1.

## Versioning rule

Once Raquel receives a persistent signed build, every later build intended to update it must:

- use the same application ID;
- use the same signing identity;
- use a higher `versionCode`.

Do not distribute a differently signed build as the persistent installation.

## RC1 closure

RC1 is closed only when:

- CI is green for the exact candidate commit;
- a permanent release signing identity exists and is backed up;
- a signed release APK is produced;
- the APK installs on Raquel's real Android phone;
- the acceptance walkthrough passes;
- the accepted commit is frozen/tagged for traceability.

Backup/Restore remains a separate product-v1 closure gate unless explicitly promoted into RC1.
