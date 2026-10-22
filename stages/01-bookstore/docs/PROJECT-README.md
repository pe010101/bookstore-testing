# Bookstore — Spring Boot, Chapter 3 и 4

Небольшое учебное приложение для списка книг. По умолчанию оно использует
in-memory базу H2: для запуска и всех автоматических тестов не нужны MySQL,
сеть или секреты.

## Запуск

На macOS сначала сделайте Maven Wrapper исполняемым, затем запустите тесты и
приложение:

```bash
chmod +x mvnw
./mvnw test
./mvnw spring-boot:run
```

На Windows используются соответствующие команды `mvnw.cmd`.

## Маршруты Chapter 3/4

| Маршрут | Назначение |
|---|---|
| `GET /booklist` | список книг и их категорий |
| `GET /add` | форма `addbook.html` для новой книги |
| `POST /save` | создаёт новую книгу и перенаправляет на список |
| `GET /edit/{id}` | форма редактирования существующей книги |
| `POST /save/{id}` | сохраняет изменения существующей книги |
| `GET /delete/{id}` | удаляет книгу и перенаправляет на список |
| `GET /h2-console` | веб-консоль H2 во время обычного запуска |

После `./mvnw spring-boot:run` откройте `http://localhost:8080/booklist`.
Проверьте стартовые книги и категории, выпадающий список Category, создание,
редактирование и удаление. После каждой отправки формы применяется PRG
(Post/Redirect/Get): браузер получает перенаправление на `/booklist`, поэтому
обновление страницы не повторяет POST-запрос.

Консоль H2 доступна по адресу `http://localhost:8080/h2-console`. Укажите JDBC
URL `jdbc:h2:mem:bookstore`, пользователя `sa` и оставьте пароль пустым.

В модели одна Category содержит много Book через
`@OneToMany(mappedBy = "category")`; Book хранит внешний ключ и является
владеющей стороной через `@ManyToOne`. Выбор категории в форме передаёт её ID,
а контроллер загружает управляемую Category перед сохранением книги.
Добавление и редактирование используют отдельный `BookForm` без id. При edit
контроллер загружает существующую entity по path variable и копирует только
разрешённые поля, поэтому подставленный параметр id не перезаписывает другую книгу.
Initializer заполняет только пустую базу и не дублирует записи после перезапуска.

## Optional: MySQL/XAMPP на macOS

Этот профиль необязателен и не входит в обычные H2-тесты. Запустите MySQL в
XAMPP вручную и создайте базу:

```sql
CREATE DATABASE test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

В том же окне терминала задайте переменные окружения. Пароль не записывается ни
в файл, ни в историю команд:

```zsh
export BOOKSTORE_MYSQL_URL='jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
export BOOKSTORE_MYSQL_USERNAME='root'
read -s "BOOKSTORE_DB_PASSWORD?MySQL password: "; echo
export BOOKSTORE_DB_PASSWORD
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

`Control+C` останавливает приложение, а закрытие терминала очищает
экспортированный пароль. Нормальные `./mvnw test` и `./mvnw package` всегда
используют только H2 и не требуют работающего MySQL.

Если порт 8080 занят, остановите другое приложение или задайте другой порт:
`./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"`.
Если MySQL не запускается, сначала запустите его в XAMPP. Если профиль сообщает
о незаданной переменной, повторите три команды `export` выше в том же терминале.

## Результат локальной проверки

| Проверка | Команда или сценарий | Фактический результат | Дата |
|---|---|---|---|
| Unit, JPA и MockMvc | `mvnw.cmd -q clean verify` | exit code 0; 21 tests, 0 failures, 0 errors | 2026-08-27 |
| Package | `mvnw.cmd -q package` | exit code 0 | 2026-08-26 |
| H2 smoke test | Browser: `/booklist`, add/edit/delete, `/h2-console` | 3 seeded books/categories shown; Databases selection persisted; test book edited and removed; H2 Console login opened | 2026-08-26 |
| MySQL optional | Not part of offline gate | Not part of offline gate | 2026-08-26 |
