package com.siso.backend.retention;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetentionPurgeSchedulerTest {

    @Mock
    private RetentionPurgeService retentionPurgeService;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    private RetentionPurgeScheduler newScheduler() {
        return new RetentionPurgeScheduler(retentionPurgeService, adminAlertRepository);
    }

    @Test
    void purge_success_doesNotRaiseAlert() {
        newScheduler().purge();

        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void purge_failure_raisesAlertInsteadOfPropagating() {
        // 개인정보처리방침에 명시된 90일 파기 약속을 지키는 배치라, 실패해도
        // 아무 알림 없이 조용히 안 도는 걸 막아야 한다 — 실패해도 예외를
        // 삼키고(스케줄러가 죽지 않게) 관리자 알림을 남겨야 한다.
        doThrow(new RuntimeException("DB 연결 실패"))
                .when(retentionPurgeService)
                .purge();

        newScheduler().purge();

        verify(adminAlertRepository).save(any(AdminAlert.class));
    }

    @Test
    void purge_failure_doesNotThrow() {
        doThrow(new RuntimeException("DB 연결 실패"))
                .when(retentionPurgeService)
                .purge();

        // 예외가 스케줄러 밖으로 새 나가지 않아야 다음 트리거도 정상 동작한다.
        newScheduler().purge();
    }
}
