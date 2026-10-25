package com.ramya.mealmate.config;

import com.ramya.mealmate.model.MenuItem;
import com.ramya.mealmate.model.Role;
import com.ramya.mealmate.model.User;
import com.ramya.mealmate.repository.MenuItemRepository;
import com.ramya.mealmate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Seeds a sample menu and an admin account on first start so the app is usable immediately. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final MenuItemRepository menu;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(MenuItemRepository menu, UserRepository users, PasswordEncoder encoder,
                      @Value("${mealmate.admin.email:}") String adminEmail,
                      @Value("${mealmate.admin.password:}") String adminPassword) {
        this.menu = menu;
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (menu.count() == 0) {
            menu.saveAll(List.of(
                    item("Margherita Pizza", "San Marzano tomato, fresh mozzarella, basil", "12.99", "Pizza"),
                    item("Pepperoni Pizza", "Classic pepperoni with oregano", "14.49", "Pizza"),
                    item("Chicken Biryani", "Basmati rice, slow-cooked chicken, saffron, raita", "15.99", "Indian"),
                    item("Paneer Butter Masala", "Cottage cheese in tomato butter gravy, with naan", "13.99", "Indian"),
                    item("Classic Burger", "Beef patty, cheddar, lettuce, tomato, house sauce", "11.49", "Burgers"),
                    item("Veggie Burger", "Black bean patty, avocado, chipotle mayo", "10.99", "Burgers"),
                    item("Caesar Salad", "Romaine, parmesan, croutons, Caesar dressing", "8.99", "Salads"),
                    item("Mango Lassi", "Chilled yogurt and mango drink", "4.49", "Drinks")));
            log.info("Seeded sample menu");
        }
        if (!adminEmail.isBlank() && !adminPassword.isBlank() && !users.existsByEmailIgnoreCase(adminEmail)) {
            users.save(new User(adminEmail.toLowerCase(), encoder.encode(adminPassword), "Admin", Role.ADMIN));
            log.info("Created admin account {}", adminEmail);
        }
    }

    private static MenuItem item(String name, String desc, String price, String category) {
        return new MenuItem(name, desc, new BigDecimal(price), category, null);
    }
}
