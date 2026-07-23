package com.siso.backend.admin;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.ReportRepository;
import com.siso.backend.pair.VoteRepository;
import com.siso.backend.post.PostRepository;
import com.siso.backend.source.Source;
import com.siso.backend.source.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AdminDashboardService {

    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final ReportRepository reportRepository;
    private final AnonUserRepository anonUserRepository;
    private final PostRepository postRepository;
    private final SourceRepository sourceRepository;

    public AdminDashboardService(
            CommentRepository commentRepository,
            VoteRepository voteRepository,
            ReportRepository reportRepository,
            AnonUserRepository anonUserRepository,
            PostRepository postRepository,
            SourceRepository sourceRepository) {
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.reportRepository = reportRepository;
        this.anonUserRepository = anonUserRepository;
        this.postRepository = postRepository;
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);

        return new DashboardDto(
                commentRepository.count(),
                voteRepository.count(),
                reportRepository.countByStatus("pending"),
                reportRepository.count(),
                anonUserRepository.countByFirstSeenAfter(since),
                anonUserRepository.countByLastSeenAfter(since),
                sourceRepository.findAllByOrderByIdAsc().stream().map(this::toSourceStat).toList());
    }

    private SourceStatDto toSourceStat(Source source) {
        long postCount = postRepository.countBySource_Id(source.getId());
        OffsetDateTime lastCollectedAt = postRepository
                .findFirstBySource_IdOrderByCollectedAtDesc(source.getId())
                .map(post -> post.getCollectedAt())
                .orElse(null);
        return new SourceStatDto(source.getName(), source.getSide().value(), postCount, lastCollectedAt);
    }
}
