# Family Cinema 0.3.0 — Android-first

Нативная семейная медиатека Kotlin / Jetpack Compose / Room / Media3. Основной сценарий работает на Android без домашнего сервера: личное видео, локальный поиск, точные родительские разрешения и продолжение просмотра. Для внешних каталогов реализован Kotlin-клиент открытого Stremio Addon Protocol; поставщики и контент не предустановлены.

[Испытания и ограничения](QA.md) · [Инструкция родителю](docs/PARENT_GUIDE_V03.md) · [Архитектура](docs/ARCHITECTURE.md) · [Библиотеки и лицензии](docs/DEPENDENCIES_V03.md) · [Скриншоты](docs/screenshots/v0.3.0/README.md)

## Возможности

- Локальная Room-библиотека: фильмы, сериалы/сезоны/эпизоды, метаданные, избранное, история, прогресс, источники и журнал решений.
- Системный выбор личных файлов, приватные проверенные копии с SHA-256, лимит места и отмена загрузки; поиск учитывает ё/е, альтернативные названия и транслитерацию.
- PIN и блокировка попыток из 0.2 сохранены. Родительская сессия закрывается в фоне; ребёнку доступны только разрешённые версии подходящего возраста.
- Автоодобрение по умолчанию OFF: подтверждение родителя, доверенные источники, известный возраст, проверенные байты; ручные запреты не перезаписываются. Удалённое автоодобрение требует ожидаемого SHA-256 от источника.
- Media3 progressive/HLS/DASH, fullscreen, landscape телефона, touch/D-pad/OK/Back, сохранение позиции. Детское воспроизведение — из неизменяемых приватных копий; HLS/DASH пока только родительский preview.
- Совместимые HTTPS addons: manifest/catalog/search/meta/streams/subtitles protocol. Stream-only источники могут дополнять каталог по стабильным внешним ID. Встроенного каталога коммерческих фильмов, TMDB/Jellyfin и торрент-воспроизведения нет.

Точная граница проверенного указана в QA. Собственный CC0-ролик — тестовый материал, а не замена настоящей пользовательской медиатеке. История 0.2: [отчёт](docs/qa/v0.2.0.md), [изменения](docs/RELEASE_0.2.0.md).

## Сборка в Codex Cloud / локально

Полный JDK 21, Android SDK 35 / build-tools 34.0.0, FFmpeg с libx264, шрифт DejaVu Sans. SDK задаётся через `ANDROID_HOME` либо исключённый из Git `local.properties`.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :domain:test :sources:test :parental:testDebugUnitTest
```

Собственное CC0-видео генерируется перед первой сборкой; его SHA-256 встраивается в код и проверяется перед воспроизведением. Генератор: `scripts/generate-demo.sh`, лицензии: [TEST_MEDIA.md](docs/TEST_MEDIA.md). Оригинальные графические ресурсы не заимствованы у фильмов или сайтов.

APK: `app/build/outputs/apk/debug/app-debug.apk`. По умолчанию universal target для mobile/tablet/TV, minSdk 26, targetSdk 35, версия 0.3.0 / versionCode 3. ARM64-сборка: `./gradlew -Parm64Only :app:assembleDebug`. Debug-подпись создаётся локально и не коммитится. Production signing и проверка физических устройств — отдельная работа.

Для proxy в Gradle используйте настройки конкретной среды и её доверенный CA; не отключайте TLS и не коммитьте proxy credentials.

## Android-проверки

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e layout phone \
  -e class ru.familycinema.app.FamilyFlowTest \
  ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
```

Тесты **очищают данные тестируемого приложения** перед каждым методом: выполняйте их на отдельном эмуляторе. `layout` задаёт префикс screenshots и проверку ожидаемого типа/размера устройства; он не подменяет его конфигурацию. Для планшета/TV нужны соответствующие реальные настройки эмулятора/образа. Можно выбрать основной сценарий через `-e class ru.familycinema.app.FamilyFlowTest#nativeFamilyFlowFullscreenRotationAndResume`.

Runner проверяет декодирование, FIT и размер PlayerView, touch и OK, скрытие/возврат контролов, два Back, смену ориентации и пересоздание Activity без смены плеера, позицию, фон, неверный PIN, одобрение/отзыв и блокировку после пяти попыток. Реальные PNG сохраняются в filesDir приложения. Фактические результаты, включая неудачные итерации, см. в QA.

## Доставка APK

```sh
ANDROID_HOME=/path/to/sdk python scripts/prepare-apk-delivery.py \
  --apk app/build/outputs/apk/debug/app-debug.apk \
  --version 0.2.0 --target universal --variant debug
```

Скрипт проверяет подпись и версию, создаёт `artifacts/delivery/family-cinema-v0.2.0-universal-debug.apk`, `SHA256SUMS.txt` и `delivery.json`. Затем APK доставляется разрешённым коннектором Google Drive; успех требует readback размера и SHA-256 либо скачивания и сравнения. Сам скрипт не выдаёт подготовку за успешную загрузку. Приватные Drive ID/ссылки, APK, видео и ключи исключены из Git; реальные PNG интерфейса сохранены в документации. Для проверки скачанного обратно файла есть --verify-roundtrip с наблюдавшимися Drive ID/URL. Фактическая доставка описана в [APK_DELIVERY_STATUS.md](docs/APK_DELIVERY_STATUS.md), QA и ответе владельцу; публичный доступ не включается.

## GitHub Actions

Только ручной `workflow_dispatch`, не push/PR. 15 минут на job, один прогон одновременно, будущие артефакты 7 дней. Сборка и проверки не требуют Actions. Перед новыми ручными прогонами нужно проверять доступные лимиты аккаунта.

## Backend

```sh
python -m venv .venv
.venv/bin/pip install -r backend/requirements.txt
cd backend
../.venv/bin/python -m pytest -q
../.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000
```

FastAPI — каркас: `/health` сообщает `database: not_configured`, `/catalog` возвращает пустой типизированный список. PostgreSQL, серверная авторизация и синхронизация отсутствуют. APK не использует сервер и не имеет INTERNET permission. Интерфейсы Torznab/torrent сохранены, но не реализованы.
