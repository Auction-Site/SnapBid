package com.SnapBid.config;

import com.SnapBid.model.Category;
import com.SnapBid.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if categories already exist
            if (categoryRepository.count() == 0) {
                logger.info("No categories found. Initializing default categories.");
                
                // Create default categories
                createCategory("Electronics", "Electronic devices and gadgets");
                createCategory("Clothing", "Clothes, shoes, and accessories");
                createCategory("Home & Garden", "Items for home and garden");
                createCategory("Sports", "Sports equipment and accessories");
                createCategory("Collectibles", "Rare items and collectibles");
                createCategory("Vehicles", "Cars, motorcycles, and other vehicles");
                createCategory("Toys & Games", "Toys, games, and entertainment items");
                createCategory("Art", "Paintings, sculptures, and art pieces");
                
                logger.info("Default categories initialized successfully.");
            } else {
                logger.info("Categories already exist. Skipping initialization.");
            }
        };
    }
    
    private void createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        categoryRepository.save(category);
        logger.info("Created category: {}", name);
    }
} 