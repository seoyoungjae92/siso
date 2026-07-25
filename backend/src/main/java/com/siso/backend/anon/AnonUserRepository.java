package com.siso.backend.anon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnonUserRepository extends JpaRepository<AnonUser, UUID> {

    long countByFirstSeenAfter(OffsetDateTime since);

    long countByLastSeenAfter(OffsetDateTime since);

    List<AnonUser> findByIpHashRecent(String ipHashRecent);

    @Query("SELECT au.ipHashRecent AS ipHash, COUNT(au) AS total FROM AnonUser au "
            + "WHERE au.ipHashRecent IS NOT NULL GROUP BY au.ipHashRecent "
            + "HAVING COUNT(au) >= :minSize ORDER BY COUNT(au) DESC")
    List<IpClusterCount> findClusters(@Param("minSize") long minSize);

    interface IpClusterCount {
        String getIpHash();

        long getTotal();
    }

    // 7절: IP 해시는 마지막 활동(last_seen)으로부터 90일 후 파기 —
    // ip_hash_recent는 활동할 때마다 최신값으로 갱신되므로(AnonUser.
    // recordComment/recordVote), last_seen이 오래됐다는 건 그 해시값도
    // 그만큼 오래됐다는 뜻. trust_score/comment_count 등 나머지 필드는
    // 유지(개인정보가 아니라 집계값).
    @Modifying
    @Query("UPDATE AnonUser au SET au.ipHashRecent = NULL WHERE au.lastSeen < :cutoff AND au.ipHashRecent IS NOT NULL")
    int purgeIpHashOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
