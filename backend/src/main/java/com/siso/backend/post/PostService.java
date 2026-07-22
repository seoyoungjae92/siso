package com.siso.backend.post;

import com.siso.backend.source.Side;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryDto> getFeed(Side side, Pageable pageable) {
        return postRepository.findBySource_SideAndSource_EnabledTrue(side, pageable)
                .map(PostSummaryDto::from);
    }
}
