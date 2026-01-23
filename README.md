# Laboratory work #1 Information Security

Веб-приложение на Spring Boot с реализацией базовых мер информационной безопасности, включая защиту от SQL-инъекций, XSS-атак и безопасную аутентификацию через JWT токены.

## 📋 Содержание

- [Описание проекта](#описание-проекта)
- [Технологический стек](#стек)
- [API Эндпоинты](#api-эндпоинты)
- [Меры защиты](#меры-защиты)
  - [Защита от SQL-инъекций (SQLi)](#защита-от-sql-инъекций-sqli)
  - [Защита от XSS](#защита-от-xss)
  - [Защита от Broken Authentication](#защита-от-broken-authentication)
- [CI/CD и Security Scanning](#cicd-и-security-scanning)
---

## Описание проекта

Проект представляет собой RESTful API для управления пользователями с реализацией базовых мер информационной безопасности. Приложение обеспечивает:

- Регистрацию и аутентификацию пользователей
- Защиту API эндпоинтов с помощью JWT токенов
- Автоматическую санитизацию пользовательских данных
- Защиту от основных типов атак (SQLi, XSS, Broken Authentication)
- Автоматизированное сканирование безопасности через CI/CD pipeline

---

## Стек

- **Java 21** - язык программирования
- **Spring Boot 3.5.6** - фреймворк
- **Spring Security** - безопасность и аутентификация
- **Spring Data JPA / Hibernate** - работа с базой данных
- **PostgreSQL** - реляционная база данных
- **JWT (JSON Web Tokens)** - токены аутентификации
- **Argon2** - хэширование паролей
- **OWASP Java HTML Sanitizer** - защита от XSS
- **SpotBugs + FindSecBugs** - статический анализ кода (SAST)
- **OWASP Dependency-Check** - анализ зависимостей (SCA)

---

## API Эндпоинты

Приложение предоставляет следующие REST API эндпоинты:

### 1. Регистрация пользователя
- **Метод:** `POST /auth/register`
- **Описание:** Создание нового пользователя в системе
- **Требует аутентификации:** Нет
- **Тело запроса:**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Ответ:** `200 OK` - "User registered" или `400 Bad Request` - "User already exists"

### 2. Аутентификация (логин)
- **Метод:** `POST /auth/login`
- **Описание:** Получение JWT токена для доступа к защищенным эндпоинтам
- **Требует аутентификации:** Нет
- **Тело запроса:**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Ответ:** `200 OK` - JWT токен (строка) или `403 Forbidden`

### 3. Получение данных
- **Метод:** `GET /api/data`
- **Описание:** Получение списка всех зарегистрированных пользователей
- **Требует аутентификации:** Да (JWT токен)
- **Заголовки:** `Authorization: Bearer <token>`
- **Ответ:** `200 OK` - массив имен пользователей `["user1", "user2", ...]` или `403 Forbidden`

### 📖 Подробные примеры запросов

Полные примеры использования всех эндпоинтов с командами cURL находятся в файле [API_EXAMPLES.md](API_EXAMPLES.md).

---

## Меры защиты

### Защита от SQL-инъекций (SQLi)

#### Описание проблемы
SQL-инъекция — это техника атаки, при которой злоумышленник внедряет вредоносный SQL-код в запросы к базе данных, что может привести к несанкционированному доступу к данным или их изменению.

#### Реализованная защита

**1. Использование ORM (Object-Relational Mapping)**
- Приложение использует **Spring Data JPA** с **Hibernate** в качестве ORM
- Все запросы к базе данных выполняются через методы репозитория, а не через прямой SQL

**2. Параметризованные запросы**
- Hibernate автоматически использует **Prepared Statements** для всех запросов
- Все параметры передаются через параметризованные запросы, что исключает возможность внедрения SQL-кода

**3. Отсутствие конкатенации строк**
- В проекте **полностью отсутствует** конкатенация строк для формирования SQL-запросов
- Все запросы генерируются автоматически через JPA методы

**Результат:** Приложение полностью защищено от SQL-инъекций благодаря использованию ORM и параметризованных запросов.

---

### Защита от XSS

#### Описание проблемы
XSS (Cross-Site Scripting) — это уязвимость, позволяющая злоумышленнику внедрить вредоносный JavaScript-код в веб-страницу, который выполняется в браузере других пользователей.

#### Реализованная защита

**1. Автоматическая санитизация всех пользовательских данных**

Приложение использует **OWASP Java HTML Sanitizer** для автоматической очистки всех входящих строковых данных от потенциально опасного контента.

**Реализация:**

```java
// SanitizingJacksonConfig.java
@Configuration
public class SanitizingJacksonConfig {
    
    @Bean
    public PolicyFactory xssPolicyFactory() {
        // Политика санитизации: разрешает базовое форматирование,
        // ссылки и изображения, но удаляет все скрипты и обработчики событий
        return Sanitizers.BLOCKS
            .and(Sanitizers.FORMATTING)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES);
    }
    
    @Bean
    public Module sanitizeAllStringsModule(PolicyFactory policy) {
        // Кастомный десериализатор для всех String полей
        SimpleModule m = new SimpleModule();
        m.addDeserializer(String.class, new SanitizingStringDeserializer(policy));
        return m;
    }
}
```

**2. Как это работает:**
- При десериализации JSON в Java объекты, **все строковые поля автоматически санитизируются**
- Опасные HTML теги (`<script>`, `<iframe>`, обработчики событий `onclick`, `onerror` и т.д.) удаляются
- Разрешенные теги (например, `<b>`, `<i>`, `<a>`) остаются, но их атрибуты проверяются на безопасность

**Пример защиты:**
```json
// Входящий запрос с потенциально опасным контентом:
{
  "username": "<script>alert('XSS')</script>user",
  "password": "pass123"
}

// После санитизации:
{
  "username": "user",  // <script> тег удален
  "password": "pass123"
}
```

**3. Защита на уровне ответов**
- Все данные, возвращаемые API, также проходят через механизмы Spring, которые автоматически экранируют специальные символы при необходимости

**Результат:** Все пользовательские данные автоматически очищаются от потенциально опасного контента при получении, что полностью защищает приложение от XSS-атак.

---

### Защита от Broken Authentication

#### Описание проблемы
Broken Authentication — это уязвимости, связанные с неправильной реализацией механизмов аутентификации, что может привести к компрометации учетных записей пользователей.

#### Реализованная защита

**1. Хэширование паролей с использованием Argon2**

**Почему Argon2?**
- Считается одним из самых безопасных алгоритмов хэширования паролей
- Устойчив к атакам перебора (brute-force) и rainbow table атакам
- Поддерживает настраиваемые параметры сложности


**Как это работает:**
- При регистрации пароль **никогда не сохраняется в открытом виде**
- Пароль хэшируется с использованием Argon2 перед сохранением в базу данных
- При аутентификации введенный пароль хэшируется и сравнивается с сохраненным хэшем


**2. JWT (JSON Web Tokens) для аутентификации**

**Преимущества JWT:**
- Stateless аутентификация (не требуется хранение сессий на сервере)
- Токены содержат информацию о пользователе и сроке действия
- Подпись токена гарантирует его целостность

**Реализация:**

**Генерация токена:**
```java
// JwtUtil.java
public String generateToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(getKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**Параметры токена (настраиваются в application.properties):**
- `jwt.secret` - секретный ключ для подписи (>= 32 байта)
- `jwt.expiration-ms` - время жизни токена (по умолчанию 3600000 мс = 1 час)

**3. Middleware для проверки JWT токенов**

**Реализация фильтра:**
```java
// JwtFilter.java
@Component
public class JwtFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(...) {
        // Извлечение токена из заголовка Authorization
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // Валидация токена
            if (jwtUtil.validateToken(token)) {
                // Установка аутентификации в SecurityContext
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
    }
}
```

**Как это работает:**
- Фильтр проверяет **каждый запрос** к защищенным эндпоинтам
- Токен извлекается из заголовка `Authorization: Bearer <token>`
- Проверяется валидность токена (подпись, срок действия)
- Если токен валиден, пользователь считается аутентифицированным

**4. Настройка безопасности в Spring Security**

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers(authEndpoints)  // CSRF отключен для /auth/**
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        )
        .sessionManagement(sm -> sm
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // Без сессий
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll()  // Публичные эндпоинты
            .anyRequest().authenticated()  // Остальные требуют аутентификации
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
}
```

**Особенности:**
- **Stateless** архитектура (без серверных сессий)
- Публичные эндпоинты (`/auth/**`) доступны без токена
- Все остальные эндпоинты требуют валидный JWT токен
- CSRF защита настроена для браузерных клиентов

**Результат:** 
- Пароли хранятся в безопасном хэшированном виде (Argon2)
- Аутентификация реализована через JWT токены с проверкой на каждом запросе
- Токены имеют ограниченный срок действия
- Все защищенные эндпоинты проверяют валидность токена через middleware

---

## CI/CD и Security Scanning

Проект настроен с автоматическими проверками безопасности через GitHub Actions.

### SAST (Static Application Security Testing)

**Инструмент:** SpotBugs + FindSecBugs Plugin

**Конфигурация:** `.github/workflows/spotbugs.yml`

**Что проверяется:**
- Статический анализ кода на наличие уязвимостей
- Поиск распространенных ошибок безопасности
- Проверка на использование небезопасных паттернов

**Запуск:** Автоматически при каждом push и pull request

### SCA (Software Composition Analysis)

**Инструмент:** OWASP Dependency-Check

**Конфигурация:** `.github/workflows/dependency-check.yml`

**Что проверяется:**
- Известные уязвимости в зависимостях проекта
- Устаревшие библиотеки с уязвимостями
- CVSS оценки уязвимостей

**Запуск:** Автоматически при каждом push, pull request и еженедельно по расписанию

### Отчеты Security Scanning

#### Скриншот 1: SAST - SpotBugs Report
<img width="818" height="378" alt="Screenshot 2026-01-23 at 10 04 11" src="https://github.com/user-attachments/assets/4376ab37-9906-4ee5-95ce-9adc97c51fa1" />

**Описание:** Отчет показывает результаты статического анализа кода, включая найденные потенциальные уязвимости и рекомендации по их исправлению.

---

#### Скриншот 2: SCA - OWASP Dependency-Check Report


**Описание:** Отчет показывает результаты анализа зависимостей проекта, включая список известных уязвимостей (CVE) в используемых библиотеках и их CVSS оценки.

---
