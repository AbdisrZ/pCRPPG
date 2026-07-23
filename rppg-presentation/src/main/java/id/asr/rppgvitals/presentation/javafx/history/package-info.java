/// JavaFX screens for session history and session detail.
///
/// Holds `SessionHistoryController` and `SessionHistoryViewModel`, following the same
/// MVVM-leaning split as the dashboard (`00 §24`). Renders stored `SessionSummary` listings and
/// per-session detail (including XChart trend rendering, a presentation concern that is not a
/// domain port per `03 §4`). Depends on `rppg-application` only. Governed by
/// `06_UI_GUIDELINE.md §6.3–§6.4`.
package id.asr.rppgvitals.presentation.javafx.history;
