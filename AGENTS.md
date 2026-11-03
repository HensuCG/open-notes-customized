# Open Notes Customized

- This is the HensuCG/open-notes-customized fork. Commit changes on `dev`; fast-forward `main` to the tested release commit when publishing.
- Use the installed Android CLI (`android --help`) and local Gradle wrapper. Use JDK 21 (the source target is Java 17) and the installed Android SDK; do not replace existing toolchains.
- Validate feature changes with `./gradlew :app:testFdroidDebugUnitTest :app:lintFdroidRelease :app:assembleFdroidRelease`. Use the `fdroid` variant for GitHub APK releases (`com.opennotes`).
- Build and sign release APKs locally, then publish using `gh release create --target <commit>`. Increment both `versionCode` and `versionName`. Do not publish unsigned APKs or use GitHub Actions to build release assets.
- Keep the fork's signing key in the ignored `.signing/` directory. Reuse it for every release, keep passwords private, and never commit the key or passwords. Verify APK signatures and record the SHA-256 checksum with each release.
- Preserve existing note data: every Room schema change needs a migration registered in `AppModule`. Include new persistent note fields in both entity mappings and backup serialization, with defaults for older backups.
- Line numbering is per note. `LineNumbering` replaces only leading integer-plus-dot prefixes, numbers blank physical lines, and maps selection/composition offsets. Route every content edit, including images and formatting, through normalization while enabled. Disabling keeps the current text.
- Pure black widgets are independent of the app and system theme. Apply black backgrounds and white text to note, empty-state, and add-note widgets. Refresh both widget types after the preference changes.
- Add user-facing strings to English, French, and Spanish resources. Keep README usage and release instructions current.
- `app/lint-baseline.xml` records only the 65 pre-existing upstream translation gaps. Do not add new feature strings or suppress other checks in that baseline.
