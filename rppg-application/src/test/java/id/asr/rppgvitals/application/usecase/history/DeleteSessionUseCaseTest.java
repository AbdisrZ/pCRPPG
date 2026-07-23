package id.asr.rppgvitals.application.usecase.history;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import id.asr.rppgvitals.domain.session.MeasurementRepository;

class DeleteSessionUseCaseTest {

    private final MeasurementRepository repository = mock(MeasurementRepository.class);

    @Test
    void execute_delegatesDeletionToTheRepository() {
        UUID id = UUID.randomUUID();

        new DeleteSessionUseCase(repository).execute(id);

        verify(repository).deleteById(id);
    }

    @Test
    void execute_withNullId_throws() {
        assertThrows(NullPointerException.class, () -> new DeleteSessionUseCase(repository).execute(null));
    }

    @Test
    void constructor_withNullRepository_throws() {
        assertThrows(NullPointerException.class, () -> new DeleteSessionUseCase(null));
    }
}
