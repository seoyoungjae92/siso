package com.siso.backend.anon;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
