package com.siso.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long> {

    List<AdminAlert> findAllByOrderByCreatedAtDesc();
}
