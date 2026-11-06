# Changelog

## 1.5.1 (version code 18)

- Fixed note title and body fields omitting the keyboard's sentence-capitalization request. Compatible keyboards can now capitalize after `. `, `? `, and `! ` while preserving text typed or pasted by the user.
- Added an emulator regression test that checks the actual Android input connection for both fields, including punctuation, trailing spaces, and a comma control case.

## 1.5.0 (version code 17)

- Added **Pure black widgets** in Appearance settings. Both widget types, including the empty note widget, use a true black background and white text when enabled. The setting applies immediately and independently of system theme or note color.
- Added a per-note automatic line-numbering toggle to the editor's bottom toolbar. Existing leading `1.`-style numbers are replaced with a continuous sequence after edits, pastes, and line changes. Selection and keyboard composition remain attached to the edited text. Turning the mode off retains the current numbers.
- Saved the numbering mode with each note and in backups. Existing databases migrate without changing note content; older backups default to numbering off.
- Published this fork with its own release signing certificate. Export notes before switching from an installation signed by a different publisher.
