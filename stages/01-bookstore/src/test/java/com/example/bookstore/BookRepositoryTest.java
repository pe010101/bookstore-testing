package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class BookRepositoryTest {
    @Autowired
    private BookRepository bookRepository;

    @Test
    void savesAndReadsEveryBookField() {
        Book saved = bookRepository.save(new Book(
                null, "Clean Code", "Robert C. Martin", "9780132350884", 2008));

        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getTitle()).isEqualTo("Clean Code");
        assertThat(reloaded.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(reloaded.getIsbn()).isEqualTo("9780132350884");
        assertThat(reloaded.getPublicationYear()).isEqualTo(2008);
    }
}
