# MeowAuth v2 🔐

**Password-free authentication mod for Minecraft Forge 1.20.1**

[English](#english) | [Русский](#русский)

---

## English

### 📖 About

MeowAuth is a server-side authentication mod that provides a secure token-based login system. Perfect for non-premium (cracked) servers.

> **v2.0** — Complete rewrite with BCrypt security, automatic token authentication, and brute-force protection.

---

### ✨ Features

- 🔒 **BCrypt token hashing** — Industry-standard security, tokens are never stored in plain text
- 🤖 **Automatic authentication** — No passwords to remember, tokens are handled automatically
- 🚫 **Brute-force protection** — Account lockout after failed attempts with configurable cooldown
- ⚡ **Async data saving** — Non-blocking I/O, no server lag
- 🛡️ **Rate limiting** — Configurable max attempts and lockout duration
- 🌐 **Customizable messages** — Kick messages, debug logging, and more
- ⚙️ **Simple configuration** — Single JSON config file with sensible defaults

---

### 📥 Installation

#### Server

1. Install **Minecraft Forge 1.20.1**
2. Download `MeowAuth-Server-2.0.0.jar` from [Releases](https://github.com/Jun1devs/MeowAuth-v2/releases)
3. Place the file in your server's `mods/` folder
4. Start the server — configuration will be created automatically

#### Client (Players)

1. Install **Minecraft Forge 1.20.1**
2. Download `MeowAuth-Client-2.0.0.jar` from [Releases](https://github.com/Jun1devs/MeowAuth-v2/releases)
3. Place the file in your `mods/` folder
4. Launch Minecraft — token is saved and sent automatically on join

---

### 🎮 Commands

| Command       | Side   | Description                      |
|---------------|--------|----------------------------------|
| `/authstatus` | Server | Check your authentication status |
| `/cleartoken` | Client | Remove saved token               |

---

### ⚙️ Configuration

After first server start, edit `config/meowauth-server.json`:

```json
{
  "debug": false,
  "tokenLength": 32,
  "dataFile": "config/meowauth_users.json",
  "autoSave": true,
  "kickMessage": "§cAuthentication failed: invalid or missing token.",
  "maxLoginAttempts": 5,
  "lockoutDurationSeconds": 300
}
```

| Setting                  | Description                  | Default                        |
|--------------------------|------------------------------|--------------------------------|
| `debug`                  | Enable debug logging         | `false`                        |
| `tokenLength`            | Token length in bytes (8–64) | `32`                           |
| `dataFile`               | User data file path          | `config/meowauth_users.json`   |
| `autoSave`               | Asynchronous auto-save       | `true`                         |
| `kickMessage`            | Message on failed auth       | `"§cAuthentication failed..."` |
| `maxLoginAttempts`       | Attempts before lockout      | `5`                            |
| `lockoutDurationSeconds` | Lockout duration             | `300` (5 min)                  |

---

### 🛡️ Security Features

| Feature                  | Description                                                        |
|--------------------------|--------------------------------------------------------------------|
| **BCrypt hashing**       | Tokens are hashed with cost factor 12 — never stored in plain text |
| **Cryptographic tokens** | Generated with `SecureRandom`, Base64 URL-safe encoded             |
| **Rate limiting**        | Configurable lockout after N failed attempts                       |
| **Token validation**     | Min 6 / Max 72 characters (BCrypt limit)                           |
| **Async I/O**            | File operations run on a separate thread — no main thread blocking |

---

### 📦 Dependencies

- **Minecraft Forge 1.20.1** (47.4.10+)
- **Java 17+**

---

### 📝 License

MIT License — free to use, modify, and distribute.

### 👥 Authors

- **Gocti** — Lead Developer
- **Jun1devs Team**

---

## Русский

### 📖 Описание

MeowAuth — мод серверной авторизации для Minecraft с безопасной системой входа на основе токенов. Идеально подходит для не-премиум (пиратских) серверов.

> **v2.0** — Полная переработка: BCrypt-хеширование, автоматическая аутентификация токенами и защита от перебора.

---

### ✨ Особенности

- 🔒 **BCrypt хеширование токенов** — Промышленный стандарт безопасности, токены никогда не хранятся в открытом виде
- 🤖 **Автоматическая аутентификация** — Не нужно запоминать пароли, токены обрабатываются автоматически
- 🚫 **Защита от перебора** — Блокировка аккаунта после неудачных попыток с настраиваемой задержкой
- ⚡ **Асинхронное сохранение** — Неблокирующий ввод-вывод, без лагов сервера
- 🛡️ **Ограничение попыток** — Настраиваемое количество попыток и длительность блокировки
- 🌐 **Настраиваемые сообщения** — Сообщения при отключении, отладочное логирование и другое
- ⚙️ **Простая конфигурация** — Один JSON-файл с разумными настройками по умолчанию

---

### 📥 Установка

#### Сервер

1. Установите **Minecraft Forge 1.20.1**
2. Скачайте `MeowAuth-Server-2.0.0.jar` из [Releases](https://github.com/Jun1devs/MeowAuth-v2/releases)
3. Поместите файл в папку `mods/` на сервере
4. Запустите сервер — конфигурация создастся автоматически

#### Клиент (Игроки)

1. Установите **Minecraft Forge 1.20.1**
2. Скачайте `MeowAuth-Client-2.0.0.jar` из [Releases](https://github.com/Jun1devs/MeowAuth-v2/releases)
3. Поместите файл в папку `mods/`
4. Запустите Minecraft — токен сохранится и отправится автоматически при подключении

---

### 🎮 Команды

| Команда       | Сторона | Описание                        |
|---------------|---------|---------------------------------|
| `/authstatus` | Сервер  | Проверить статус аутентификации |
| `/cleartoken` | Клиент  | Удалить сохранённый токен       |

---

### ⚙️ Конфигурация

После первого запуска сервера отредактируйте `config/meowauth-server.json`:

```json
{
  "debug": false,
  "tokenLength": 32,
  "dataFile": "config/meowauth_users.json",
  "autoSave": true,
  "kickMessage": "§cAuthentication failed: invalid or missing token.",
  "maxLoginAttempts": 5,
  "lockoutDurationSeconds": 300
}
```

| Настройка                | Описание                        | По умолчанию                   |
|--------------------------|---------------------------------|--------------------------------|
| `debug`                  | Включить отладочное логирование | `false`                        |
| `tokenLength`            | Длина токена в байтах (8–64)    | `32`                           |
| `dataFile`               | Путь к файлу данных             | `config/meowauth_users.json`   |
| `autoSave`               | Асинхронное автосохранение      | `true`                         |
| `kickMessage`            | Сообщение при ошибке входа      | `"§cAuthentication failed..."` |
| `maxLoginAttempts`       | Попыток до блокировки           | `5`                            |
| `lockoutDurationSeconds` | Длительность блокировки         | `300` (5 мин)                  |

---

### 🛡️ Функции безопасности

| Функция                      | Описание                                                                 |
|------------------------------|--------------------------------------------------------------------------|
| **BCrypt хеширование**       | Токены хешируются с cost factor 12 — никогда не хранятся в открытом виде |
| **Криптографические токены** | Генерируются через `SecureRandom`, закодированы Base64 URL-safe          |
| **Ограничение попыток**      | Блокировка после N неудачных попыток                                     |
| **Валидация токенов**        | Мин. 6 / Макс. 72 символа (ограничение BCrypt)                           |
| **Асинхронный ввод-вывод**   | Файловые операции в отдельном потоке — без блокировки главного потока    |

---

### 📦 Зависимости

- **Minecraft Forge 1.20.1** (47.4.10+)
- **Java 17+**

---

### 📝 Лицензия

MIT License — свободное использование, изменение и распространение.

### 👥 Авторы

- **Gocti** — Ведущий разработчик
- **Jun1devs Team**

---

## 📬 Support / Поддержка

- **Issues**: [GitHub Issues](https://github.com/Jun1devs/MeowAuth-v2/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Jun1devs/MeowAuth-v2/discussions)

---

<p style="text-align: center;">Made with ❤️ by Jun1devs</p>
