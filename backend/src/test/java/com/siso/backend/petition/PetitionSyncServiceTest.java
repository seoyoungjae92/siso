package com.siso.backend.petition;

import com.siso.backend.settings.PetitionSettings;
import com.siso.backend.settings.PetitionSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetitionSyncServiceTest {

    @Mock
    private AssemblyPetitionClient assemblyPetitionClient;

    @Mock
    private PetitionRepository petitionRepository;

    @Mock
    private PetitionSettingsRepository petitionSettingsRepository;

    private PetitionSyncService newService() {
        return new PetitionSyncService(assemblyPetitionClient, petitionRepository, petitionSettingsRepository);
    }

    private PetitionSettings settings(String eraco, int windowDays) {
        PetitionSettings settings = mock(PetitionSettings.class);
        lenient().when(settings.getEraco()).thenReturn(eraco);
        lenient().when(settings.getWindowDays()).thenReturn(windowDays);
        return settings;
    }

    @Test
    void sync_newCitizenPetition_isInserted() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200))
                .thenReturn(List.of(new AssemblyPetitionRow(
                        "P1", "2200321", "제목", "국민동의", "53,728", LocalDate.now().toString(), "https://a")));
        when(petitionRepository.findById("P1")).thenReturn(Optional.empty());
        when(petitionRepository.findByStatusAndReceivedAtBefore(anyString(), any())).thenReturn(List.of());

        PetitionSyncResult result = newService().sync();

        assertThat(result.upserted()).isEqualTo(1);
        verify(petitionRepository).save(any(Petition.class));
    }

    @Test
    void sync_existingCollectingPetition_isRefreshedNotReinserted() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        Petition existing = new Petition(
                "P1", "제22대", "2200321", "제목", 10_000L, LocalDate.now(), "https://a", OffsetDateTime.now());
        when(assemblyPetitionClient.fetchList("제22대", 200))
                .thenReturn(List.of(new AssemblyPetitionRow(
                        "P1", "2200321", "제목", "국민동의", "53,728", LocalDate.now().toString(), "https://a")));
        when(petitionRepository.findById("P1")).thenReturn(Optional.of(existing));
        when(petitionRepository.findByStatusAndReceivedAtBefore(anyString(), any())).thenReturn(List.of());

        PetitionSyncResult result = newService().sync();

        assertThat(result.upserted()).isEqualTo(1);
        assertThat(existing.getAgreeCount()).isEqualTo(53_728L);
        verify(petitionRepository, never()).save(any(Petition.class));
    }

    @Test
    void sync_lawmakerIntroducedKind_isSkipped() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200))
                .thenReturn(List.of(new AssemblyPetitionRow(
                        "P1", "2200319", "의원소개 청원", "의원소개", null, LocalDate.now().toString(), "https://a")));
        when(petitionRepository.findByStatusAndReceivedAtBefore(anyString(), any())).thenReturn(List.of());

        PetitionSyncResult result = newService().sync();

        assertThat(result.upserted()).isZero();
        verify(petitionRepository, never()).save(any(Petition.class));
    }

    @Test
    void sync_unparsableAgreeCount_isSkipped() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200))
                .thenReturn(List.of(new AssemblyPetitionRow(
                        "P1", "2200321", "제목", "국민동의", "not-a-number", LocalDate.now().toString(), "https://a")));
        when(petitionRepository.findByStatusAndReceivedAtBefore(anyString(), any())).thenReturn(List.of());

        PetitionSyncResult result = newService().sync();

        assertThat(result.upserted()).isZero();
    }

    @Test
    void sync_expiredCollectingPetition_committeeReferred_closesAsEstablished() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200)).thenReturn(List.of());

        Petition expired = new Petition(
                "P2", "제22대", "2200300", "제목", 60_000L, LocalDate.now().minusDays(31), "https://b", OffsetDateTime.now());
        when(petitionRepository.findByStatusAndReceivedAtBefore(Petition.STATUS_COLLECTING, LocalDate.now().minusDays(30)))
                .thenReturn(List.of(expired));
        when(assemblyPetitionClient.fetchDetail("P2"))
                .thenReturn(new AssemblyPetitionDetail("P2", "농림축산식품해양수산위원회", LocalDate.now().toString(), null));

        PetitionSyncResult result = newService().sync();

        assertThat(result.closed()).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(Petition.STATUS_CLOSED);
        assertThat(expired.getOutcome()).isEqualTo(Petition.OUTCOME_ESTABLISHED);
        assertThat(expired.getCommitteeName()).isEqualTo("농림축산식품해양수산위원회");
    }

    @Test
    void sync_expiredCollectingPetition_noCommitteeReferral_closesAsNotEstablished() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200)).thenReturn(List.of());

        Petition expired = new Petition(
                "P3", "제22대", "2200301", "제목", 8_000L, LocalDate.now().minusDays(31), "https://c", OffsetDateTime.now());
        when(petitionRepository.findByStatusAndReceivedAtBefore(Petition.STATUS_COLLECTING, LocalDate.now().minusDays(30)))
                .thenReturn(List.of(expired));
        when(assemblyPetitionClient.fetchDetail("P3")).thenReturn(new AssemblyPetitionDetail("P3", null, null, "16.0"));

        PetitionSyncResult result = newService().sync();

        assertThat(result.closed()).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(Petition.STATUS_CLOSED);
        assertThat(expired.getOutcome()).isEqualTo(Petition.OUTCOME_NOT_ESTABLISHED);
        assertThat(expired.getAchvRatio()).isEqualTo(16.0f);
    }

    @Test
    void sync_detailFetchThrows_leavesStillCollecting() {
        PetitionSettings settings = settings("제22대", 30);
        when(petitionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(assemblyPetitionClient.fetchList("제22대", 200)).thenReturn(List.of());

        Petition expired = new Petition(
                "P4", "제22대", "2200302", "제목", 1_000L, LocalDate.now().minusDays(31), "https://d", OffsetDateTime.now());
        when(petitionRepository.findByStatusAndReceivedAtBefore(Petition.STATUS_COLLECTING, LocalDate.now().minusDays(30)))
                .thenReturn(List.of(expired));
        when(assemblyPetitionClient.fetchDetail("P4")).thenThrow(new RuntimeException("network error"));

        PetitionSyncResult result = newService().sync();

        assertThat(result.closed()).isZero();
        assertThat(expired.getStatus()).isEqualTo(Petition.STATUS_COLLECTING);
    }
}
