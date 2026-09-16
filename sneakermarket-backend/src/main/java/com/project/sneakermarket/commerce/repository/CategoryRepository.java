package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
