package com.siso.backend.comment;

import jakarta.validation.constraints.Size;

public record ReportCreateRequest(String reason, @Size(max = 500) String detail) {
}
