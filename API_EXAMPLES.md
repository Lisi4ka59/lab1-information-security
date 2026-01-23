# Примеры запросов к API

Базовый URL: `http://localhost:8080`

## 1. Регистрация пользователя

**Эндпоинт:** `POST /auth/register`

**Описание:** Создание нового пользователя в системе. Пароль автоматически хэшируется с помощью Argon2.

### cURL
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### HTTP запрос
```http
POST /auth/register HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

### Пример ответа (успех)
```json
"User registered"
```

### Пример ответа (ошибка - пользователь уже существует)
```json
"User already exists"
```

---

## 2. Аутентификация (логин)

**Эндпоинт:** `POST /auth/login`

**Описание:** Получение JWT токена для доступа к защищенным эндпоинтам. Токен действителен 1 час (3600000 мс).

### cURL
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### HTTP запрос
```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

### Пример ответа (успех)
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTcwMDAwMDAwMH0.abc123...
```

**Важно:** Сохраните полученный токен для использования в следующих запросах.

### Пример ответа (ошибка - неверные учетные данные)
```
401 Unauthorized
```

---

## 3. Получение данных (защищенный эндпоинт)

**Эндпоинт:** `GET /api/data`

**Описание:** Получение списка всех зарегистрированных пользователей. Требует JWT токен в заголовке Authorization.

### cURL
```bash
curl -X GET http://localhost:8080/api/data \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTcwMDAwMDAwMH0.abc123..."
```

### HTTP запрос
```http
GET /api/data HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTcwMDAwMDAwMH0.abc123...
```

### Пример ответа (успех)
```json
["testuser", "admin", "user1"]
```

### Пример ответа (ошибка - токен отсутствует или невалиден)
```
403 Forbidden
```

---

## Примечания

- JWT токен имеет срок действия 1 час (настраивается в `application.properties` через `jwt.expiration-ms`)
- Все пароли хэшируются с помощью Argon2 перед сохранением в базу данных
- Все пользовательские данные санитизируются для защиты от XSS
- Защищенные эндпоинты требуют валидный JWT токен в заголовке `Authorization: Bearer <token>`
