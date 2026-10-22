# Bookstore Chapter 6.2 — пользователи БД

Самостоятельный контрольный срез 6.2 после 6.1. Пользовательские записи в `app_users`, username уникален; PasswordEncoder BCrypt и DatabaseUserDetailsService обеспечивают настоящий вход через БД. Формы login/logout, имя пользователя, роли и CSRF сохранены. Пользовательский репозиторий не экспортируется через Data REST, пароли не сериализуются.

[Исходное задание](ASSIGNMENT.md) · [Полный разбор со всеми изменёнными файлами](MANUAL.md)

## Запуск на Mac

Архив `bookstore-6-2.zip` распаковать в `<локальный-путь>`, получится папка `bookstore-6-2`. Java 17+, Maven Wrapper включён.

```bash
cd <папка-этого-репозитория>
bash ./mvnw clean verify
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

После сообщения Started открыть `http://127.0.0.1:8083/login`. Порт выбрать свободный. Приложение не зависает: терминал остаётся занят, пока работает сервер. Control+C останавливает.

По умолчанию профиль dev/H2 в памяти создаёт `user / User123!`, `admin / Admin123!`, хешируя пароли перед сохранением. Это локальные учебные аккаунты, не пароль MySQL и не внешнее развёртывание. H2 сбрасывается при остановке. Для сознательной локальной проверки MySQL+seed включаются профили `mysql,dev` с переменными из application-mysql.properties; внешняя БД не обязательна для 6.2.

Общий запускатель из HW.Group-2: `bash ./build-and-run.sh --project "bookstore-6-2"`.

**Свой Bookstore продолжать в `bookstore-data-rest`**, перенося перечисленные в CHANGED-FROM-PART-I.txt файлы по мануалу. Не писать поверх неизвестных пользовательских правок и не соединять два среза как один пакет Java.

Запуск тестов `mvnw.cmd clean verify` в Windows / `bash ./mvnw clean verify` в macOS. Проверка duplicate username намеренно может печатать SQL ERROR, итог Maven должен показывать 0 failures/errors. Архив без .git/target/локальных секретов.
