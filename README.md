# Family Cinema

Семейный кинотеатр на Kotlin + Jetpack Compose / Media3. Русский интерфейс, локальный одобренный каталог, родительский PIN. Реализация **этапа 1**; статус проверок и ограничения — в [QA.md](QA.md). Контракт: [PROJECT_CONTRACT.md](PROJECT_CONTRACT.md), задание: [CODEX_TASK.md](CODEX_TASK.md).

## Сборка

Нужны полный JDK 21 (с javac), Android SDK 35 / build-tools 34.0.0, FFmpeg с libx264 и шрифт `/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf` (Debian/Ubuntu: `ffmpeg fonts-dejavu-core`). Укажите SDK в `local.properties` (`sdk.dir=...`) либо `ANDROID_HOME`.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :domain:test :parental:testDebugUnitTest :app:lintDebug
```

При первом запуске сборки генерируется собственное CC0-видео. Тяжёлые файлы, APK и keystore исключены из Git. APK: `app/build/outputs/apk/debug/app-debug.apk`. Это тестовая сборка с автоматически созданной локальной debug-подписью; для публикации требуется отдельная release-подпись. TV и mobile используют один APK с разными оболочками.

За прокси Gradle могут понадобиться `-Dhttps.proxyHost=... -Dhttps.proxyPort=...` и настроенное доверие к CA среды; проверку TLS отключать нельзя. Значения прокси и доверенного хранилища не зашиты в проект.

## Первый запуск

1. Родитель создаёт PIN (4–8 цифр) и повторяет его.
2. Открывает карточку «Тихий вечер», просматривает тестовое видео и одобряет эту версию.
3. Нажимает «Выйти»: в детском каталоге появляется одобренная карточка.
4. Запускает просмотр; при выходе позиция сохраняется. «Продолжить просмотр» возобновляет ролик.
5. В режиме родителя можно отозвать одобрение. При изменении версии прежнее одобрение не действует.

PIN не предустановлен. Восстановление PIN пока не реализовано. Очистка данных удаляет PIN, одобрения и позиции. Детский интерфейс не открывает внешние сайты или поиск.

## Проверки Android

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear ru.familycinema.app
adb shell am instrument -w ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
```

Инструментальный тест требует чистых данных приложения и мобильной оболочки. Он проверяет PIN, одобрение, реальное движение позиции Media3, продолжение, ошибочный PIN и отзыв одобрения. TV D-pad, физические устройства, доступность TalkBack и реальные кодеки проверяются отдельно.

## Backend

```sh
python -m venv .venv
.venv/bin/pip install -r backend/requirements.txt
cd backend
../.venv/bin/python -m pytest -q
../.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000
```

`GET /health` сообщает, что PostgreSQL не подключён. `GET /catalog` возвращает пустой типизированный список. Это серверный каркас, APK пока его не использует. Безопасный пример будущих переменных — `backend/.env.example`.

[Архитектура и границы модулей](docs/ARCHITECTURE.md), [происхождение тестового материала](docs/TEST_MEDIA.md). Torznab, magnet/.torrent, P2P, gateway, серверная синхронизация и ограничения времени ещё не реализованы. Их интерфейсы не являются рабочими интеграциями.
