package com.siso.backend.admin;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.anon.AnonUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAbuseServiceTest {

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    private AdminAbuseService newService() {
        return new AdminAbuseService(adminAlertRepository, anonUserRepository);
    }

    @Test
    void getAlerts_noFilter_returnsAll() {
        AdminAlert alert = new AdminAlert("activity_spike", Map.of("count", 30), OffsetDateTime.now());
        when(adminAlertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert));

        List<AdminAlertDto> result = newService().getAlerts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("activity_spike");
    }

    @Test
    void getAlerts_filteredByResolved() {
        when(adminAlertRepository.findByResolvedOrderByCreatedAtDesc(false)).thenReturn(List.of());

        List<AdminAlertDto> result = newService().getAlerts(false);

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_marksAlertResolved() {
        AdminAlert alert = new AdminAlert("activity_spike", Map.of(), OffsetDateTime.now());
        when(adminAlertRepository.findById(1L)).thenReturn(Optional.of(alert));

        newService().resolve(1L);

        assertThat(alert.isResolved()).isTrue();
    }

    @Test
    void resolve_notFound_throws() {
        when(adminAlertRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().resolve(1L)).isInstanceOf(ResponseStatusException.class);
    }
}
