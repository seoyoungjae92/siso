package com.siso.backend.comment;

public record CommentCreateRequest(String body, Long parentId, String stance) {
}
