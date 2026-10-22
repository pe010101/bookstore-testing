# Chapter 5.1 - Bookstore REST: от начала до проверки

## 1. Что должно получиться

[Исходное задание](ASSIGNMENT.md) требует два метода: JSON всех книг и одной книги по ID.
Существующая HTML-страница остаётся работать. Это продолжение нашего Bookstore, не новое React-приложение.

Маршрут: запустить прежний Bookstore → убрать цикл JSON → добавить REST-контроллер → проверить список, одну книгу и ошибки → подготовить свою сдачу.

## 2. Как открыть готовый вариант

Распакуйте комплект в обычную папку, откройте `projects/backend/bookstore-rest` в редакторе и терминале.
Внутри должны быть `pom.xml`, `mvnw`, `src`. Не запускайте Maven из `HW.Group-2`, если там нет `pom.xml`.

```bash
java -version
bash ./mvnw test
bash ./mvnw spring-boot:run
```

Откройте `http://localhost:8080/booklist`. Если порт занят, используйте:

```bash
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Далее во всех примерах замените `8080` на свой порт. `Ctrl+C` останавливает приложение, но не удаляет проект.
Нужен JDK 17 или 21; Wrapper управляет Maven. MySQL не требуется: используем H2.

## 3. Как внести этот этап в СВОЙ репозиторий

Не заменяйте целиком свой проект и не переносите чужую `.git`. Сначала убедитесь, что ваш предыдущий Bookstore запускается.

Для этапа 5.1 нужны ровно две содержательные правки:

1. Добавить `@JsonIgnore` к обратному списку книг в `Category`.
2. Создать `BookRestController.java` рядом с имеющимся `BookController.java`.

Если у вас package, например, `fi.haagahelia.bookstore`, сохраните именно его во всех новых Java-файлах. Путь папок должен соответствовать package.
Названия `BookRepository`, `Category`, свойств и методов должны совпадать с вашим проектом.
Зависимость Data REST и `/api` относятся к следующему этапу: для двух ручных GET-методов достаточно уже имеющихся Spring Web и JPA.

## 4. Почему возникает бесконечный JSON

В БД связь правильная: книга содержит категорию, категория содержит список книг.
Но при преобразовании объекта в JSON получается:

```text
Book → Category → books[] → Book → Category → ...
```

`@JsonIgnore` на `Category.books` прекращает эту цепочку. Это не удаление поля и не разрыв связи в базе.
В HTML Thymeleaf и в Java список по-прежнему доступен; только сериализатор не включает его в ответ.

## 5. Зачем отдельный контроллер

У старого `BookController` остаётся `@Controller`: он возвращает имена HTML-шаблонов.
У нового `BookRestController` стоит `@RestController`: Spring отправляет возвращённый объект в HTTP-ответе как JSON.
Не заменяйте аннотацию старого класса на `@RestController`: иначе браузер увидит строку `booklist`, а не таблицу.

| Конструкция | Что делает | Почему здесь |
|---|---|---|
| Конструктор с `BookRepository` | Spring передаёт готовый репозиторий | Нет ручного `new` и подключения к БД в контроллере |
| `@GetMapping("/books")` | Связывает GET-запрос и метод | Операция только читает данные |
| `findAll()` | Читает все книги | Запрос к БД создаёт Spring Data JPA |
| `Iterable<Book>` | Тип результата `CrudRepository.findAll()` | Не требуется небезопасное приведение к `List<Book>` |
| `@PathVariable("id")` | Получает число из `/book/7` | Это ID в пути, не `?id=7` |
| `findById(id)` | Возвращает `Optional<Book>` | Книга может отсутствовать |
| `map(ResponseEntity::ok)` | При наличии создаёт HTTP 200 с книгой | Успех имеет ясный код ответа |
| `orElseGet(...)` | При отсутствии создаёт HTTP 404 | Не возвращаем ложный успех с пустым JSON |

`Optional.map` здесь не JavaScript `Array.map`: это преобразование единственного присутствующего значения.
`::` - ссылка на метод Java; `ResponseEntity::ok` значит «вызвать `ok` для найденной книги».

## 6. Проверить вручную

```bash
curl -i http://localhost:8080/books
```

Ожидается `200`, тип `application/json` и массив. Найдите реальный ID в нём:

```bash
curl -i http://localhost:8080/book/1
curl -i http://localhost:8080/book/9223372036854775807
curl -i http://localhost:8080/book/not-a-number
```

Для существующей книги - `200`; для отсутствующей - `404`; для нечислового ID - `400`.
В каждой книге есть `category`, но внутри неё нет `books`. Убедитесь, что ответ конечный, без повторяющихся вложенных категорий.
Проверьте старый `http://localhost:8080/booklist`: HTML-таблица, добавление и редактирование должны остаться доступны.

Пример формы JSON, значения зависят от ваших данных:

```json
{
  "id": 1,
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "9780132350884",
  "publicationYear": 2008,
  "category": { "id": 2, "name": "Architecture" }
}
```

Не ожидайте, что первый ID всегда 1: в вашем MySQL или ранее использованной БД номера могут отличаться.

## 7. Автоматическая проверка

В комплекте `BookRestControllerTest` проверяет массив, категорию без рекурсии, одну книгу, 404, 400 и сохранённую HTML-страницу.

```bash
bash ./mvnw test
```

Тесты используют H2 и не нуждаются в MySQL-пароле. `BUILD SUCCESS` относится к проверкам, не означает отправку преподавателю.

## 8. Типичные ошибки

- **`./mvnw: no such file`**: терминал открыт не рядом с `pom.xml`.
- **`Port 8080 already in use`**: выберите другой порт командой выше; чужой процесс убивать не нужно.
- **`Optional<Book> cannot be converted to Book`**: у `findById` результат `Optional`, а не `Book`; используйте полный метод ниже.
- **`StackOverflowError`/огромный JSON**: забыта `@JsonIgnore` на обратной связи.
- **После добавления REST исчез HTML**: аннотация старого контроллера была заменена; нужен отдельный класс.
- **В браузере `/api/books` показывает `_embedded`**: это следующий этап, Spring Data REST; ручной список находится на `/books`.
- **Ошибка подключения MySQL**: для этой работы профиль MySQL не активируйте; проверьте `SPRING_PROFILES_ACTIVE`.

## 9. Что сдавать и как объяснить

Своими словами: «Репозиторий читает сущности из БД, REST-контроллер возвращает их вместо шаблона, Jackson превращает объекты в JSON. Обратный список игнорируется, чтобы не было бесконечного обхода. ID берётся из URL; отсутствующая книга возвращает 404».

Сверьте изменения через `git diff`, запустите тесты, затем самостоятельно закоммитьте и отправьте код в нужный Classroom-репозиторий. Не включайте `target`, реальные `.env` или пароли. Отдельная ветка в этом исходнике не обязательна.

## 10. Полные файлы этого этапа

Ниже идут полные `Category.java`, `BookRestController.java` и тест, не фрагменты. Остальные классы, HTML, конфигурация и тесты находятся целиком в [общем листинге](../../../../../../stages/02-bookstore-rest/SOURCES.md).

<!-- BEGIN FULL SOURCES -->

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
