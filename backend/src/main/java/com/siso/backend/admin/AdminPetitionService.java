package com.siso.backend.admin;

import com.siso.backend.petition.Petition;
import com.siso.backend.petition.PetitionRepository;
import com.siso.backend.petition.PetitionSyncResult;
import com.siso.backend.petition.PetitionSyncService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminPetitionService {

    private static final int LIST_LIMIT = 100;

    private final PetitionRepository petitionRepository;
    private final PetitionSyncService petitionSyncService;

    public AdminPetitionService(PetitionRepository petitionRepository, PetitionSyncService petitionSyncService) {
        this.petitionRepository = petitionRepository;
        this.petitionSyncService = petitionSyncService;
    }

    @Transactional(readOnly = true)
    public List<AdminPetitionDto> list() {
        return petitionRepository.findAllByOrderByLastSyncedAtDesc(PageRequest.of(0, LIST_LIMIT)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PetitionSyncResult syncNow() {
        return petitionSyncService.sync();
    }

    private AdminPetitionDto toDto(Petition petition) {
        return new AdminPetitionDto(
                petition.getPttId(),
                petition.getTitle(),
                petition.getAgreeCount(),
                petition.getReceivedAt().toString(),
                petition.getLinkUrl(),
                petition.getStatus(),
                petition.getOutcome(),
                petition.getCommitteeName(),
                petition.getCommitteeReferredAt() == null ? null : petition.getCommitteeReferredAt().toString(),
                petition.getAchvRatio(),
                petition.getLastSyncedAt().toString(),
                petition.getClosedAt() == null ? null : petition.getClosedAt().toString());
    }
}
