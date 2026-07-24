package com.siso.backend.admin;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.anon.AnonUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminAbuseService {

    private static final long MIN_CLUSTER_SIZE_FOR_VISIBILITY = 2;

    private final AdminAlertRepository adminAlertRepository;
    private final AnonUserRepository anonUserRepository;

    public AdminAbuseService(AdminAlertRepository adminAlertRepository, AnonUserRepository anonUserRepository) {
        this.adminAlertRepository = adminAlertRepository;
        this.anonUserRepository = anonUserRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminAlertDto> getAlerts(Boolean resolved) {
        List<AdminAlert> alerts = resolved == null
                ? adminAlertRepository.findAllByOrderByCreatedAtDesc()
                : adminAlertRepository.findByResolvedOrderByCreatedAtDesc(resolved);
        return alerts.stream().map(this::toDto).toList();
    }

    @Transactional
    public void resolve(Long id) {
        AdminAlert alert = adminAlertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "alert not found"));
        alert.resolve();
    }

    @Transactional(readOnly = true)
    public List<IpClusterDto> getIpClusters() {
        return anonUserRepository.findClusters(MIN_CLUSTER_SIZE_FOR_VISIBILITY).stream()
                .map(row -> new IpClusterDto(row.getIpHash(), row.getTotal()))
                .toList();
    }

    private AdminAlertDto toDto(AdminAlert alert) {
        return new AdminAlertDto(
                alert.getId(), alert.getType(), alert.getPayload(), alert.isResolved(), alert.getCreatedAt());
    }
}
