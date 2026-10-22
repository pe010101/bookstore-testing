package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DataRestApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private CategoryRepository categories;
    private Long bookId;
    private Long categoryId;

    @BeforeEach
    void prepareData() {
        books.deleteAll();
        categories.deleteAll();
        Category category = categories.save(new Category(null, "Programming"));
        categoryId = category.getId();
        Book book = new Book(null, "Learning Spring", "Test Author", "api-001", 2026);
        book.setCategory(category);
        bookId = books.save(book).getId();
    }

    @Test
    void apiRootProvidesRepositoryLinks() throws Exception {
        mvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.books.href").exists())
                .andExpect(jsonPath("$._links.categories.href").exists());
    }

    @Test
    void listsBooksInHal() throws Exception {
        mvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books", hasSize(1)))
                .andExpect(jsonPath("$._embedded.books[0].title").value("Learning Spring"))
                .andExpect(jsonPath("$._embedded.books[0]._links.self.href").exists());
    }

    @Test
    void querySearchesByTitleIgnoringCase() throws Exception {
        mvc.perform(get("/api/books/search/by-title").param("title", "SPRING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.books", hasSize(1)));
    }

    @Test
    void createsBookWithCategoryUri() throws Exception {
        String json = """
                {"title":"New book","author":"Student","isbn":"api-002","publicationYear":2026,
                 "category":"http://localhost/api/categories/%d"}
                """.formatted(categoryId);
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .accept("application/hal+json").content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("New book"));
        Book created = books.findByTitleContainingIgnoreCase("New book").get(0);
        assertThat(created.getCategory().getId()).isEqualTo(categoryId);
    }

    @Test
    void canPatchBook() throws Exception {
        mvc.perform(patch("/api/books/{id}", bookId).contentType(MediaType.APPLICATION_JSON)
                        .accept("application/hal+json").content("{\"title\":\"Changed title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Changed title"));
        assertThat(books.findById(bookId).orElseThrow().getTitle()).isEqualTo("Changed title");
    }

    @Test
    void canReplaceBook() throws Exception {
        String json = """
                {"title":"Replacement","author":"Student","isbn":"api-003","publicationYear":2025,
                 "category":"http://localhost/api/categories/%d"}
                """.formatted(categoryId);
        mvc.perform(put("/api/books/{id}", bookId).contentType(MediaType.APPLICATION_JSON)
                        .accept("application/hal+json").content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Replacement"));
    }

    @Test
    void deletesOnlySelectedBook() throws Exception {
        mvc.perform(delete("/api/books/{id}", bookId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(status().isNotFound());
        assertThat(categories.existsById(categoryId)).isTrue();
    }

    @Test
    void missingDataRestBookReturns404() throws Exception {
        mvc.perform(get("/api/books/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithBrowserAcceptHeaderCanReturn200() throws Exception {
        mvc.perform(delete("/api/books/{id}", bookId).accept("*/*"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(status().isNotFound());
    }
}
