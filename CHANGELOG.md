# StreamWarden's Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Refactor
- Default output directory changed from OS-specific Downloads folder to local "downloads" directory for better portability and consistency.

---

## [0.1.0] - 2025-09-15

### Added
- Initial graphical user interface for stream monitoring and recording.
- Support for monitoring Twitch and YouTube channels.
- Automatic detection and recording when a stream goes live.
- Channel management: add, edit (via double-click or toolbar button), and remove channels.
- Organized recordings: each channel has its own folder for saved streams.
- Customizable recording quality per channel.
- Activity logs with toggleable visibility (setting and checkbox).
- Confirmation dialog before removing a channel.
- Graceful shutdown: all recording processes are terminated when closing the app.
- Modal dialog displayed during shutdown to inform the user.
- Default output directory set to the user's Downloads folder (cross-platform).
- Streamlink executable path auto-detection based on OS.
- Kick platform support in channel list, with version check and warning if unsupported.
- Persistent settings stored in JSON configuration file.
- Platform icons display in channel dialog and main table view for better visual identification.
- Context menu on channel rows with "Edit", "Remove" and "Open in Browser" actions for quick access.
- Settings dialog for configuring output directory, monitoring behavior, and application preferences.
- Quality fallback system to ensure recordings start even when exact quality is unavailable.
- High FPS recording preference setting to control framerate priority (30+fps or not).
- Support for FPS variants in quality selection (e.g., 1080p60, 720p30).
- Actual recording quality detection and logging.
- Minimize in system tray feature

---

[Unreleased]: https://github.com/YouG-o/StreamWarden/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/YouG-o/StreamWarden/releases/tag/v0.1.0
