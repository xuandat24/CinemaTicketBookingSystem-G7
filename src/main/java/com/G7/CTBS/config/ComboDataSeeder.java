package com.G7.CTBS.config;

import com.G7.CTBS.entity.Combo;
import com.G7.CTBS.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class    ComboDataSeeder implements CommandLineRunner {

    private final ComboRepository comboRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long comboCount = comboRepository.count();
        if (comboCount > 0) {
            log.info("Skip combo seed because combos table already has {} rows.", comboCount);
            return;
        }

        List<Combo> combos = List.of(
                combo("Ice Cream", "1 Ice Cream (Various flavors)", 30000, "icecream.png"),
                combo("Popcorn (Large)", "1 Large Popcorn", 45000, "popcorn.png"),
                combo("Soft Drink", "1 Soft Drink (Coca / Pepsi)", 25000, "drink.png"),
                combo("Snack Oishi", "1 Pack of Oishi Snack", 20000, "snack.png"),
                combo("Ice Cream Combo", "1 Ice Cream + 1 Soft Drink + 1 Large Popcorn", 90000, "combo1.png"),
                combo("Sweet Combo", "1 Large Popcorn + 2 Soft Drinks", 85000, "combo2.png"),
                combo("Family Combo", "2 Large Popcorn + 4 Soft Drinks + 2 Snack Oishi", 210000, "combo3.png"),
                combo("Beta Combo", "1 Large Popcorn + 1 Soft Drink", 65000, "combo4.png"),
                combo("Astronaut Combo", "1 Special Drink + 1 Popcorn", 120000, "combo5.png")
        );

        comboRepository.saveAll(combos);
        log.info("Seeded {} default combos.", combos.size());
    }

    private Combo combo(String name, String description, int price, String image) {
        Combo combo = new Combo();
        combo.setName(name);
        combo.setDescription(description);
        combo.setPrice(price);
        combo.setImage(image);
        return combo;
    }
}