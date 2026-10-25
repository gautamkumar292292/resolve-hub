
package com.qsp.resolvehub.service.impl;

import com.qsp.resolvehub.entity.Category;
import com.qsp.resolvehub.exception.ResourceNotFoundException;
import com.qsp.resolvehub.repository.CategoryRepository;
import com.qsp.resolvehub.service.CategoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category) {
        validateCategory(category);

        String name = category.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A category with this name already exists");
        }

        category.setId(null);
        category.setName(name);
        category.setActive(true);

        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .filter(Category::isActive)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Category not found");
        }

        return category;
    }

    @Override
    public Category updateCategory(
            Integer id,
            Category updatedCategory) {

        validateCategory(updatedCategory);

        Category existing = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        String name = updatedCategory.getName().trim();

        boolean duplicate = categoryRepository
                .existsByNameIgnoreCaseAndIdNot(name, id);

        if (duplicate) {
            throw new IllegalArgumentException(
                    "A category with this name already exists");
        }

        existing.setName(name);
        existing.setDescription(updatedCategory.getDescription());
        existing.setActive(updatedCategory.isActive());

        return categoryRepository.save(existing);
    }

    @Override
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        // Soft delete: preserve categories referenced by historical tickets.
        category.setActive(false);
        categoryRepository.save(category);
    }

    private void validateCategory(Category category) {
        if (category == null || !StringUtils.hasText(category.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }

        if (category.getName().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Category name cannot exceed 100 characters");
        }

        if (category.getDescription() != null
                && category.getDescription().length() > 500) {
            throw new IllegalArgumentException(
                    "Category description cannot exceed 500 characters");
        }
    }
}
