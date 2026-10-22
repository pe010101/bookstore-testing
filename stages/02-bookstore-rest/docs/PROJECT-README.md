# Bookstore - Chapter 5.1 и 5.2

Готовая отдельная копия нашего Bookstore из Chapter 3-4, расширенная двумя REST-заданиями.
Старый проект в клиентской папке не изменён.

| Этап | Условие | Пошаговый разбор |
|---|---|---|
| 5.1 Ручной REST-контроллер | [MD](../../../docs/materials/homework-2026-09-20/docs/backend/01-bookstore-rest/ASSIGNMENT.md) | [MD](../../../docs/materials/homework-2026-09-20/docs/backend/01-bookstore-rest/MANUAL.md) |
| 5.2 Spring Data REST | [MD](../../../docs/materials/homework-2026-09-20/docs/backend/02-spring-data-rest/ASSIGNMENT.md) | [MD](../../../docs/materials/homework-2026-09-20/docs/backend/02-spring-data-rest/MANUAL.md) |

## Запуск на Mac

Откройте терминал именно в этой папке: рядом должны быть `pom.xml` и `mvnw`.

```bash
java -version
bash ./mvnw test
bash ./mvnw spring-boot:run
```

Требуется JDK 17 или 21. Maven отдельно устанавливать не обязательно: Wrapper скачает закреплённую версию 3.9.16 при первом запуске. Требуется интернет для первой загрузки зависимостей.

Если 8080 занят:

```bash
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

В этой копии `mvnw.cmd` дополнительно обрабатывает обычную папку `.m2` без символической ссылки: исходный Wrapper обращался к пустому `Target[0]`.

## Что открыть

| Адрес | Результат |
|---|---|
| http://localhost:8080/booklist | Старая HTML-таблица книг и Bootstrap |
| http://localhost:8080/books | Простой JSON-массив всех книг |
| http://localhost:8080/book/1 | Одна книга; ID уточняйте в `/books` |
| http://localhost:8080/api | Ссылки Spring Data REST |
| http://localhost:8080/api/books | Книги в HAL-формате |
| http://localhost:8080/api/books/search/by-title?title=spring | Поиск по части названия |

H2 работает в памяти: при остановке и новом запуске изменения сбрасываются, загружаются демонстрационные книги.
MySQL для Chapter 5 не нужен. Старый файл `application-mysql.properties` сохранён, но без реальных паролей и без активирования профиля.
Если на компьютере ранее экспортирован `SPRING_PROFILES_ACTIVE=mysql`, уберите его из текущей сессии: `unset SPRING_PROFILES_ACTIVE` (Mac).

API учебный, без авторизации и без полной бизнес-валидации. Доступ по умолчанию только с данного компьютера, `127.0.0.1`.

## Версии и исходники

- Spring Boot 3.5.16 оставлен из предыдущего комплекта; это не заявление, что он новейший.
- Java-код компилируется с целевой версией 17.
- Пример преподавателя в репозитории на момент разбора использует Spring Boot 4.1.1. Его `pom.xml` нельзя бездумно смешивать с нашим: названия starter-ов и тестовые пакеты могут отличаться.
- [Полные листинги приложения и тестов](../SOURCES.md). Библиотечные Bootstrap и Maven Wrapper лежат в проекте целиком, но не дублируются многими страницами в учебном листинге.
- Результаты проверки комплекта (исходная общая ссылка вне этого пакета).
