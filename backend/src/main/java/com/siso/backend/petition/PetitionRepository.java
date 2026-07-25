package com.siso.backend.petition;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PetitionRepository extends JpaRepository<Petition, String> {

    List<Petition> findByStatusOrderByAgreeCountDesc(String status, Pageable pageable);

    List<Petition> findByStatusAndReceivedAtBefore(String status, LocalDate cutoff);

    List<Petition> findAllByOrderByLastSyncedAtDesc(Pageable pageable);
}
