
package com.qsp.resolvehub.dto.request;

import com.qsp.resolvehub.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(

        @NotBlank(message = "Title is required")
        @Size(
                max = 150,
                message = "Title cannot exceed 150 characters"
        )
        String title,

        @NotBlank(message = "Description is required")
        @Size(
                max = 5000,
                message = "Description cannot exceed 5000 characters"
        )
        String description,

        @NotNull(message = "Category is required")
        Integer categoryId,

        @NotNull(message = "Priority is required")
        TicketPriority priority

) {
}
