package com.ramya.mealmate.repository;

import com.ramya.mealmate.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByAvailableTrueOrderByCategoryAscNameAsc();

    List<MenuItem> findByAvailableTrueAndCategoryIgnoreCaseOrderByNameAsc(String category);
}
