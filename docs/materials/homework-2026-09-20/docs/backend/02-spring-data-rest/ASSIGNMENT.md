# Chapter 5.2 - Bookstore: Spring Data REST

## Исходное задание (текст со скриншота)

Источник: [скриншот Chapter 5](../../../sources/rest/Screenshot 2026-09-18 at 14.09.07.png).

> 2. Bookstore: Spring Data REST
>
> Use your own bookstore application from the GitHub.
>
> Enable Spring Data REST by adding the dependency and the configuration to application.properties
>
> Test different RESTful services (searching, adding…)

## Требуется по-русски

1. Продолжить тот же собственный Bookstore.
2. Подключить зависимость `spring-boot-starter-data-rest`.
3. Настроить базовый путь API в `application.properties`.
4. Проверить сервисы: получение данных, поиск, добавление; в комплекте также показаны изменение и удаление.

Слайды 13-21 PDF объясняют автоматический экспорт репозиториев, HAL, `curl`, запросы к связанным сущностям и поиск.
Точные URL книг в условии не зафиксированы: в решении используем `/api/books` и `/api/categories`.

## Обязательное и дополнительное

- Spring Security и развёртывание сервера не входят в этот этап.
- Метод `findByTitleContainingIgnoreCase` - выбранный способ показать требуемый поиск, а не дословное имя из задания.
- В тестовом учебном API разрешены операции изменения данных, поэтому сервер привязан к `127.0.0.1`. Не выставляйте его в интернет без отдельной защиты.
- Это второе задание, но не вторая папка Bookstore: код 5.1 и 5.2 находится в одном продолжении проекта.

[Пошаговый разбор](MANUAL.md) → [полный проект](../../../../../../stages/02-bookstore-rest/docs/PROJECT-README.md).
