# Spring Boot — Bookstore, главы 3–6 и Testing

Одна накопительная линия: CRUD → категории → REST → Data REST → Security 6.1 → пользователи в БД 6.2 → REST/JPA Testing. MySQL Optional отдельная ветка. Контрольные срезы сохранять, свои проекты ученицы не заменять. Testing зарегистрировано как следующее невыполненное задание.

Маршрут: прочитать условия → установить зависимости → запустить → пройти проверки → подготовить к сдаче.

## Условия

- [03-spring-bookstore-chapter-3.md](docs/materials/fullstack-homework-kit/docs/assignments/03-spring-bookstore-chapter-3.md)
- [04-spring-bookstore-chapter-4.md](docs/materials/fullstack-homework-kit/docs/assignments/04-spring-bookstore-chapter-4.md)
- [09-spring-bookstore-mysql.md](docs/materials/fullstack-homework-kit/docs/assignments/09-spring-bookstore-mysql.md)
- [ASSIGNMENT.md](docs/materials/homework-2026-09-20/docs/backend/01-bookstore-rest/ASSIGNMENT.md)
- [ASSIGNMENT.md](docs/materials/homework-2026-09-20/docs/backend/02-spring-data-rest/ASSIGNMENT.md)
- [bookstore-security-1.md](docs/materials/homework-2026-10-03/assignments/bookstore-security-1.md)
- [bookstore-security-2.md](docs/materials/homework-2026-10-03/assignments/bookstore-security-2.md)
- [OPEN-QUESTIONS.md](docs/materials/homework-2026-10-13/OPEN-QUESTIONS.md)

## Запуск из чистой копии

Нужен JDK 17 или 21 и интернет для первой загрузки Maven Wrapper/зависимостей. Откройте терминал в корне репозитория рядом с pom.xml.

macOS/Linux:

~~~bash
java -version
bash ./mvnw spring-boot:run
~~~

Windows PowerShell:

~~~powershell
java -version
.\mvnw.cmd spring-boot:run
~~~

Остановка: Ctrl+C. Адрес и настройки приведены в исходном руководстве ниже.

## Проверка

~~~bash
bash ./mvnw clean verify
~~~

Windows PowerShell: .\mvnw.cmd clean verify.

В этой упаковке установки и проверки не выполнялись. Сборка, браузер, macOS и реальное устройство имеют отдельный статус; наличие файлов не означает готовность к сдаче.

## Разбор и материалы

- [Исходное руководство приложения](docs/PROJECT-README.md)
- [03-spring-bookstore-chapter-3.md](docs/materials/fullstack-homework-kit/docs/tasks/03-spring-bookstore-chapter-3.md)
- [04-spring-bookstore-chapter-4.md](docs/materials/fullstack-homework-kit/docs/tasks/04-spring-bookstore-chapter-4.md)
- [09-spring-bookstore-mysql.md](docs/materials/fullstack-homework-kit/docs/tasks/09-spring-bookstore-mysql.md)
- [MANUAL.md](docs/materials/homework-2026-09-20/docs/backend/01-bookstore-rest/MANUAL.md)
- [MANUAL.md](docs/materials/homework-2026-09-20/docs/backend/02-spring-data-rest/MANUAL.md)

## Предыдущие самостоятельные срезы

- [bookstore](stages/01-bookstore/README.md)
- [bookstore-rest](stages/02-bookstore-rest/README.md)
- [chapter6-1-inmemory](stages/03-chapter6-1-inmemory/README.md)

Каждый срез запускается из своей папки. Главная версия находится в корне.

## Зависимости и открытые проверки

- Основная версия: Chapter 6.2, dev/H2. MySQL — необязательная ветка; Bookstore Testing остаётся невыполненным заданием.

## Объективные пробелы

- Следующее невыполненное задание по coordination registry; упаковка не реализует его.


## Единый запуск

```bash
bash ./build-and-run.sh --list
bash ./build-and-run.sh
```

[Запуск и перенос в свой GitHub](docs/CLIENT-HANDOFF.md).

