
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.dto.request.CategoryRequest;
import com.qsp.resolvehub.entity.Category;

import java.util.List;

public interface CategoryService {

    Category createCategory(CategoryRequest request);

    List<Category> getAllCategories();

    Category getCategoryById(Integer id);

    Category updateCategory(Integer id, CategoryRequest request);

    void deleteCategory(Integer id);
}
