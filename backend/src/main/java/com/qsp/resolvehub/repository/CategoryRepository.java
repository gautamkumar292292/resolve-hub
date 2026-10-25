package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository
        extends JpaRepository<Category, Integer> {
}