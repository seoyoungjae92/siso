package com.siso.backend.comment;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.moderation.ProfanityFilter;
import com.siso.backend.pair.TopicPair;
import com.siso.backend.pair.TopicPairRepository;
import com.siso.backend.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANON_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ANON_C = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private TopicPairRepository topicPairRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    @Mock
    private IpHasher ipHasher;

    @Mock
    private ProfanityFilter profanityFilter;

    @Mock
    private RateLimiter rateLimiter;

    private CommentService newService() {
        return new CommentService(
                commentRepository,
                reactionRepository,
                topicPairRepository,
                anonUserRepository,
                ipHasher,
                profanityFilter,
                rateLimiter);
    }

    @Test
    void create_topLevelComment_succeeds() {
        CommentService service = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        when(topicPairRepository.findById(1L)).thenReturn(Optional.of(pair));
        when(anonUserRepository.findById(ANON_A)).thenReturn(Optional.empty());
        when(ipHasher.hash(any())).thenReturn("hashed-ip");

        CommentDto dto = service.create(1L, ANON_A, "127.0.0.1", null, "본문", "left");

        assertThat(dto.parentId()).isNull();
        assertThat(dto.body()).isEqualTo("본문");
        assertThat(dto.selfReply()).isFalse();
        assertThat(dto.myReaction()).isNull();
    }

    @Test
    void create_rateLimited_isRejectedBeforeAnyLookup() {
        CommentService service = newService();
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
                .when(rateLimiter).checkOrThrow("comment", ANON_A);

        assertThatThrownBy(() -> service.create(1L, ANON_A, "127.0.0.1", null, "본문", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void create_containsBannedWord_isRejected() {
        CommentService service = newService();
        when(profanityFilter.containsBannedWord("나쁜말")).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, ANON_A, "127.0.0.1", null, "나쁜말", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_replyToTopLevelComment_succeeds() {
        CommentService service = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment topLevel = new Comment(pair, null, ANON_A, "닉네임", "부모", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(topLevel, "id", 10L);

        when(topicPairRepository.findById(1L)).thenReturn(Optional.of(pair));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(topLevel));
        when(anonUserRepository.findById(ANON_B)).thenReturn(Optional.empty());
        when(ipHasher.hash(any())).thenReturn("hashed-ip");

        CommentDto dto = service.create(1L, ANON_B, "127.0.0.1", 10L, "대댓글", null);

        assertThat(dto.parentId()).isEqualTo(10L);
        assertThat(dto.selfReply()).isFalse();
    }

    @Test
    void create_replyBySameAnonIdAsParent_marksSelfReply() {
        CommentService service = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment topLevel = new Comment(pair, null, ANON_A, "닉네임", "부모", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(topLevel, "id", 10L);

        when(topicPairRepository.findById(1L)).thenReturn(Optional.of(pair));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(topLevel));
        when(anonUserRepository.findById(ANON_A)).thenReturn(Optional.empty());
        when(ipHasher.hash(any())).thenReturn("hashed-ip");

        CommentDto dto = service.create(1L, ANON_A, "127.0.0.1", 10L, "셀프 대댓글", null);

        assertThat(dto.selfReply()).isTrue();
    }

    @Test
    void create_replyToAReply_isRejected() {
        CommentService service = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment topLevel = new Comment(pair, null, ANON_A, "닉네임", "부모", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(topLevel, "id", 10L);
        Comment reply = new Comment(pair, topLevel, ANON_B, "닉네임2", "대댓글", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(reply, "id", 11L);

        when(topicPairRepository.findById(1L)).thenReturn(Optional.of(pair));
        when(commentRepository.findById(11L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> service.create(1L, ANON_C, "127.0.0.1", 11L, "대대댓글", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("depth");
    }

    @Test
    void getComments_returnsEmptyListWhenNoComments() {
        CommentService service = newService();
        when(commentRepository.findByPair_IdAndStatusNot(eq(1L), eq("deleted"), any(Sort.class)))
                .thenReturn(List.of());

        List<CommentDto> result = service.getComments(1L, "top", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getComments_blindedComment_bodyIsReplacedWithPlaceholder() {
        CommentService service = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment parent = new Comment(pair, null, ANON_A, "닉네임", "원본 내용", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(parent, "id", 10L);
        parent.blind();
        Comment reply = new Comment(pair, parent, ANON_B, "닉네임2", "대댓글", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(reply, "id", 11L);

        when(commentRepository.findByPair_IdAndStatusNot(eq(1L), eq("deleted"), any(Sort.class)))
                .thenReturn(List.of(parent, reply));

        List<CommentDto> result = service.getComments(1L, "top", null);

        CommentDto parentDto = result.stream().filter(c -> c.id().equals(10L)).findFirst().orElseThrow();
        CommentDto replyDto = result.stream().filter(c -> c.id().equals(11L)).findFirst().orElseThrow();

        assertThat(parentDto.blinded()).isTrue();
        assertThat(parentDto.body()).isEqualTo("신고 처리로 가려진 댓글입니다");
        assertThat(replyDto.blinded()).isFalse();
        assertThat(replyDto.parentId()).isEqualTo(10L);
    }

    private Comment commentWithId(long id, UUID anonId) {
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment comment = new Comment(pair, null, anonId, "닉네임", "본문", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    void react_withNoExistingReaction_createsAndIncrementsCount() {
        CommentService service = newService();
        Comment comment = commentWithId(1L, ANON_A);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reactionRepository.findByComment_IdAndAnonId(1L, ANON_B)).thenReturn(Optional.empty());

        service.react(1L, ANON_B, "up");

        assertThat(comment.getUpCount()).isEqualTo(1);
        assertThat(comment.getDownCount()).isEqualTo(0);
        verify(reactionRepository).save(any(Reaction.class));
    }

    @Test
    void react_sameTypeAgain_togglesOff() {
        CommentService service = newService();
        Comment comment = commentWithId(1L, ANON_A);
        comment.adjustUpCount(1);
        Reaction existing = new Reaction(comment, ANON_B, "up");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reactionRepository.findByComment_IdAndAnonId(1L, ANON_B)).thenReturn(Optional.of(existing));

        service.react(1L, ANON_B, "up");

        assertThat(comment.getUpCount()).isEqualTo(0);
        verify(reactionRepository).delete(existing);
    }

    @Test
    void react_switchingType_adjustsBothCounts() {
        CommentService service = newService();
        Comment comment = commentWithId(1L, ANON_A);
        comment.adjustUpCount(1);
        Reaction existing = new Reaction(comment, ANON_B, "up");
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reactionRepository.findByComment_IdAndAnonId(1L, ANON_B)).thenReturn(Optional.of(existing));

        service.react(1L, ANON_B, "down");

        assertThat(comment.getUpCount()).isEqualTo(0);
        assertThat(comment.getDownCount()).isEqualTo(1);
        assertThat(existing.getType()).isEqualTo("down");
    }

    @Test
    void react_invalidType_isRejected() {
        CommentService service = newService();

        assertThatThrownBy(() -> service.react(1L, ANON_A, "sideways"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
