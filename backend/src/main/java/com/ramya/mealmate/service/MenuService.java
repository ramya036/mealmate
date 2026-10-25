package com.ramya.mealmate.service;

import com.ramya.mealmate.dto.Dtos.MenuItemRequest;
import com.ramya.mealmate.dto.Dtos.MenuItemResponse;
import com.ramya.mealmate.exception.ApiException;
import com.ramya.mealmate.model.MenuItem;
import com.ramya.mealmate.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository menu;

    public MenuService(MenuItemRepository menu) {
        this.menu = menu;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> list(String category) {
        List<MenuItem> items = (category == null || category.isBlank())
                ? menu.findByAvailableTrueOrderByCategoryAscNameAsc()
                : menu.findByAvailableTrueAndCategoryIgnoreCaseOrderByNameAsc(category);
        return items.stream().map(MenuItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse get(Long id) {
        return MenuItemResponse.from(menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item")));
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest req) {
        MenuItem item = new MenuItem(req.name(), req.description(), req.price(), req.category(), req.imageUrl());
        item.setAvailable(req.available());
        return MenuItemResponse.from(menu.save(item));
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest req) {
        MenuItem item = menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item"));
        item.update(req.name(), req.description(), req.price(), req.category(), req.imageUrl(), req.available());
        return MenuItemResponse.from(item);
    }

    /** Soft delete: past orders still reference the item. */
    @Transactional
    public void archive(Long id) {
        menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item")).setAvailable(false);
    }
}
