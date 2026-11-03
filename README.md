# Open Notes [<img src="https://shields.rbtlog.dev/simple/com.opennotes" alt="RB Status" height="80">](https://shields.rbtlog.dev/com.opennotes)

**A minimalist, secure, and fully offline notes app.**

## Customized version

Download the signed APK from [this fork's releases](https://github.com/HensuCG/open-notes-customized/releases/latest) and open it on an Android 8.0+ device to install. Allow installation from your browser or file manager when Android asks.

In **Settings → Appearance → Pure black widgets**, enable black backgrounds and white text for both note and add-note widgets. Existing widgets refresh immediately. This option works independently of the app and system theme and overrides note colors only in widgets.

In the note editor, tap the **numbered-lines button** at the left of the bottom toolbar. It numbers every line from `1.`, replacing previous leading numbers, and keeps numbering up to date while you type, paste, or delete lines. Blank lines count; wrapped display lines do not. The mode is saved for each note and included in backups. Tap again to stop automatic numbering and retain the current text.

### Local builds and releases

Use the installed Android CLI, Android SDK 36, and JDK 17 or 21:

```sh
android --help
./gradlew :app:testFdroidDebugUnitTest :app:lintFdroidRelease :app:assembleFdroidRelease
```

The unsigned APK is written to `app/build/outputs/apk/fdroid/release/`. Align and sign it locally using Android SDK `zipalign` and `apksigner`, with the fork's existing key from the ignored `.signing/` directory. Verify the signature and package version, then publish the APK and a SHA-256 checksum using `gh release create --target <tested-commit>`. Preserve and back up the key and password files; future updates require the same key. CI validates source changes and does not publish release assets. See [AGENTS.md](AGENTS.md) for maintenance rules.

This fork uses its own signing key. It cannot update an upstream Play Store, F-Droid, or other differently signed installation. Export your notes first if switching from one of those builds; Android requires uninstalling it before installing this fork. Backups from older versions remain supported.


## Built using
> [<img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android2.svg">]()
> [<img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/AndroidStudio/androidstudio3.svg">]()
> [<img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg">]()


## ✨ Features

- **Native Markdown rendering** for rich text formatting, including headers, checklists, numbered lists, code blocks, and quotes.
- **Set reminders** for your notes and receive timely notifications.
- **Seamless image support** to embed photos directly alongside your text.
- **Organization tools** that allow you to color-code, pin, and sort your notes.
- **Home screen widgets** for quick access to your most important notes.
- **Built-in privacy options** to lock the app behind biometric authentication and block the Android system from taking screenshots of your data.
- **Extensive personalization** with dark and light themes, plus the ability to choose your favorite color for the home screen icon.
- **Full offline backups** that let you export and import your complete note library as a single ZIP file.
- **Completely free and open-source** with zero invasive permissions.


---

## Get from

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.opennotes.play)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.opennotes)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.opennotes)



---

## Screenshots

| | |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/d34a6de6-57ab-4a04-a8e7-c8b2875a7e7a" width="300" alt="Dashboard" /> | <img src="https://github.com/user-attachments/assets/b7b94e0f-f685-41a4-8637-b9683ca46231" width="300" alt="Note Editor" /> |
| <img src="https://github.com/user-attachments/assets/b6e44e15-062e-4eb2-830d-198aa45f83aa" width="300" alt="Appearance Settings" /> | <img src="https://github.com/user-attachments/assets/8c26bc2e-4ca9-44bd-83ed-f7a99cca1b1c" width="300" alt="Widgets" /> |

---

## Contributing
Contributions are welcome! Your help in making OpenNotes better is appreciated.
Please open an issue before submitting a pull request that adds a new feature, so it can be discussed first.
All changes should be committed to the dev branch, not main.


---

## Translations

Help translate Open Notes into your language on [Weblate](https://hosted.weblate.org/projects/open-notes/).

[![Translation status](https://hosted.weblate.org/widget/open-notes/287x66-white.png)](https://hosted.weblate.org/engage/open-notes/)

---

## Inspiration

- [EasyNotes](https://github.com/Kin69/EasyNotes?tab=readme-ov-file)
- [Tomato](https://github.com/nsh07/Tomato)
---

# Security

SHA-256 fingerprint for this fork's GitHub release signing certificate:
```text
5C:25:E9:8A:5F:DF:13:F7:12:E1:DF:FC:F7:C5:02:A0:A9:38:64:30:B1:CC:DF:80:23:57:3C:CB:C8:0E:C4:82
```

SHA-1:
```text
CB:50:F4:46:49:BC:7F:58:E6:3D:84:2D:BE:FA:8D:72:DD:5C:A9:50
```

