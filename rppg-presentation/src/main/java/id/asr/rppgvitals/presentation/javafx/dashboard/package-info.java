/// JavaFX dashboard screen for the live measurement session.
///
/// Holds `LiveMeasurementController` and `LiveMeasurementViewModel`, following the MVVM-leaning
/// split of `00 §24`: the ViewModel exposes observable `Property` state, the Controller wires
/// FXML to it, and business logic stays out of both. All background-to-UI updates are marshalled
/// onto the JavaFX Application Thread (`00 §24`, `11 §9`). Depends on `rppg-application` only,
/// never on an infrastructure module. Governed by `06_UI_GUIDELINE.md §6.2`.
package id.asr.rppgvitals.presentation.javafx.dashboard;
