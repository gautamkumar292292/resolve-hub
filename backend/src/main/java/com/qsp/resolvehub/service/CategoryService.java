
package com.qsp.resolvehub.service;

import com.qsp.resolvehub.entity.Category;
import java.util.List;

public interface CategoryService {

    Category createCategory(Category category);

    List<Category> getAllCategories();

    Category getCategoryById(Integer id);

    Category updateCategory(Integer id, Category updatedCategory);

    void deleteCategory(Integer id);
}
