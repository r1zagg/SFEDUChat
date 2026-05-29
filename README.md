# SFEDU Chat — зашифрованный мессенджер (курсовая)

Android-приложение для отправки **зашифрованных текстовых сообщений и изображений** с гибридной криптосхемой (AES + RSA), архитектурой **MVVM + Clean Architecture** и локальным демо-транспортом (без обязательного Firebase).

## Технологии

| Компонент | Решение |
|-----------|---------|
| Язык | Kotlin |
| minSdk | 24 |
| UI | Jetpack Compose + Material 3 |
| Архитектура | `:domain`, `:data`, `:app` (presentation) |
| DI | Koin |
| БД | Room (пользователи, чаты, зашифрованная история, relay) |
| Ключи | Android Keystore (RSA-2048) |
| Сеть | Firebase Realtime Database (опционально) + локальный relay в Room |

## Криптосхема (гибридная)

```
Отправитель                          Получатель
──────────                          ──────────
1. Генерация случайного AES-256 ключа (на каждое сообщение)
2. AES-GCM(plaintext, key, random IV) → ciphertext + tag
3. HMAC-SHA256(ciphertext, derive(key)) → подпись (доп. демо)
4. RSA-OAEP(recipient_pubkey, AES key) → encryptedAesKey
5. RSA-OAEP(sender_pubkey, AES key) → encryptedAesKeySelf (для своей истории)
6. На «сервер» уходит только ciphertext, IV, encryptedAesKey, hmac
7. Получатель: RSA decrypt → AES decrypt (только на устройстве)
```

**Почему гибридная схема?** RSA медленный и не подходит для больших данных. AES быстрый, но нужен общий секрет. RSA передаёт только короткий AES-ключ (обёртка). Публичный ключ собеседника хранится в `/users/{uid}/public_key` (Firebase) или в локальной БД.

### Алгоритмы

- **Симметричное:** AES-256-GCM, IV 12 байт, случайный на каждое сообщение
- **Асимметричное:** RSA-2048, OAEP SHA-256 MGF1
- **Подпись:** HMAC-SHA256 (дополнительно к аутентификации GCM)
- **Приватный ключ:** только Android Keystore, alias `sfedu_rsa_key_{uid}`

## Сборка

1. Клонировать / открыть проект в **Android Studio** (Ladybug+).
2. JDK 11+.
3. Синхронизировать Gradle.
4. Запустить на эмуляторе API 24+.

### Firebase (опционально)

1. Создать проект в [Firebase Console](https://console.firebase.google.com).
2. Добавить Android-приложение `fm.mrc.sfeduchat`.
3. Скачать `google-services.json` в `app/`.
4. В `app/build.gradle.kts` раскомментировать `alias(libs.plugins.google.services)`.
5. В корневом `build.gradle.kts` применить плагин `google-services`.
6. Realtime Database: правила для демо (только учебный проект!):

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

Без Firebase приложение работает через **LocalRelayTransport** (общая таблица `relay_messages` в Room).

## Демо: два пользователя на одном эмуляторе

1. Зарегистрируйте **alice** / пароль `1234`.
2. Выйдите → зарегистрируйте **bob** / `1234`.
3. Войдите как **alice** → «+» → новый чат → username `bob`.
4. Отправьте сообщение.
5. Выйдите → войдите как **bob** → откройте чат с **alice** → сообщение расшифруется.

Сообщения в Room и relay хранятся **только в зашифрованном виде**; plaintext появляется в UI после расшифровки.

## Модули

- **`domain`** — модели `User`, `Chat`, `EncryptedMessage`, интерфейсы репозиториев, use cases
- **`data`** — `AesEncryptionManager`, `RsaEncryptionManager`, `KeystoreManager`, `HmacSignatureManager`, Room, транспорт
- **`app`** — Compose UI, ViewModels, Hilt, навигация

## Тесты

```bash
./gradlew :data:testDebugUnitTest
```

JUnit-тесты: AES round-trip, RSA wrap, HMAC, гибридное шифрование (JVM, без Keystore).

## Экраны

- Логин / регистрация
- Список чатов
- Чат (текст + изображение, статус доставки)
- Управление ключами (просмотр pubkey, регенерация RSA)

## Структура ключевых классов

```
data/crypto/
  AesEncryptionManager.kt
  RsaEncryptionManager.kt
  KeystoreManager.kt
  HmacSignatureManager.kt
  MessageCryptoService.kt
data/repository/
  AuthRepositoryImpl.kt
  MessageRepositoryImpl.kt
  KeyRepositoryImpl.kt
app/presentation/
  ChatViewModel.kt
  KeyManagementViewModel.kt
```

## Автор

Курсовая работа — «Реализация приложения для отправки зашифрованных сообщений на Android».
