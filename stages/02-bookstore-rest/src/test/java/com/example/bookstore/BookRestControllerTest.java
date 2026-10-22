package com.example.bookstore;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookRestControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private CategoryRepository categories;
    private Long bookId;

    @BeforeEach
    void prepareBook() {
        books.deleteAll();
        categories.deleteAll();
        Category category = categories.save(new Category(null, "Programming"));
        Book book = new Book(null, "REST in Practice", "Example Author", "test-001", 2026);
        book.setCategory(category);
        bookId = books.save(book).getId();
    }

    @Test
    void listIsPlainJsonWithCategoryButWithoutRecursiveBooks() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("REST in Practice"))
                .andExpect(jsonPath("$[0].category.name").value("Programming"))
                .andExpect(jsonPath("$[0].category.books").doesNotExist());
    }

    @Test
    void findsOneBookUsingPathVariable() throws Exception {
        mvc.perform(get("/book/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.title").value("REST in Practice"))
                .andExpect(jsonPath("$.category.books").doesNotExist());
    }

    @Test
    void missingBookReturns404() throws Exception {
        mvc.perform(get("/book/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/book/not-a-number")).andExpect(status().isBadRequest());
    }

    @Test
    void htmlPageRemainsAvailable() throws Exception {
        mvc.perform(get("/booklist"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(view().name("booklist"));
    }
}
