package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookstoreDataInitializerTest {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    @Qualifier("initialData")
    private CommandLineRunner initialData;

    @Test
    void runningInitializerAgainDoesNotDuplicateBooksOrCategories() throws Exception {
        long booksBefore = bookRepository.count();
        long categoriesBefore = categoryRepository.count();

        initialData.run();

        assertThat(bookRepository.count()).isEqualTo(booksBefore);
        assertThat(categoryRepository.count()).isEqualTo(categoriesBefore);
    }
}
