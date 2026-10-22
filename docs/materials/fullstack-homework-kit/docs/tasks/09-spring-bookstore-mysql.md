# Spring — MySQL Booklist (Optional)

> Статус: **Optional**. Выполняется после обязательного Chapter 4, не заменяет категории и сдаётся через Moodle, если студент выбирает дополнительную работу.

## Что требуется

- Ознакомиться с StudentListJDBC из Moodle как примером; отдельно сдавать его не требуется.
- Установить и запустить локальный MySQL через XAMPP.
- Создать учебную базу `test`.
- Использовать пользователя `root`; в исходном листе пустой пароль указан как default учебной XAMPP, но фактическая локальная настройка может отличаться.
- Запустить тот же Booklist/Bookstore с MySQL вместо H2.
- Добавить MySQL Connector/J как runtime dependency.
- Вынести MySQL в явно активируемый Spring profile `mysql`, чтобы default и tests оставались H2-only.
- Читать URL и username из окружения, а password — **только** из `${BOOKSTORE_DB_PASSWORD}` без default/fallback.
- Не хранить реальные credentials, `.env` или local properties в Git, ZIP, guide либо shell history.
- Проверить CRUD, categories и сохранение данных после перезапуска на реальном локальном MySQL.
- Вернуть Optional-работу через актуальное поле сдачи Moodle.

## Границы Optional

Исходное условие требует MySQL-версию Booklist, но не диктует profile, env names, migrations или отдельную копию приложения. Профиль — безопасное улучшение accumulated-проекта: Java-код, JPA entities, repositories, controller и Thymeleaf из Chapter 4 остаются одни и те же. `application-mysql.properties` меняет только инфраструктуру. Обычный `clean verify` не активирует `mysql`, поэтому XAMPP и MySQL credentials тестам не нужны.

## Что получится

Без профиля приложение использует in-memory H2 из `application.properties`. Только при явном `mysql` Spring читает три переменные окружения, загружает Connector/J и подключается к `test`. `ddl-auto=update` сохраняет схему и строки между перезапусками; H2 Console отключена. Реальный password нигде не записывается.

## Подготовка на macOS

Сначала полностью завершите guide 04 и проверьте его на H2. Нужны JDK 17+, Git, Maven Wrapper и XAMPP. Скачивайте XAMPP с официального Apache Friends. В XAMPP Manager/Control Panel включите MySQL и убедитесь, что порт (обычно 3306) свободен.

До любых MySQL-настроек выполните default gate.

macOS/Linux:

```bash
java -version
cd fullstack-homework-kit/projects/spring-boot/bookstore
chmod +x mvnw
./mvnw clean verify
```

### Windows PowerShell

```powershell
java -version
Set-Location fullstack-homework-kit\projects\spring-boot\bookstore
.\mvnw.cmd clean verify
```

Этот запуск обязан пройти при остановленном XAMPP и без переменных MySQL: это доказательство H2-only default profile.

## Создание с нуля

Начинайте только с проверенного Bookstore Chapter 4. Добавьте Connector/J, profile, ignore rules и static test по полным файлам ниже, не меняя Java business logic/templates. Затем создайте базу `test`.

В phpMyAdmin откройте вкладку SQL либо используйте MySQL client:

```sql
CREATE DATABASE test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Команду выполняют один раз. Если `test` уже существует, сначала убедитесь, что это ваша учебная база; не удаляйте чужие данные. `root` и пустой password отражают default исходного задания. Безопаснее задать локальному root password и вводить его только через скрытый prompt.

## Структура файлов

```text
bookstore/
├── pom.xml                                      # изменить
├── .gitignore                                   # проверить/изменить
├── src/main/resources/application.properties   # не менять: default H2
├── src/main/resources/application-mysql.properties  # создать
└── src/test/java/com/example/bookstore/
    └── BookstoreApplicationTests.java           # дополнить статическим gate
```

Java production-классы и templates Chapter 4 не меняются. Ниже приведены полные листинги каждого ручного файла Optional.

## Пошаговая реализация

### 1. `pom.xml`

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
    <artifactId>bookstore</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>bookstore</name>
    <description>Учебное приложение Spring Boot Bookstore</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
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

Единственное Optional-добавление относительно чистого Chapter 4 — `com.mysql:mysql-connector-j` с `runtime` scope. Код использует стандартные JPA/DataSource abstractions и не импортирует driver classes, поэтому compile dependency не нужна. H2 остаётся рядом: наличие Connector/J не активирует MySQL само по себе, datasource выбирает profile. Версию Connector/J согласованно управляет Spring Boot parent, вручную её задавать не требуется.

### 2. `src/main/resources/application-mysql.properties`

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

Имя файла задаёт profile `mysql`: Spring загружает его только при активации. `${BOOKSTORE_MYSQL_URL}` и `${BOOKSTORE_MYSQL_USERNAME}` требуют значения окружения. Password читает **ровно** `${BOOKSTORE_DB_PASSWORD}`; формы `${BOOKSTORE_DB_PASSWORD:}`, `${BOOKSTORE_DB_PASSWORD:secret}` и hardcoded password запрещены, потому что fallback позволил бы незаметно запустить приложение с сохранённым credential.

`com.mysql.cj.jdbc.Driver` — Connector/J. `ddl-auto=update` сохраняет существующие данные и обновляет учебную схему; для production обычно применяют Flyway/Liquibase, но это вне задания. SQL logging оставлен учебным. H2 Console выключена, потому что активна внешняя БД. В этом файле нет `spring.profiles.active`: профиль включается только командой запуска.

### 3. `.gitignore`

```gitignore
target/
.env
.env.*
*.local.properties
*.mv.db
```

`target/` — build output. `.env` и любые `.env.*` не позволяют случайно закоммитить credentials; Optional не создаёт даже `.env.example`, потому что имена переменных уже документированы здесь. `*.local.properties` закрывает локальные overrides, `*.mv.db` — файловые H2-базы. Ignore — дополнительный барьер, а не разрешение хранить password локально в проекте: используйте только process environment.

### 4. `BookstoreApplicationTests.java`

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
    void defaultProfileConfiguresH2SqlLoggingAndConsole() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).contains("spring.datasource.url=jdbc:h2:mem:bookstore");
        assertThat(properties).contains("spring.jpa.show-sql=true");
        assertThat(properties).contains("spring.h2.console.enabled=true");
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

`@SpringBootTest` без active profile запускает реальный default context на H2 — это основной offline behavior gate. Третий тест статически проверяет boundary внешней инфраструктуры без соединения: правильные env placeholders, точное dedicated-имя password и отсутствие fallback. Он не активирует `mysql`, поэтому не требует XAMPP. Если изменить password placeholder на старое имя или добавить default, тест падает до попытки внешнего подключения.

## Неизменённый default H2

`application.properties` из Chapter 4 сохраняется без изменений:

```properties
spring.application.name=bookstore
spring.datasource.url=jdbc:h2:mem:bookstore;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Этот файл не создаётся/не изменяется Optional, но полный листинг показывает, почему tests остаются H2-only. Profile properties переопределяют datasource/DDL/console только при `mysql`. Пустой password здесь принадлежит ephemeral H2, а не MySQL credential.

## Настройка окружения и запуск на macOS/Linux

Задайте только несекретные URL/username обычным export:

```bash
export BOOKSTORE_MYSQL_URL='jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
export BOOKSTORE_MYSQL_USERNAME='root'
```

На macOS стандартный shell — zsh. В нём password прочитайте без эха такой командой:

```zsh
read -s "BOOKSTORE_DB_PASSWORD?MySQL password: "
echo
export BOOKSTORE_DB_PASSWORD
./mvnw -Dspring-boot.run.profiles=mysql spring-boot:run
```

В Bash (например, Linux или явно запущенный `bash`) синтаксис prompt другой:

```bash
IFS= read -r -s -p 'MySQL password: ' BOOKSTORE_DB_PASSWORD
echo
export BOOKSTORE_DB_PASSWORD
./mvnw -Dspring-boot.run.profiles=mysql spring-boot:run
```

Если XAMPP действительно использует пустой password, нажмите Enter: переменная будет экспортирована с пустым значением, но placeholder всё равно не имеет default. Безопаснее назначить локальный password. После `Control+C` выполните `unset BOOKSTORE_DB_PASSWORD BOOKSTORE_MYSQL_URL BOOKSTORE_MYSQL_USERNAME` или закройте terminal.

## Настройка окружения и запуск на Windows

Используйте PowerShell 7 (`pwsh`) для скрытого строкового ввода:

```powershell
$env:BOOKSTORE_MYSQL_URL='jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:BOOKSTORE_MYSQL_USERNAME='root'
$env:BOOKSTORE_DB_PASSWORD=Read-Host 'MySQL password' -MaskInput
.\mvnw.cmd -Dspring-boot.run.profiles=mysql spring-boot:run
```

В Windows PowerShell 5.1 `-MaskInput` отсутствует. Безопасный prompt без попадания password в history:

```powershell
$securePassword = Read-Host 'MySQL password' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:BOOKSTORE_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    .\mvnw.cmd -Dspring-boot.run.profiles=mysql spring-boot:run
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    Remove-Item Env:BOOKSTORE_DB_PASSWORD -ErrorAction SilentlyContinue
}
```

Не печатайте переменную и не вставляйте реальное значение в guide/command line. Для старого PowerShell и пустого XAMPP password надёжнее сначала назначить локальный password: разные версии PowerShell по-разному сохраняют пустые process variables, а profile намеренно не имеет fallback.

## Запуск и тесты

Сначала выполните статическую проверку profile и default H2 gate без MySQL. Только после их успеха запускайте отдельный ручной сценарий с локальным XAMPP и credentials командами macOS/Windows выше.

macOS/Linux:

```bash
grep -Fx 'spring.datasource.password=${BOOKSTORE_DB_PASSWORD}' src/main/resources/application-mysql.properties
! grep -F 'spring.datasource.password=${BOOKSTORE_DB_PASSWORD:' src/main/resources/application-mysql.properties
test ! -e .env
./mvnw clean verify
```

Windows PowerShell:

```powershell
$profileText = Get-Content -Raw src/main/resources/application-mysql.properties
if (-not $profileText.Contains('spring.datasource.password=${BOOKSTORE_DB_PASSWORD}')) { throw 'Wrong password placeholder' }
if ($profileText.Contains('spring.datasource.password=${BOOKSTORE_DB_PASSWORD:')) { throw 'Password fallback is forbidden' }
if (Test-Path .env) { throw '.env must not be stored in the project' }
.\mvnw.cmd clean verify
```

Эти команды не активируют profile и не открывают network connection. Maven tests используют H2. Отсутствие реального MySQL/XAMPP не является ошибкой этого offline gate.

## Ручная проверка

1. XAMPP/MySQL запущен, база `test` существует.
2. Заданы URL, username и `BOOKSTORE_DB_PASSWORD`; включён profile `mysql`.
3. В логах datasource — MySQL, не H2; приложение открывается на `/booklist`.
4. Seed books/categories появились только в пустой базе.
5. Создайте книгу, измените category, перезапустите приложение: запись осталась из-за внешней БД и `ddl-auto=update`.
6. Удалите созданную книгу; Category остаётся.
7. Проверьте таблицы/`BOOK.CATEGORY_ID` в phpMyAdmin.
8. `/h2-console` при profile `mysql` выключена.
9. Остановите MySQL и снова выполните default `clean verify`: tests проходят на H2.
10. `git status` не показывает `.env`, password или local properties.

Реальное подключение нельзя воспроизвести только по репозиторию: нужны локально запущенный XAMPP/MySQL и credentials пользователя. Поэтому автоматический gate намеренно не заявляет, что живое соединение проверено.

## Типичные ошибки

- `Communications link failure`: MySQL выключен, порт/URL неверен или TCP заблокирован.
- `Unknown database 'test'`: сначала создайте database.
- `Access denied for user 'root'`: неверный password/user или grants; не записывайте password в Git.
- Запуск без profile правильно выбирает H2.
- Отсутствующая `BOOKSTORE_DB_PASSWORD` должна остановить mysql startup: fallback запрещён.
- `BOOKSTORE_MYSQL_PASSWORD` — старое неверное имя; используется только `BOOKSTORE_DB_PASSWORD`.
- `.env`, `application-local.properties` с credentials и password в CLI/history запрещены.
- H2 `MODE=MySQL` не доказывает реальное MySQL connection.
- `ddl-auto=update` годится для учебной Optional, но не заменяет production migrations.
- Порт 8080 может быть занят предыдущим Spring process; остановите его `Control+C`.

## Вопросы и ответы

**Почему не создано второе приложение?** Profile меняет datasource, а entities/repositories/controller/templates остаются теми же; одна кодовая база не расходится на две копии.

**Почему tests не активируют MySQL?** Воспроизводимый automated gate должен проходить offline на H2. Живое MySQL connection — отдельная ручная проверка Optional.

**Почему password обязателен даже при пустом default XAMPP?** Обязательная env variable явно переносит ответственность в process environment; отсутствие fallback не позволяет незаметно использовать сохранённый credential.

**Можно ли создать `.env`?** Нет. Имена variables уже документированы, а реальные values вводятся только в текущий process.

**Заменяет ли H2 `MODE=MySQL` реальный MySQL?** Нет. Он помогает совместимости SQL, но persistence после restart и Connector/J проверяются только на живом MySQL.

## Защита

Покажите default и mysql properties, объясните profile override и почему Java-код не меняется. Не раскрывая values, назовите `BOOKSTORE_MYSQL_URL`, `BOOKSTORE_MYSQL_USERNAME`, `BOOKSTORE_DB_PASSWORD`. В phpMyAdmin покажите tables/category FK и запись после перезапуска. Затем остановите MySQL и докажите, что H2 tests проходят.

## Чек-лист сдачи

После живой MySQL-проверки остановите приложение, очистите environment и снова подтвердите H2-only gate:

```bash
unset BOOKSTORE_DB_PASSWORD BOOKSTORE_MYSQL_URL BOOKSTORE_MYSQL_USERNAME
./mvnw clean verify
git status --short
git add pom.xml .gitignore src/main/resources/application-mysql.properties src/test
git diff --cached --check
git diff --cached
git commit -m "Добавить опциональный профиль MySQL для Bookstore"
git push
```

Windows PowerShell:

```powershell
Remove-Item Env:BOOKSTORE_DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:BOOKSTORE_MYSQL_URL -ErrorAction SilentlyContinue
Remove-Item Env:BOOKSTORE_MYSQL_USERNAME -ErrorAction SilentlyContinue
.\mvnw.cmd clean verify
git status --short
git add pom.xml .gitignore src/main/resources/application-mysql.properties src/test
git diff --cached --check
git diff --cached
git commit -m "Добавить опциональный профиль MySQL для Bookstore"
git push
```

В staged diff не должно быть реального password, `.env`, локального override или `target/`. Точный формат Moodle в исходном листе не задан, поэтому откройте актуальное поле сдачи перед отправкой. Если оно принимает ссылку — вставьте ссылку на нужный commit/репозиторий. Если нужен ZIP исходников, после commit создайте воспроизводимый архив только из отслеживаемых файлов:

```bash
git archive --format=zip --output ../bookstore-mysql.zip HEAD
```

Проверьте содержимое архива и загрузите его в Moodle: там не должно быть `.env`, credentials, `target/` или дампа локальной БД. В комментарии можно указать, что CRUD и сохранение после перезапуска проверены на локальном XAMPP/MySQL, но никогда не прикладывайте password или скриншот с его значением.

- [ ] Mandatory Chapter 4 завершён отдельно.
- [ ] XAMPP/MySQL запущен, database называется `test`.
- [ ] Connector/J добавлен с runtime scope.
- [ ] Profile `mysql` активируется только явно.
- [ ] URL/username читаются из environment.
- [ ] Password записан только как `${BOOKSTORE_DB_PASSWORD}` без default.
- [ ] H2 Console выключена в mysql profile.
- [ ] List/add/edit/delete/category вручную проверены на реальном MySQL.
- [ ] Данные сохраняются после перезапуска.
- [ ] Default `clean verify` проходит при остановленном MySQL.
- [ ] `.env`, реальные credentials и local properties не попали в Git/ZIP.
- [ ] Реальное соединение не заявлено проверенным без локального XAMPP/credentials.
- [ ] Формат сдачи проверен в актуальном Moodle.
