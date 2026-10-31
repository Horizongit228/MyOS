# MyOS 1.1 · Android + Groq

Простой светлый интерфейс: главная, чат, голосовой ввод, заметки, задачи и настройки.

## Главные ИИ-модели
- **Чат**: openai/gpt-oss-120b (Groq Chat Completions)
- **Голос → текст**: whisper-large-v3 (Groq Audio Transcriptions)
- Выбор альтернатив: openai/gpt-oss-20b, qwen/qwen3.8-27b; whisper-large-v3-turbo.

## Получить APK
Перейдите в Actions → Build MyOS APK → последний успешный запуск → Artifacts → MyOS-debug-APK.
Скачайте ZIP-артефакт и извлеките app-debug.apk. При новом коммите сборка запускается автоматически.

Либо: gradle :app:assembleDebug (JDK 17, Gradle 8.14.3, Android SDK 36).

## Первое использование
1. Установить APK на Android 8.0+.
2. Открыть Настройки и вставить личный API-ключ с https://console.groq.com/keys.
3. Открыть ИИ и написать текст либо нажать микрофон, разрешить запись и остановить её.

## Безопасность
Ключ **не публикуется** в репозитории или APK. Он хранится в Android Keystore, а запросы направляются напрямую на api.groq.com по HTTPS. Для публичных установок с одним общим ключом нужен сервер-посредник. Разрешение RECORD_AUDIO запрашивается по нажатию микрофона; звук временно сохраняется в кэше, отправляется на Groq для распознавания и затем удаляется. История сообщений, заметки и задачи хранятся локально.

## Ограничения
Groq API требует подключение к интернету и имеет бесплатные лимиты. Debug APK подписывается отладочным ключом CI: для постоянного обновления на разных сборках потребуется свой постоянный release-keystore. Это приложение, а не замена Android.

Документация:
- https://console.groq.com/docs/models
- https://console.groq.com/docs/speech-to-text
- https://console.groq.com/docs/reasoning

## MyOS 1.4
- Installed-app grid: search, starred favorites, recent launches and original icons.
- AI chat opens apps from explicit user commands without confirmation.
- Microphone and Send are embedded in the chat text field.

## Permanent signed releases
The private release keystore must NEVER be committed to this public repo.
Set these GitHub Actions repository secrets: MYOS_KEYSTORE_BASE64 (base64
contents of permanent myos-release.jks), MYOS_STORE_PASSWORD,
MYOS_KEY_PASSWORD and MYOS_KEY_ALIAS (myos).

Settings > Secrets and variables > Actions > New repository secret.
On every push, GitHub builds debug and release variants. With the secrets,
it signs and verifies MyOS-release-signed.apk. Otherwise, it uploads
a debug APK and aligned unsigned release for secure off-repo signing.

Previously installed debug builds have a different signing certificate.
Back up local data before migrating to the release build.
