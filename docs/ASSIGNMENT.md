# Chapter 6.2 — Bookstore: Securing the web application Part II

Источник: `sources/SP5-Exercise-Security.pdf`, страница 1, пункт 2.

## Исходное задание

Use your own bookstore application from the GitHub.

a. Create user entity for your application endusers.

   Attributes: username (unique), password, email, role.

b. Create crud repository for user entity.

c. Implement UserDetailService.

d. Add some users to your database and hash the passwords with BCrypt hashing algorithm.

e. Change Spring Security configuration to use user entities instead of in-memory users in authentication.

Push all changes to GitHub.

## Требования без решения

- Продолжить Part I, заменив источник пользователей: память → база данных.
- Создать сущность пользователя с уникальным username, паролем, email и ролью.
- Создать CRUD-репозиторий и сервис загрузки пользователя.
- Добавить пользователей с BCrypt-хешами паролей.
- Подключить аутентификацию через базу вместо in-memory пользователей.
- Сохранить изменения в GitHub при сдаче.

В PDF написано `UserDetailService`; корректное имя интерфейса Spring — `UserDetailsService`. Публичная регистрация, JWT, подтверждение email и восстановление пароля в этом задании не требуются.

[Полный разбор и код](MANUAL.md)
