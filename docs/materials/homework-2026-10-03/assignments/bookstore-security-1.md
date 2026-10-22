# Chapter 6.1 — Bookstore: Securing the web application

Источник: `sources/SP5-Exercise-Security.pdf`, страница 1, пункт 1.
Имя файла начинается с SP5, но заголовок внутри — **Chapter 6**. Это не повтор Chapter 5 REST.

## Исходное задание

Use your own bookstore application from the GitHub.

Check SecureStudentList example from the moodle.

a. Add Spring Security to your project by adding dependency.

b. Add Spring Security configuration class which will secure all URLs (requires authentication in all URLs) and add two in-memory users with roles USER and ADMIN.

c. Create the login page and add login method to controller.

d. Add logout functionality to your booklist page.

e. Show authenticated username in your booklist page.

f. Restrict Delete book functionality only to users with ADMIN role.

Push all changes to GitHub.

## Требования без решения

- Продолжить собственный Bookstore.
- Подключить Spring Security и закрыть прикладные URL от анонимных пользователей.
- Создать двух пользователей в памяти с ролями USER и ADMIN.
- Реализовать собственную страницу входа, выход и показ имени вошедшего пользователя.
- Разрешить удаление книги только ADMIN.
- Сохранить изменения в GitHub при сдаче.

Пример SecureStudentList из Moodle в архив не включён. Форма входа технически должна быть доступна до аутентификации; иначе войти невозможно. Это пояснение к реализации, не новое условие преподавателя.

[Пошаговый разбор](../guides/bookstore-security-1.md)
