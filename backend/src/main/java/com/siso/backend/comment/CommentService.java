package com.siso.backend.comment;

import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.anon.NicknameGenerator;
import com.siso.backend.pair.TopicPair;
import com.siso.backend.pair.TopicPairRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CommentService {

    private static final String DELETED_STATUS = "deleted";

    private final CommentRepository commentRepository;
    private final TopicPairRepository topicPairRepository;
    private final AnonUserRepository anonUserRepository;
    private final IpHasher ipHasher;

    public CommentService(
            CommentRepository commentRepository,
            TopicPairRepository topicPairRepository,
            AnonUserRepository anonUserRepository,
            IpHasher ipHasher) {
        this.commentRepository = commentRepository;
        this.topicPairRepository = topicPairRepository;
        this.anonUserRepository = anonUserRepository;
        this.ipHasher = ipHasher;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long pairId, String sort) {
        Sort order = "new".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "upCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Comment> comments = commentRepository.findByPair_IdAndStatusNot(pairId, DELETED_STATUS, order);

        Map<Long, UUID> anonIdById = new HashMap<>();
        for (Comment comment : comments) {
            anonIdById.put(comment.getId(), comment.getAnonId());
        }

        return comments.stream()
                .map(comment -> toDto(comment, anonIdById))
                .toList();
    }

    private CommentDto toDto(Comment comment, Map<Long, UUID> anonIdById) {
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        boolean selfReply = parentId != null
                && comment.getAnonId().equals(anonIdById.get(parentId));

        return new CommentDto(
                comment.getId(),
                parentId,
                comment.getNickname(),
                comment.getBody(),
                comment.getStance(),
                comment.getUpCount(),
                comment.getDownCount(),
                selfReply,
                comment.getCreatedAt());
    }

    @Transactional
    public CommentDto create(
            Long pairId, UUID anonId, String remoteAddr, Long parentId, String body, String stance) {
        TopicPair pair = topicPairRepository.findById(pairId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent comment not found"));
            if (parent.getParent() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot reply to a reply (max depth 2)");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        String ipHash = ipHasher.hash(remoteAddr);
        String nickname = NicknameGenerator.generate(anonId.toString());

        AnonUser anonUser = anonUserRepository.findById(anonId)
                .orElseGet(() -> new AnonUser(anonId, now, ipHash));
        anonUser.recordComment(now, ipHash);
        anonUserRepository.save(anonUser);

        Comment comment = new Comment(pair, parent, anonId, nickname, body, ipHash, stance, now);
        commentRepository.save(comment);

        Map<Long, UUID> anonIdById = parent == null ? Map.of() : Map.of(parent.getId(), parent.getAnonId());
        return toDto(comment, anonIdById);
    }
}
