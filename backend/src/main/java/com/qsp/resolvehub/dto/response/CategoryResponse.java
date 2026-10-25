
package com.qsp.resolvehub.dto.response;

import com.qsp.resolvehub.entity.Category;

public record CategoryResponse(
        Integer id,
        String name,
        String description,
        boolean active
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}
