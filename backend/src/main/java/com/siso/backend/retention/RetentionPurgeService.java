package com.siso.backend.retention;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.comment.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 7절: IP 해시는 수집일로부터 90일 후 자동 파기 — 개인정보처리방침(/privacy
 * 3절)에 이미 명시된 값이라 어드민에서 바꾸면 방침 텍스트와 어긋날 위험이
 * 있어, 다른 임계값들과 달리 의도적으로 어드민 설정이 아닌 상수로 고정한다
 * (방침 문구를 바꿀 때 이 상수도 같이 바꿔야 함).
 */
@Service
public class RetentionPurgeService {

    private static final int RETENTION_DAYS = 90;

    private final CommentRepository commentRepository;
    private final AnonUserRepository anonUserRepository;

    public RetentionPurgeService(CommentRepository commentRepository, AnonUserRepository anonUserRepository) {
        this.commentRepository = commentRepository;
        this.anonUserRepository = anonUserRepository;
    }

    @Transactional
    public RetentionPurgeResult purge() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoff = now.minusDays(RETENTION_DAYS);

        int commentsPurged = commentRepository.purgeIpHashOlderThan(cutoff);
        int anonUsersPurged = anonUserRepository.purgeIpHashOlderThan(cutoff);

        return new RetentionPurgeResult(commentsPurged, anonUsersPurged, now);
    }
}
