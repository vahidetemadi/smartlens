# Changelog

All notable changes to this project will be documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), newest version first.
The `[Unreleased]` section is shown as the plugin's change notes and is renamed to the version on release
(`./gradlew patchChangelog`).

## [Unreleased]

### Added
- Settings: "Your other Git e-mails" to recognize your own commits made with other identities (e.g. GitHub no-reply addresses).
- End-to-end test that runs the plugin in a headless IDE against a real Git repository.
- Opt-in studies (attribution scenarios, mining of open-source repositories, tool-window renderer), excluded from the default test run.

### Changed
- Tables keep the ranking computed by the analysis (highest priority/risk first) until a column header is clicked.
- File columns show the file name first, then its directory; numeric columns use a fixed, compact width.
- Untracked files are read directly from Git, so files an agent has just created show up immediately.
- Learn no longer reports imports of the project's own packages as new skills.
- Risk ignores dotfiles (e.g. `.gitignore`) and Markdown documents.
- Risk summary shows the primary developer e-mail and the number of aliases instead of every address.

### Fixed
- Understand and Risk listed the lowest priority/risk first.
- New files created by an agent were missing from Understand until the IDE's change list refreshed.
- The agent hook log (`.smartlens/agent-edits.jsonl`) appeared as an agent change when it was not git-ignored.

## [0.1.0] - 2026-10-15

### Added
- Attribution of pending changes to an AI agent, the developer, or both, from external file changes, editor edits,
  an optional agent hook log, and `Co-authored-by` trailers in the Git history.
- SmartLens tool window (Tools | SmartLens) with three tabs: Learn, Understand and Risk.
- Settings page (Settings | Tools | SmartLens): agent identities, hook log path, code file extensions, number of
  commits to analyze, and the Git quiet period.
- Refresh and Reset Attribution actions.
