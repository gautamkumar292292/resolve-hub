
package com.qsp.resolvehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotBlank(message = "Comment content cannot be empty")
        @Size(max = 2000,
                message = "Comment cannot exceed 2000 characters")
        String content

) {
}
