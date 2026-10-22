package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class CategoryRepositoryTest {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void bookBelongsToCategoryAndCategoryLoadsBooks() {
        Category category = categoryRepository.save(new Category(null, "Programming"));
        Book book = new Book(null, "Effective Java", "Joshua Bloch", "9780134685991", 2018);
        book.setCategory(category);
        bookRepository.save(book);

        entityManager.flush();
        entityManager.clear();

        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();

        assertThat(reloaded.getName()).isEqualTo("Programming");
        assertThat(reloaded.getBooks()).extracting(Book::getTitle)
                .containsExactly("Effective Java");
    }

    @Test
    void movingBookBetweenCategoriesKeepsBothSidesConsistent() {
        Category original = new Category(null, "Programming");
        Category replacement = new Category(null, "Databases");
        Book book = new Book(null, "Effective Java", "Joshua Bloch", "9780134685991", 2018);

        book.setCategory(original);
        book.setCategory(replacement);

        assertThat(book.getCategory()).isSameAs(replacement);
        assertThat(original.getBooks()).doesNotContain(book);
        assertThat(replacement.getBooks()).containsExactly(book);
    }

    @Test
    void booksAttributeCanBeReplacedThroughItsRequiredSetter() throws Exception {
        Category category = new Category(null, "Testing");
        List<Book> replacement = List.of(
                new Book(null, "Test-Driven Development", "Kent Beck", "9780321146533", 2002));
        Method setter = Arrays.stream(Category.class.getMethods())
                .filter(method -> method.getName().equals("setBooks"))
                .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{List.class}))
                .findFirst()
                .orElse(null);

        assertThat(setter).as("Category must expose setBooks(List<Book>)").isNotNull();
        setter.invoke(category, replacement);

        assertThat(category.getBooks()).isSameAs(replacement);
    }
}
