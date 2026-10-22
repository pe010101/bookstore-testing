package com.example.bookstore;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookstoreDataInitializer {
    @Bean
    CommandLineRunner initialData(CategoryRepository categoryRepository, BookRepository bookRepository) {
        return arguments -> {
            if (categoryRepository.count() > 0 || bookRepository.count() > 0) {
                return;
            }

            Category programming = categoryRepository.save(new Category(null, "Programming"));
            Category architecture = categoryRepository.save(new Category(null, "Architecture"));
            Category databases = categoryRepository.save(new Category(null, "Databases"));

            Book cleanCode = new Book(null, "Clean Code", "Robert C. Martin", "9780132350884", 2008);
            cleanCode.setCategory(architecture);
            Book effectiveJava = new Book(null, "Effective Java", "Joshua Bloch", "9780134685991", 2018);
            effectiveJava.setCategory(programming);
            Book springInAction = new Book(null, "Spring in Action", "Craig Walls", "9781617297571", 2022);
            springInAction.setCategory(programming);

            bookRepository.saveAll(List.of(cleanCode, effectiveJava, springInAction));
        };
    }
}
