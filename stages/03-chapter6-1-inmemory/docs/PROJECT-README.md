# Bookstore Chapter 6.1 — вход и роли

Самостоятельный контрольный срез Bookstore с формой login, POST logout, именем пользователя, ADMIN-only удалением и защищёнными manual REST / Data REST маршрутами. Разобранное исходное задание: [ASSIGNMENT.md](ASSIGNMENT.md). [Полная инструкция и код](MANUAL.md).

## Запуск на Mac

Распаковать архив `bookstore-6-1.zip` в `<локальный-путь>`. Папка проекта — `bookstore-6-1`. Java 17 или новее; Maven установлять отдельно не требуется, Wrapper входит в архив.

```bash
cd <папка-этого-репозитория>
bash ./mvnw clean verify
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

После `Started BookstoreApplication` открыть `http://127.0.0.1:8082/login`. Порт примерный: выбрать свободный. Терминал остаётся занят до Control+C. H2 в памяти, установок MySQL не нужно; данные сбрасываются при остановке.

Учебные аккаунты: `user / User123!`, `admin / Admin123!`. Только локальная демонстрация. Bootstrap включён локально. Архив не содержит Git-привязки, build-файлов и секретов.

С общим запускателем из HW.Group-2: `bash ./build-and-run.sh --project "bookstore-6-1"`. Переносить изменения в свой `bookstore-data-rest` по инструкции, не заменять неизвестную рабочую копию без резервной копии и не совмещать оба SecurityConfig.

Windows: `mvnw.cmd clean verify` / `mvnw.cmd spring-boot:run`. Mac, MySQL и реальный браузер ученицы проверяются отдельно.
