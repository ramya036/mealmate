package com.ramya.mealmate.controller;

import com.ramya.mealmate.dto.Dtos.MenuItemRequest;
import com.ramya.mealmate.dto.Dtos.MenuItemResponse;
import com.ramya.mealmate.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** GET endpoints are public; writes require ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menu;

    public MenuController(MenuService menu) {
        this.menu = menu;
    }

    @GetMapping
    public List<MenuItemResponse> list(@RequestParam(required = false) String category) {
        return menu.list(category);
    }

    @GetMapping("/{id}")
    public MenuItemResponse get(@PathVariable Long id) {
        return menu.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest req) {
        return menu.create(req);
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable Long id, @Valid @RequestBody MenuItemRequest req) {
        return menu.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        menu.archive(id);
    }
}
