# Chapter 5.1 - Bookstore: REST

## Исходное задание (текст со скриншота)

Источник: [скриншот Chapter 5](../../../sources/rest/Screenshot 2026-09-18 at 14.09.07.png).
Ниже сохранены формулировки задания, в том числе исходные грамматические особенности.

> Chapter 5
>
> 1. Bookstore: REST
>
> Use your own bookstore application from the GitHub.
>
> Check StudentListREST example from the moodle
>
> a.) Create REST service that return all books (JSON)
> - a. Create a method to controller
> - b. Ignore one-to-many link from JSON
>
> b.) Create REST service that return one book by id (JSON)
> - a. Create a method to controller
> - b. Use path variable to get book id
>
> Push all changes to GitHub

## Требуется по-русски

1. Продолжить собственный Bookstore из предыдущих глав, а не делать новое приложение StudentList.
2. Создать метод контроллера, возвращающий все книги как JSON.
3. Исключить обратную связь `Category.books` из JSON: иначе получится цикл «книга - категория - книги».
4. Создать метод, возвращающий одну книгу по идентификатору.
5. Получать идентификатор из пути URL с помощью `@PathVariable`.
6. После самостоятельной проверки отправить изменения в свой учебный GitHub-репозиторий.

## Где проходит граница задания

- `404` для отсутствующей книги и автоматические тесты - полезные дополнения решения, не отдельные пункты исходника.
- CRUD через Spring Data REST относится к следующему заданию Chapter 5.2.
- MySQL, авторизация и публикация сервера в интернет для этого задания не требуются.
- `SP4-REST.pdf` - 21 слайд теории и примеров, а не 21 отдельное упражнение.
- Требование «Push» описывает действия ученика при сдаче. Комплект ничего не публикует автоматически.

## Маршрут

[Пошаговый разбор](MANUAL.md) → [полный проект](../../../../../../stages/02-bookstore-rest/docs/PROJECT-README.md).
