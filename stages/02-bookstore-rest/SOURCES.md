# Bookstore - полные исходники

Все авторские Java-классы, шаблоны, свойства и тесты. Файлы библиотек Bootstrap и Maven Wrapper поставляются целиком рядом, но здесь не дублируются.

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>bookstore</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>bookstore</name>
    <description>Учебное приложение Spring Boot Bookstore</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### .mvn/wrapper/maven-wrapper.properties

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

### src/main/java/com/example/bookstore/Book.java

```java
package com.example.bookstore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String isbn;
    private int publicationYear;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public Book() {
    }

    public Book(Long id, String title, String author, String isbn, int publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        if (this.category == category) {
            return;
        }

        Category previousCategory = this.category;
        this.category = category;
        if (previousCategory != null) {
            previousCategory.getBooks().remove(this);
        }
        if (category != null && !category.getBooks().contains(this)) {
            category.getBooks().add(this);
        }
    }
}
```

### src/main/java/com/example/bookstore/BookController.java

```java
package com.example.bookstore;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class BookController {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public BookController(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/booklist")
    public String bookList(Model model) {
        model.addAttribute("books", bookRepository.findAll());
        return "booklist";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        addFormModel(model, new Book());
        return "addbook";
    }

    @PostMapping("/save")
    public String addBook(@ModelAttribute BookForm form, @RequestParam Long categoryId) {
        return saveBook(new Book(), form, categoryId);
    }

    @PostMapping("/save/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute BookForm form, @RequestParam Long categoryId) {
        return saveBook(requiredBook(id), form, categoryId);
    }

    private String saveBook(Book book, BookForm form, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());
        book.setPublicationYear(form.getPublicationYear());
        book.setCategory(category);
        bookRepository.save(book);
        return "redirect:/booklist";
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        addFormModel(model, requiredBook(id));
        return "addbook";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.delete(requiredBook(id));
        return "redirect:/booklist";
    }

    private Book requiredBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private void addFormModel(Model model, Book book) {
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryRepository.findAll());
    }
}
```

### src/main/java/com/example/bookstore/BookForm.java

```java
package com.example.bookstore;

public class BookForm {
    private String title;
    private String author;
    private String isbn;
    private int publicationYear;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }
}
```

### src/main/java/com/example/bookstore/BookRepository.java

```java
package com.example.bookstore;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import java.util.List;

@RepositoryRestResource(path = "books", collectionResourceRel = "books")
public interface BookRepository extends CrudRepository<Book, Long> {
    @RestResource(path = "by-title", rel = "by-title")
    List<Book> findByTitleContainingIgnoreCase(@Param("title") String title);
}
```

### src/main/java/com/example/bookstore/BookRestController.java

```java
package com.example.bookstore;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookRestController {
    private final BookRepository bookRepository;

    public BookRestController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public Iterable<Book> allBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<Book> oneBook(@PathVariable("id") Long id) {
        return bookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

### src/main/java/com/example/bookstore/BookstoreApplication.java

```java
package com.example.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
```

### src/main/java/com/example/bookstore/BookstoreDataInitializer.java

```java
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
```

### src/main/java/com/example/bookstore/Category.java

```java
package com.example.bookstore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<Book> books = new ArrayList<>();

    public Category() {
    }

    public Category(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
```

### src/main/java/com/example/bookstore/CategoryRepository.java

```java
package com.example.bookstore;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories")
public interface CategoryRepository extends CrudRepository<Category, Long> {
}
```

### src/main/resources/application-mysql.properties

```properties
spring.datasource.url=${BOOKSTORE_MYSQL_URL}
spring.datasource.username=${BOOKSTORE_MYSQL_USERNAME}
spring.datasource.password=${BOOKSTORE_DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=false
```

### src/main/resources/application.properties

```properties
spring.application.name=bookstore
spring.datasource.url=jdbc:h2:mem:bookstore;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Keep generated HAL endpoints separate from our manual REST controller.
spring.data.rest.base-path=/api
# Classroom demo: the API is not authenticated. Bind only to this computer.
server.address=127.0.0.1
```

### src/main/resources/templates/addbook.html

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="https://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title th:text="${book.id == null ? 'Add Book' : 'Edit Book'}">Add Book</title>
    <link type="text/css" rel="stylesheet" href="/css/bootstrap.min.css" th:href="@{/css/bootstrap.min.css}">
</head>
<body>
<main class="container py-4">
    <h1 th:text="${book.id == null ? 'Add Book' : 'Edit Book'}">Add Book</h1>
    <form th:action="${book.id == null} ? @{/save} : @{/save/{id}(id=${book.id})}"
          th:object="${book}" method="post" class="mt-4">
        <div class="mb-3">
            <label class="form-label" for="title">Title</label>
            <input class="form-control" id="title" type="text" th:field="*{title}" required>
        </div>
        <div class="mb-3">
            <label class="form-label" for="author">Author</label>
            <input class="form-control" id="author" type="text" th:field="*{author}" required>
        </div>
        <div class="mb-3">
            <label class="form-label" for="isbn">ISBN</label>
            <input class="form-control" id="isbn" type="text" th:field="*{isbn}" required>
        </div>
        <div class="mb-3">
            <label class="form-label" for="publicationYear">Publication year</label>
            <input class="form-control" id="publicationYear" type="number" th:field="*{publicationYear}" required>
        </div>
        <div class="mb-3">
            <label class="form-label" for="categoryId">Category</label>
            <select class="form-select" id="categoryId" name="categoryId" required>
                <option th:each="category : ${categories}"
                        th:value="${category.id}"
                        th:text="${category.name}"
                        th:selected="${book.category != null and book.category.id == category.id}"></option>
            </select>
        </div>
        <button class="btn btn-success" type="submit">Save</button>
        <a class="btn btn-secondary" th:href="@{/booklist}">Cancel</a>
    </form>
</main>
</body>
</html>
```

### src/main/resources/templates/booklist.html

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="https://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bookstore</title>
    <link type="text/css" rel="stylesheet" href="/css/bootstrap.min.css" th:href="@{/css/bootstrap.min.css}">
</head>
<body>
<main class="container py-4">
    <h1 class="mb-4">Bookstore</h1>
    <p><a class="btn btn-success" th:href="@{/add}">Add Book</a></p>
    <table class="table table-striped">
        <thead>
        <tr>
            <th scope="col">ID</th>
            <th scope="col">Title</th>
            <th scope="col">Author</th>
            <th scope="col">ISBN</th>
            <th scope="col">Publication year</th>
            <th scope="col">Category</th>
            <th scope="col">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="book : ${books}">
            <td th:text="${book.id}"></td>
            <td th:text="${book.title}"></td>
            <td th:text="${book.author}"></td>
            <td th:text="${book.isbn}"></td>
            <td th:text="${book.publicationYear}"></td>
            <td th:text="${book.category != null ? book.category.name : ''}"></td>
            <td>
                <a class="btn btn-danger" th:href="@{/delete/{id}(id=${book.id})}">Delete</a>
                <a class="btn btn-outline-secondary" th:href="@{/edit/{id}(id=${book.id})}">Edit</a>
            </td>
        </tr>
        </tbody>
    </table>
</main>
</body>
</html>
```

### src/test/java/com/example/bookstore/BookControllerTest.java

```java
package com.example.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void bookListRendersSeededBooksAndBootstrapClasses() throws Exception {
        mockMvc.perform(get("/booklist"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Clean Code")))
                .andExpect(content().string(containsString("class=\"table table-striped\"")))
                .andExpect(content().string(containsString("class=\"btn btn-success\"")))
                .andExpect(content().string(containsString("href=\"/css/bootstrap.min.css\"")));
    }

    @Test
    void editLinkAppearsAfterDeleteLinkAsRequiredByTheExercise() throws Exception {
        MvcResult result = mockMvc.perform(get("/booklist"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html.indexOf(">Delete</a>"))
                .isGreaterThanOrEqualTo(0)
                .isLessThan(html.indexOf(">Edit</a>"));
    }

    @Test
    void bootstrapIsServedLocally() throws Exception {
        mockMvc.perform(get("/css/bootstrap.min.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bootstrap  v5.3.3")));
    }

    @Test
    void addFormRendersTheNamedChapterThreeTemplateWithBookFields() throws Exception {
        mockMvc.perform(get("/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add Book")))
                .andExpect(content().string(containsString("name=\"title\"")))
                .andExpect(content().string(containsString("name=\"author\"")))
                .andExpect(content().string(containsString("name=\"isbn\"")))
                .andExpect(content().string(containsString("name=\"publicationYear\"")));
    }

    @Test
    void saveRedirectsAndNewBookAppearsInList() throws Exception {
        Category category = categoryRepository.findAll().iterator().next();

        mockMvc.perform(post("/save")
                        .param("title", "Domain-Driven Design")
                        .param("author", "Eric Evans")
                        .param("isbn", "9780321125217")
                        .param("publicationYear", "2003")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booklist"));

        mockMvc.perform(get("/booklist"))
                .andExpect(content().string(containsString("Domain-Driven Design")));
    }

    @Test
    void addIgnoresAnInjectedBookIdInsteadOfOverwritingAnotherBook() throws Exception {
        Category category = categoryRepository.findAll().iterator().next();
        Book protectedBook = bookRepository.save(new Book(
                null, "Protected title", "Original author", "protected-isbn", 1999));

        mockMvc.perform(post("/save")
                        .param("id", protectedBook.getId().toString())
                        .param("title", "New independent book")
                        .param("author", "New author")
                        .param("isbn", "new-isbn")
                        .param("publicationYear", "2026")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booklist"));

        Book unchangedBook = bookRepository.findById(protectedBook.getId()).orElseThrow();
        assertThat(unchangedBook.getTitle()).isEqualTo("Protected title");
        assertThat(bookRepository.findAll())
                .extracting(Book::getTitle)
                .contains("New independent book");
    }

    @Test
    void postEditUpdatesOnlyTheRequestedExistingBook() throws Exception {
        Category category = categoryRepository.findAll().iterator().next();
        Book book = bookRepository.save(new Book(
                null, "Before edit", "Before author", "before-isbn", 2001));

        mockMvc.perform(post("/save/{id}", book.getId())
                        .param("title", "After edit")
                        .param("author", "After author")
                        .param("isbn", "after-isbn")
                        .param("publicationYear", "2025")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booklist"));

        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updatedBook.getTitle()).isEqualTo("After edit");
        assertThat(updatedBook.getAuthor()).isEqualTo("After author");
        assertThat(updatedBook.getIsbn()).isEqualTo("after-isbn");
        assertThat(updatedBook.getPublicationYear()).isEqualTo(2025);
        assertThat(updatedBook.getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    void postEditForUnknownBookReturnsTheControllerNotFoundError() throws Exception {
        Category category = categoryRepository.findAll().iterator().next();

        mockMvc.perform(post("/save/999999")
                        .param("title", "Missing")
                        .param("author", "Nobody")
                        .param("isbn", "missing-isbn")
                        .param("publicationYear", "2026")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ResponseStatusException.class)
                        .hasMessageContaining("Book not found"));
    }

    @Test
    void editLoadsExistingBookAndDeleteRemovesIt() throws Exception {
        Book book = bookRepository.save(new Book(
                null, "Working Effectively with Legacy Code", "Michael Feathers", "9780131177055", 2004));

        mockMvc.perform(get("/edit/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Working Effectively with Legacy Code")));

        mockMvc.perform(get("/delete/{id}", book.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booklist"));

        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    @Test
    void unknownBookIdsReturnNotFound() throws Exception {
        mockMvc.perform(get("/edit/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/delete/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFormListsCategoryNames() throws Exception {
        mockMvc.perform(get("/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"categoryId\"")))
                .andExpect(content().string(containsString(">Programming<")))
                .andExpect(content().string(containsString(">Architecture<")));
    }

    @Test
    void savedBookShowsSelectedCategory() throws Exception {
        Category category = categoryRepository.findAll().iterator().next();

        mockMvc.perform(post("/save")
                        .param("title", "Refactoring")
                        .param("author", "Martin Fowler")
                        .param("isbn", "9780134757599")
                        .param("publicationYear", "2018")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/booklist"))
                .andExpect(content().string(containsString("Refactoring")))
                .andExpect(content().string(containsString(category.getName())));
    }

    @Test
    void unknownCategoryReturnsNotFound() throws Exception {
        mockMvc.perform(post("/save")
                        .param("title", "Rejected")
                        .param("author", "Nobody")
                        .param("isbn", "000")
                        .param("publicationYear", "2026")
                        .param("categoryId", "999999"))
                .andExpect(status().isNotFound());
    }
}
```

### src/test/java/com/example/bookstore/BookRepositoryTest.java

```java
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
```

### src/test/java/com/example/bookstore/BookRestControllerTest.java

```java
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
```

### src/test/java/com/example/bookstore/BookstoreApplicationTests.java

```java
package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookstoreApplicationTests {
    @Test
    void applicationContextLoadsWithDefaultH2Profile() {
    }

    @Test
    void defaultProfileConfiguresH2SqlLoggingAndConsole() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).contains("spring.datasource.url=jdbc:h2:mem:bookstore");
        assertThat(properties).contains("spring.jpa.show-sql=true");
        assertThat(properties).contains("spring.h2.console.enabled=true");
    }

    @Test
    void mysqlProfileRequiresTheDedicatedDatabasePasswordVariableWithoutFallback() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application-mysql.properties"));

        assertThat(properties).contains("spring.datasource.url=${BOOKSTORE_MYSQL_URL}");
        assertThat(properties).contains("spring.datasource.username=${BOOKSTORE_MYSQL_USERNAME}");
        assertThat(properties).contains("spring.datasource.password=${BOOKSTORE_DB_PASSWORD}");
        assertThat(properties).doesNotContain("spring.datasource.password=${BOOKSTORE_DB_PASSWORD:");
        assertThat(properties).contains("spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver");
    }
}
```

### src/test/java/com/example/bookstore/BookstoreDataInitializerTest.java

```java
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
```

### src/test/java/com/example/bookstore/CategoryRepositoryTest.java

```java
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
```

### src/test/java/com/example/bookstore/DataRestApiTest.java

```java
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
```
