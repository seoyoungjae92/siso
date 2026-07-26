package com.siso.backend.comment;

import jakarta.validation.constraints.Size;

public record CommentCreateRequest(@Size(max = 2000) String body, Long parentId, String stance) {
}
