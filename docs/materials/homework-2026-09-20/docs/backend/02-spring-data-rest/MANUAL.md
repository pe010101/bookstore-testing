# Chapter 5.2 - Spring Data REST: от зависимости до CRUD

## 1. Результат и маршрут

[Условие](ASSIGNMENT.md): включить Spring Data REST в прежнем Bookstore и проверить сервисы.

Маршрут: завершить 5.1 → добавить starter → выбрать `/api` → экспортировать репозитории и поиск → проверить GET/POST/PUT/PATCH/DELETE → сохранить свою работу.

Оба задания уже объединены в [готовой копии Bookstore](../../../../../../stages/02-bookstore-rest/docs/PROJECT-README.md).
Если используете её, файлы повторно менять не нужно: можно сразу запускать и разбирать код.

## 2. Добавить зависимость и конфигурацию

В своём `pom.xml` добавьте `spring-boot-starter-data-rest` внутри существующего `<dependencies>`, не создавайте второй блок и не удаляйте JPA/Web/Thymeleaf/H2.
Не указывайте отдельную версию starter: её согласует Spring Boot parent.

В `src/main/resources/application.properties` добавьте `spring.data.rest.base-path=/api`.
Это отделяет автоматический API от нашего `/books` и HTML-страниц.
В учебной копии также задано `server.address=127.0.0.1`, потому что API допускает изменения без авторизации.

Полные итоговые файлы помещены в конце руководства.

## 3. Репозитории

`BookRepository` уже наследует `CrudRepository<Book, Long>`: Spring Data REST может использовать `findAll`, `findById`, `save`, `deleteById` для HTTP-операций.
`@RepositoryRestResource(path = "books", collectionResourceRel = "books")` фиксирует путь и имя коллекции; второй репозиторий аналогично экспортирует `categories`.

Добавленный `findByTitleContainingIgnoreCase` означает поиск книг, где название содержит введённый текст без учёта регистра.
`@Param("title")` привязывает параметр URL к аргументу, а `@RestResource(path = "by-title")` задаёт удобный URL поиска.
SQL вручную не пишем: запрос выводится из имени метода.

## 4. Запуск на Mac и Windows

Терминал открываем в `projects/backend/bookstore-rest`, где находятся `pom.xml` и `mvnw`.

```bash
bash ./mvnw test
bash ./mvnw spring-boot:run
```

Windows: `.\mvnw.cmd test`, затем `.\mvnw.cmd spring-boot:run`.
Java 17/21, отдельный Maven не нужен. Если 8080 занят:

```bash
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Установите удобную переменную в ДРУГОМ окне терминала Mac (сервер должен продолжать работать):

```bash
BOOKSTORE_BASE=http://localhost:8080
```

Если выбрали иной порт, исправьте значение. Это примеры для локальной учебной H2-копии, не для чужого сервера или рабочей базы.

## 5. Чтение и отличие от обычного JSON

```bash
curl -i "$BOOKSTORE_BASE/api"
curl -i "$BOOKSTORE_BASE/api/books"
curl -i "$BOOKSTORE_BASE/api/categories"
curl -i "$BOOKSTORE_BASE/books"
```

| Адрес | Что возвращает |
|---|---|
| `/books` | Простой JSON-массив нашего REST-контроллера |
| `/book/{id}` | Один обычный объект Book |
| `/api` | Ссылки на экспортированные репозитории |
| `/api/books` | HAL: `_embedded.books` и `_links` |
| `/api/books/{id}` | Одна книга и ссылки на связанные ресурсы |

В HAL идентификатор обычно содержится в `_links.self.href`; отсутствие отдельного поля `id` само по себе не ошибка.
Вместо вложенной категории автоматический API может показывать ссылку на неё. Следуйте `_links.category.href`.

## 6. Поиск

```bash
curl -i --get "$BOOKSTORE_BASE/api/books/search/by-title" \
  --data-urlencode 'title=spring'
```

`--data-urlencode` кодирует пробелы и специальные символы. В выдаче ожидается `Spring in Action`, если исходные демонстрационные данные ещё не менялись.
Несовпадающая строка означает отсутствие результатов, а не ошибку сервера.

## 7. Создать книгу

Сначала посмотрите `/api/categories` и скопируйте `self.href` одной существующей категории.
В свежей H2 `Programming` получает ID 1, но в вашем проекте это может быть другой номер.
Следующие запросы выполняйте только со своей выбранной категорией:

```bash
BOOKSTORE_CATEGORY_URL="$BOOKSTORE_BASE/api/categories/1"
curl -i -X POST "$BOOKSTORE_BASE/api/books" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/hal+json' \
  -d "{\"title\":\"REST practice\",\"author\":\"Student\",\"isbn\":\"practice-001\",\"publicationYear\":2026,\"category\":\"$BOOKSTORE_CATEGORY_URL\"}"
```

Ожидается `201 Created`. В заголовке `Location` будет адрес СОЗДАННОЙ книги. Скопируйте именно его:

```bash
BOOKSTORE_BOOK_URL=http://localhost:8080/api/books/4
curl -i "$BOOKSTORE_BOOK_URL"
```

Число `4` - пример для свежей демобазы, не универсальный ID. Для связи `category` передаётся URI ресурса, а не просто число и не объект `{ "id": 1 }`.

## 8. Изменить и удалить созданную учебную книгу

Частичное изменение (`PATCH`) - передаём только меняемое поле:

```bash
curl -i -X PATCH "$BOOKSTORE_BOOK_URL" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/hal+json' \
  -d '{"title":"REST practice updated"}'
```

Полная замена (`PUT`) - перечисляем все нужные поля:

```bash
curl -i -X PUT "$BOOKSTORE_BOOK_URL" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/hal+json' \
  -d "{\"title\":\"REST practice replaced\",\"author\":\"Student\",\"isbn\":\"practice-001\",\"publicationYear\":2025,\"category\":\"$BOOKSTORE_CATEGORY_URL\"}"
```

Удалить только созданную для проверки книгу:

```bash
curl -i -X DELETE "$BOOKSTORE_BOOK_URL"
curl -i "$BOOKSTORE_BOOK_URL"
```

Удаление возвращает `200 OK` с представлением или `204 No Content`, затем повторный GET возвращает `404 Not Found`. Выбор тела зависит в том числе от `Accept`: обычные curl/fetch посылают `Accept: */*`, поэтому в нашем живом запросе DELETE вернул `200`. Оба варианта означают успех. Ничего не удаляйте по неизвестному ID.
При PUT/PATCH с `Accept: application/hal+json` здесь ожидается `200` и тело. Без заголовка допустим `204`: отсутствие тела не означает провал операции.

## 9. Что использовано и зачем

| Элемент | Роль |
|---|---|
| `spring-boot-starter-data-rest` | Подключает экспорт репозиториев как HTTP API |
| `spring.data.rest.base-path` | Выделяет пространство адресов `/api` |
| `CrudRepository` | Доступ к сущностям и CRUD-операциям |
| `@RepositoryRestResource` | Явные имена путей и коллекций |
| `@RestResource` | Короткий адрес конкретного поиска |
| `@Param` | Имя аргумента поиска в URL |
| HAL `_embedded` | Коллекция объектов в ответе |
| HAL `_links` | Ссылки для перехода к объектам и их связям |
| HTTP 201 / 204 / 404 | Создано / успешно без тела / не найдено |
| `MockMvc` | Проверка реального Spring MVC-стека без ручного браузера |
| `@Transactional` в тестах | Откат тестовых изменений после каждого теста |

Для этого API контроллер вручную не пишется: его предоставляет библиотека. Ручной `BookRestController` из 5.1 остаётся отдельным учебным примером.

## 10. Проверка перед сдачей

1. `bash ./mvnw test` завершается успешно.
2. `/booklist`, `/books` и `/book/{id}` не сломаны после добавления Data REST.
3. `/api` показывает ссылки, `/api/books` показывает книги.
4. Поиск с `title=SPRING` находит книгу без учёта регистра.
5. Новая книга действительно появляется в обычном `/books` и HTML-таблице.
6. Изменение видно при повторном GET, удаление приводит к 404.
7. В репозитории нет секретов, `.env`, `target` и локальных данных.

Если нужно показать историю по этапам, сначала сохраните 5.1 отдельным коммитом, затем 5.2. Требования создавать ветки здесь нет.
Отправку в GitHub/Moodle выполняет ученик; этот комплект ничего туда не отправляет.

## 11. Связь с материалами преподавателя

PDF `SP4-REST.pdf` содержит 21 слайд: 2-12 - ручной REST, 13-21 - Spring Data REST. Слайд 12 иллюстрирует идею поиска, но его возвращаемый тип нужно согласовать с современным `Optional`.
В [StudentListRest](https://github.com/Haaga-Helia-SOF003AS3A/backend-programming/tree/master/StudentListRest) показывается аналогичный API для студентов; у нас предметная область - книги. [Конфигурация примера](https://github.com/Haaga-Helia-SOF003AS3A/backend-programming/blob/master/StudentListRest/src/main/resources/application.properties) также использует `/api`.
Поведение кодов ответа и HAL описано в [документации Spring Data REST](https://docs.spring.io/spring-data/rest/reference/repository-resources.html).

## 12. Полные файлы этого этапа

Ниже помещены полные итоговые `pom.xml`, свойства, оба репозитория и тест API. Все остальные Java-классы и HTML без сокращений есть в [общем листинге](../../../../../../stages/02-bookstore-rest/SOURCES.md).

<!-- BEGIN FULL SOURCES -->

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

### src/main/java/com/example/bookstore/CategoryRepository.java

```java
package com.example.bookstore;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories")
public interface CategoryRepository extends CrudRepository<Category, Long> {
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
