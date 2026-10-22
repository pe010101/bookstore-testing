# Bookstore 6.2 — полный разбор: пользователи в базе и BCrypt

[Исходное задание](../assignments/bookstore-security-2.md) · Запуск на Mac (исходная общая ссылка вне этого пакета)

## 1. Задание, результат и выбранный проект

Требуется продолжить свой Bookstore после 6.1: создать пользователя с уникальным username, password, email, role; CRUD-репозиторий; UserDetailsService; добавить пользователей с BCrypt-хешами и заменить in-memory аутентификацию на чтение БД. Вход, выход и ограничения USER/ADMIN сохраняются. Регистрация, JWT и восстановление пароля не требуются.

**Продолжать в той же папке `bookstore-data-rest`, где уже работает 6.1**, а не в старых `03-spring-bookstore/bookstore` или `bookstore-rest`. Номера меню могут меняться — выбирать по пути:

```text
<локальный-путь>
```

Готовый самостоятельный контрольный проект 6.2 в архиве расположен в `bookstore-6-2`. Его можно запустить отдельно для сверки, но не переносить поверх собственного проекта без резервной копии. Контрольный срез 6.1 сохраняет предыдущее состояние — в одном работающем приложении не должны одновременно существовать оба SecurityConfig.

Сначала остановить свой предыдущий запуск Control+C. Сохранить файлы 6.1 коммитом либо отдельным архивом **вне HW.Group-2**, чтобы общий запускатель не находил резервную копию как новый проект.

## 2. Маршрут: форма → Security → сервис → репозиторий → БД

Форма отправляет username/password. `DaoAuthenticationProvider` вызывает `DatabaseUserDetailsService`, получает сохранённый хеш и проверяет исходный пароль через BCrypt. После успешного входа создаётся HTTP-сессия; фильтры отдельно проверяют роль при удалении.

Пароль из формы не сравнивается с хешем через `equals`: его проверяет `PasswordEncoder.matches`. Хеш нельзя повторно кодировать — это испортит проверку. В БД хранится `USER` или `ADMIN`; вызов `roles(...)` сам добавляет префикс `ROLE_`.

## 3. Реализация по файлам — вставлять содержимое целиком

### Шаг 1. pom.xml

В готовой основе уже есть Security, Thymeleaf Extras и security-test из 6.1. Зависимости сохраняются; `artifactId` среза помогает отличить сборку 6.2 от 6.1. Если свой проект имеет другое имя пакета или согласованную версию Boot, адаптировать пакет и проверить API своей версии, не обновлять его слепо по этому образцу.

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
    <artifactId>bookstore-security-database</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>bookstore-security-database</name>
    <description>Bookstore Chapter 6.2: database users and BCrypt</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity6</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
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

### Шаг 2. AppUser.java — сущность пользователя

Путь `src/main/java/com/example/bookstore/AppUser.java`. Имя AppUser не конфликтует с `User` из Spring Security. Таблица `app_users` избегает SQL-ключевого слова `user`. Уникальность имени обеспечивается БД, а не только проверкой интерфейса. `@JsonIgnore` закрывает пароль при сериализации, даже если объект случайно попадёт в ответ.

```java
package com.example.bookstore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String role;

    protected AppUser() {
    }

    public AppUser(String username, String password, String email, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
```

### Шаг 3. AppUserRepository.java

Путь `src/main/java/com/example/bookstore/AppUserRepository.java`. Здесь используется `JpaRepository`, включающий возможности CRUD-репозитория и JPA; `findByUsername` возвращает Optional. `exported=false` обязателен в нашей основе со Spring Data REST: пользовательский репозиторий не превращается в API изменения паролей и ролей.

```java
package com.example.bookstore;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

// Authentication data must never become a generated public CRUD API.
@RepositoryRestResource(exported = false)
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
```

### Шаг 4. DatabaseUserDetailsService.java

Путь `src/main/java/com/example/bookstore/DatabaseUserDetailsService.java`. Сервис загружает запись, но не проверяет пароль вручную. Не создавать рядом второй UserDetailsService из альтернативной инструкции. В проекте должны остаться один этот сервис и один PasswordEncoder.

```java
package com.example.bookstore;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(appUser.getRole())
                .build();
    }
}
```

### Шаг 5. DemoUserInitializer.java — только учебный профиль dev

Путь `src/main/java/com/example/bookstore/DemoUserInitializer.java`. `CommandLineRunner` заполняет БД при старте. Поиск имени перед вставкой делает повторный запуск seed идемпотентным: существующий пароль не меняется и второй пользователь с тем же именем не создаётся.

```java
package com.example.bookstore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DemoUserInitializer {
    @Bean
    CommandLineRunner initialUsers(AppUserRepository users, PasswordEncoder encoder) {
        return arguments -> {
            addIfAbsent(users, encoder, "user", "User123!", "user@example.test", "USER");
            addIfAbsent(users, encoder, "admin", "Admin123!", "admin@example.test", "ADMIN");
        };
    }

    private void addIfAbsent(AppUserRepository users, PasswordEncoder encoder,
            String username, String plainPassword, String email, String role) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(new AppUser(username, encoder.encode(plainPassword), email, role));
        }
    }
}
```

| Учебный логин | Пароль приложения | Роль |
|---|---|---|
| `user` | `User123!` | USER |
| `admin` | `Admin123!` | ADMIN |

Это **не логины MySQL**. Только локальная демонстрация, не внешнее развёртывание с реальными пользователями. Часть 6.2 не добавляет форму самостоятельной регистрации.

### Шаг 6. SecurityConfig.java — заменить вариант из 6.1

Путь `src/main/java/com/example/bookstore/SecurityConfig.java`. **Удалить прежний bean с InMemoryUserDetailsManager**, заменив файл этим вариантом. `DaoAuthenticationProvider` явно связывает DB-сервис и BCrypt. Если оставить старый bean, успешный вход перестанет доказывать чтение БД.

```java
package com.example.bookstore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .requestMatchers("/delete/**").hasRole("ADMIN")
                // Protect every generated DELETE route, including association endpoints.
                .requestMatchers(HttpMethod.DELETE, "/api", "/api/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                    .defaultSuccessUrl("/booklist", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll())
            .httpBasic(Customizer.withDefaults());
        // CSRF stays enabled: all POST, PUT, PATCH and DELETE requests require a token.
        return http.build();
    }
}
```

### Шаг 7. application.properties

Путь `src/main/resources/application.properties`. По умолчанию активен `dev` и используется H2 в памяти: дополнительные MySQL/Maven установки для первой проверки не нужны. H2 — полноценная БД, но записи исчезают при остановке, а seed восстанавливает учебные аккаунты.

```properties
spring.application.name=bookstore
# Local development only: activates the idempotent demo-user initializer.
spring.profiles.default=dev
spring.datasource.url=jdbc:h2:mem:bookstore;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=false

# Keep generated HAL endpoints separate from our manual REST controller.
spring.data.rest.base-path=/api
# Local classroom credentials must never be used for an Internet deployment.
server.address=127.0.0.1
```

`create-drop` и встроенные пароли — только учебный локальный профиль. Если активировать только `mysql`, профиль dev уже не действует: учебные пользователи не появятся автоматически. Для сознательного локального теста с внешней БД и seed нужны оба профиля `mysql,dev`. Не включать seed на внешнем сервере. Настройки MySQL и её пользователей — отдельный вопрос, не аутентификация веб-приложения.

### Шаг 8. BookController.java — сохраняем страницы и POST-удаление

Путь `src/main/java/com/example/bookstore/BookController.java`. GET `/login` отдаёт страницу. POST `/login` обрабатывает Spring Security — свой обработчик не нужен. GET удаления ничего не меняет; удаление выполняется POST-запросом с токеном.

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

    @GetMapping("/")
    public String home() {
        return "redirect:/booklist";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
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

    @PostMapping("/delete/{id}")
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

### Шаг 9. templates/login.html — сохраняем собственную форму

Путь `src/main/resources/templates/login.html`. Поля называются `username` и `password`; `th:action` обеспечивает CSRF в POST-форме. Сообщения при `?error` и `?logout` помогают различать неудачный вход и выход.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="https://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bookstore — Log in</title>
    <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
</head>
<body>
<main class="container py-4" style="max-width: 480px">
    <h1>Bookstore</h1>
    <h2 class="h4">Log in</h2>
    <p class="alert alert-danger" th:if="${param.error}">Invalid username or password.</p>
    <p class="alert alert-success" th:if="${param.logout}">You have logged out.</p>
    <form th:action="@{/login}" method="post">
        <div class="mb-3">
            <label for="username" class="form-label">Username</label>
            <input id="username" name="username" class="form-control"
                   autocomplete="username" required autofocus>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label">Password</label>
            <input id="password" name="password" type="password" class="form-control"
                   autocomplete="current-password" required>
        </div>
        <button type="submit" class="btn btn-primary">Log in</button>
    </form>
</main>
</body>
</html>
```

### Шаг 10. templates/booklist.html — имя, выход, ADMIN

Путь `src/main/resources/templates/booklist.html`. `sec:authentication="name"` показывает пользователя. Logout — POST, не обычная ссылка. Скрытие Delete у USER дополняет серверную проверку, но не заменяет её.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="https://www.thymeleaf.org"
      xmlns:sec="https://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bookstore</title>
    <link type="text/css" rel="stylesheet" href="/css/bootstrap.min.css" th:href="@{/css/bootstrap.min.css}">
</head>
<body>
<main class="container py-4">
    <h1 class="mb-4">Bookstore</h1>
    <div class="d-flex align-items-center gap-3 mb-3">
        <p class="mb-0">Signed in as <strong sec:authentication="name"></strong></p>
        <form th:action="@{/logout}" method="post">
            <button type="submit" class="btn btn-outline-secondary">Log out</button>
        </form>
    </div>
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
                <form class="d-inline" sec:authorize="hasRole('ADMIN')"
                      th:action="@{/delete/{id}(id=${book.id})}" method="post">
                    <button type="submit" class="btn btn-danger">Delete</button>
                </form>
                <a class="btn btn-outline-secondary" th:href="@{/edit/{id}(id=${book.id})}">Edit</a>
            </td>
        </tr>
        </tbody>
    </table>
</main>
</body>
</html>
```

## 4. Запуск на Mac — после переноса файлов

В терминале своего проекта:

```bash
cd <папка-этого-репозитория>
bash ./mvnw clean verify
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

Порт 8082 — пример свободного порта. После `Started BookstoreApplication` открыть `http://127.0.0.1:8082/login`. Терминал остаётся занят: это работающий сервер, а не зависание. Control+C остановит его. `spring-boot:run` не открывает браузер автоматически.

Для самостоятельного готового среза вместо этого перейти в `HW.Group-2/bookstore-6-2`. Общий запускатель:

```bash
cd <папка-этого-репозитория>
bash ./build-and-run.sh --project "bookstore-data-rest"
# Для отдельно распакованного контрольного среза:
bash ./build-and-run.sh --project "bookstore-6-2"
```

Открывать **порт из его итоговой таблицы**, а не старую вкладку 8080. Сборка `clean verify` убирает старые JAR, запускатель отказывается выбирать первый из нескольких и проверяет готовность с учётом авторизации. Bootstrap уже лежит локально в `src/main/resources/static/css/bootstrap.min.css`.

При запуске готового JAR имя — `bookstore-security-database-0.0.1-SNAPSHOT.jar`: проверить фактическое имя в target. Не запускать любой JAR по первой найденной букве и не копировать target со старой машины.

## 5. Что использовано и почему

| Конструкция / функция | Назначение и причина |
|---|---|
| `@Entity` / `@Table` | Связывают Java-объект и таблицу БД |
| `unique=true` | БД запрещает повторный username, включая параллельные вставки |
| `Optional<AppUser>` | Явно описывает «не найден» без случайного null |
| `loadUserByUsername` | Преобразует сущность в UserDetails без самодельного сравнения пароля |
| `roles(...)` | Добавляет ROLE_ к USER/ADMIN ровно один раз |
| `BCryptPasswordEncoder` | Солёный хеш, который нельзя обратить в исходный пароль |
| `encode` / `matches` | Первое сохраняет хеш, второе проверяет введённый пароль |
| `DaoAuthenticationProvider` | Связывает пользователей БД и проверку пароля |
| `@Profile("dev")` | Не включает известные demo-аккаунты в другом профиле автоматически |
| `CommandLineRunner` | Инициализация учебных пользователей при запуске |
| `exported=false` / `@JsonIgnore` | Не открывают CRUD учётных записей и парольные хеши |
| `SecurityFilterChain` | Проверка URL, ролей, form login, сессии и logout |
| CSRF-токен | Защищает меняющие запросы, не отключается ради устранения 403 |

## 6. Проверка от начала до конца

1. Анонимно `/login` доступен, `/booklist` закрыт. В браузере с HTML-запросом происходит переход на вход; HTTP-клиент может получить 401 — это работающая защита.
2. `user / User123!` входит, видит своё имя, не видит Delete, может выйти.
3. `admin / Admin123!` входит, видит Delete и может удалить книгу с CSRF.
4. Неверный пароль не даёт войти.
5. Повторный dev seed не создаёт дубликатов; сохранённые пароли BCrypt, не открытые строки.
6. Создать в тесте третьего пользователя **только в БД** и выполнить настоящий login: это доказывает DB-аутентификацию. Такой тест включён в DatabaseUsersTest.
7. Повторный username отвергается БД; пользовательский репозиторий не экспортируется через `/api`.
8. После logout защищённый адрес снова требует входа. Не оценивать выход по наличию старой страницы в кэше браузера.
9. Прежние книги, категории, HTML-формы, manual REST и HAL CRUD проходят адаптированные тесты.

Отрицательный тест duplicate username намеренно может печатать SQL ERROR. Смотрите итоговую сводку Maven: failures/errors должны быть 0. Mockito предупреждение об agents само по себе не означает провал тестов.

## 7. Частые ошибки и как отличить причину

- `/login` даёт 404 и нет Logout: проверить точный процесс/порт/JAR и наличие текущих templates; в старой копии мог запускаться JAR предыдущей главы.
- 401/403 в старых тестах: они не учитывают Security. Полные адаптированные тесты находятся ниже; CSRF не отключать.
- Логин работает только после возврата in-memory bean: это неверное завершение 6.2, DB-сервис не задействован.
- После выключения H2 исчезли записи: ожидаемо для `jdbc:h2:mem`, а не ошибка BCrypt. Для сохранения между запусками нужен отдельный persistent-профиль.
- PasswordEncoder.encode на уже сохранённом хеше ломает вход. Кодировать исходный пароль один раз при создании записи.
- `ROLE_ROLE_ADMIN`: хранить ADMIN в таблице, roles добавит префикс.
- Два UserDetailsService / два SecurityConfig в одном пакете: не совмещать альтернативные срезы.

## 8. Что сдавать и объяснять

Исходники своего Bookstore, README с запуском/профилями/учебными аккаунтами, тесты и изменения в учебном GitHub при самостоятельной сдаче. Не добавлять target, свою `.env`, пароли БД и другую локальную конфигурацию. Публикация и push этим комплектом автоматически не выполняются.

На защите показать последовательность форма → Security → DatabaseUserDetailsService → AppUserRepository → AppUser и объяснить различие «кто пользователь» / «какие операции ему разрешены». Полный комплект ниже включает все изменённые тесты: переносить их целиком в свой тестовый пакет, не создавать второй класс с тем же именем.

## Полное содержимое остальных рабочих файлов

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


## 9. Полное содержимое тестов

Если в выбранной папке уже есть тест с таким именем, заменить его, а не создавать второй одноимённый класс. Здесь сохранены проверки прежних возможностей; HTTP-тесты адаптированы к аутентификации и CSRF. Неизменённые проверки репозиториев тоже приведены целиком.

### src/test/java/com/example/bookstore/SecurityWorkflowTest.java

```java
package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityWorkflowTest {
    @Autowired private MockMvc mvc;
    @Autowired private BookRepository books;
    @Autowired private CategoryRepository categories;

    @Test
    void anonymousUsersCannotOpenAnyApplicationPageOrApi() throws Exception {
        for (String path : new String[]{"/", "/booklist", "/add", "/books", "/api/books"}) {
            mvc.perform(get(path).accept("text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Test
    void loginAndStaticCssRemainPublicAndFormContainsCsrfToken() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/css/bootstrap.min.css")).andExpect(status().isOk());
    }

    @Test
    void actualUserPasswordAuthenticatesAndThePageShowsUsernameWithoutDelete() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("user").password("User123!"))
                .andExpect(authenticated().withUsername("user").withRoles("USER"))
                .andExpect(redirectedUrl("/booklist")).andReturn();
        mvc.perform(get("/booklist").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">user</strong>")))
                .andExpect(content().string(not(containsString(">Delete</button>"))))
                .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @Test
    void actualAdminPasswordAuthenticatesAndThePageShowsDelete() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("admin").password("Admin123!"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN")).andReturn();
        mvc.perform(get("/booklist").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">admin</strong>")))
                .andExpect(content().string(containsString(">Delete</button>")));
    }

    @Test
    void wrongPasswordReturnsLoginError() throws Exception {
        mvc.perform(formLogin().user("user").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutWithCsrfInvalidatesTheAuthenticatedSession() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("user").password("User123!")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(post("/logout").session(session).accept("text/html").with(csrf()))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void userCannotDeleteAHtmlBookEvenWithAValidCsrfToken() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void adminCanDeleteAHtmlBookWithAValidCsrfToken() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/booklist"));
        assertThat(books.existsById(book.getId())).isFalse();
    }

    @Test
    void adminCannotDeleteWithoutCsrfAndGetNeverDeletes() throws Exception {
        Book book = newBook();
        mvc.perform(post("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/delete/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void userCannotDeleteThroughDataRestOrAssociationRoutesEvenWithCsrf() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId()).with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/books/{id}/category", book.getId())
                        .with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    @Test
    void adminCanDeleteThroughDataRestWithCsrf() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(books.existsById(book.getId())).isFalse();
    }

    @Test
    void apiDeletionAlsoRequiresCsrfForAdmin() throws Exception {
        Book book = newBook();
        mvc.perform(delete("/api/books/{id}", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(books.existsById(book.getId())).isTrue();
    }

    private Book newBook() {
        Category category = categories.findAll().iterator().next();
        Book book = new Book(null, "Security test book", "Test author", "security-test", 2026);
        book.setCategory(category);
        return books.save(book);
    }
}
```

### src/test/java/com/example/bookstore/DatabaseUsersTest.java

```java
package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DatabaseUsersTest {
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UserDetailsService detailsService;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private DemoUserInitializer initializer;

    @Test
    void demoUsersContainBcryptHashesAndSeparateRoles() {
        AppUser user = users.findByUsername("user").orElseThrow();
        AppUser admin = users.findByUsername("admin").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2").isNotEqualTo("User123!");
        assertThat(admin.getPassword()).startsWith("$2").isNotEqualTo("Admin123!");
        assertThat(encoder.matches("User123!", user.getPassword())).isTrue();
        assertThat(encoder.matches("Admin123!", admin.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(admin.getRole()).isEqualTo("ADMIN");
        assertThat(detailsService).isInstanceOf(DatabaseUserDetailsService.class)
                .isNotInstanceOf(InMemoryUserDetailsManager.class);
    }

    @Test
    void aNewDatabaseUserCanLogInThroughTheRealFormFilter() throws Exception {
        users.saveAndFlush(new AppUser("database-only", encoder.encode("Database123!"),
                "database@example.test", "USER"));
        mvc.perform(formLogin().user("database-only").password("Database123!"))
                .andExpect(authenticated().withUsername("database-only").withRoles("USER"))
                .andExpect(redirectedUrl("/booklist"));
    }

    @Test
    void absentDatabaseUserCannotLogIn() throws Exception {
        mvc.perform(formLogin().user("no-such-user").password("anything"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        assertThatThrownBy(() -> detailsService.loadUserByUsername("no-such-user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void usernameIsUniqueInTheDatabase() {
        users.saveAndFlush(new AppUser("unique-demo", encoder.encode("First123!"),
                "first@example.test", "USER"));
        assertThatThrownBy(() -> users.saveAndFlush(new AppUser("unique-demo",
                encoder.encode("Second123!"), "second@example.test", "USER")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void initializerIsIdempotentAndNeverRehashesAnExistingHash() throws Exception {
        long beforeCount = users.count();
        String beforeHash = users.findByUsername("user").orElseThrow().getPassword();
        CommandLineRunner runner = initializer.initialUsers(users, encoder);
        runner.run();
        runner.run();
        assertThat(users.count()).isEqualTo(beforeCount);
        assertThat(users.findByUsername("user").orElseThrow().getPassword()).isEqualTo(beforeHash);
    }

    @Test
    void authenticationRepositoryHasNoGeneratedRestEndpointOrRootLink() throws Exception {
        mvc.perform(get("/api").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.appUsers").doesNotExist())
                .andExpect(jsonPath("$._links.users").doesNotExist());
        mvc.perform(get("/api/appUsers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordIsNeverSerializedToJson() throws Exception {
        String json = mapper.writeValueAsString(users.findByUsername("user").orElseThrow());
        assertThat(json).doesNotContain("password", "$2", "User123!");
        assertThat(json).contains("\"username\":\"user\"");
    }
}
```

### src/test/java/com/example/bookstore/BookControllerTest.java

```java
package com.example.bookstore;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
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
    void editLinkAppearsAfterSafeDeleteButton() throws Exception {
        MvcResult result = mockMvc.perform(get("/booklist"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html.indexOf(">Delete</button>"))
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

        mockMvc.perform(post("/save").with(csrf())
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

        mockMvc.perform(post("/save").with(csrf())
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

        mockMvc.perform(post("/save/{id}", book.getId()).with(csrf())
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

        mockMvc.perform(post("/save/999999").with(csrf())
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

        mockMvc.perform(post("/delete/{id}", book.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/booklist"));

        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    @Test
    void unknownBookIdsReturnNotFound() throws Exception {
        mockMvc.perform(get("/edit/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/delete/999999").with(csrf()))
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

        mockMvc.perform(post("/save").with(csrf())
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
        mockMvc.perform(post("/save").with(csrf())
                        .param("title", "Rejected")
                        .param("author", "Nobody")
                        .param("isbn", "000")
                        .param("publicationYear", "2026")
                        .param("categoryId", "999999"))
                .andExpect(status().isNotFound());
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
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

### src/test/java/com/example/bookstore/DataRestApiTest.java

```java
package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
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
        mvc.perform(post("/api/books").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .accept("application/hal+json").content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("New book"));
        Book created = books.findByTitleContainingIgnoreCase("New book").get(0);
        assertThat(created.getCategory().getId()).isEqualTo(categoryId);
    }

    @Test
    void canPatchBook() throws Exception {
        mvc.perform(patch("/api/books/{id}", bookId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
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
        mvc.perform(put("/api/books/{id}", bookId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .accept("application/hal+json").content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Replacement"));
    }

    @Test
    void deletesOnlySelectedBook() throws Exception {
        mvc.perform(delete("/api/books/{id}", bookId).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(status().isNotFound());
        assertThat(categories.existsById(categoryId)).isTrue();
    }

    @Test
    void missingDataRestBookReturns404() throws Exception {
        mvc.perform(get("/api/books/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithBrowserAcceptHeaderCanReturn200() throws Exception {
        mvc.perform(delete("/api/books/{id}", bookId).with(csrf()).accept("*/*"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/books/{id}", bookId)).andExpect(status().isNotFound());
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
    void defaultProfileConfiguresH2SqlLoggingButDisablesUnprotectedConsole() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).contains("spring.datasource.url=jdbc:h2:mem:bookstore");
        assertThat(properties).contains("spring.jpa.show-sql=true");
        assertThat(properties).contains("spring.h2.console.enabled=false");
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
