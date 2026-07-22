package com.siso.backend.comment;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.pair.TopicPair;
import com.siso.backend.pair.TopicPairRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANON_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ANON_C = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TopicPairRepository topicPairRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    @Mock
    private IpHasher ipHasher;

    private CommentService newService() {
        return new CommentService(commentRepository, topicPairRepository, anonUserRepository, ipHasher);
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

        List<CommentDto> result = service.getComments(1L, "top");

        assertThat(result).isEmpty();
    }
}
