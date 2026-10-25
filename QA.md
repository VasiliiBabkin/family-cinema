# QA — Family Cinema 0.2.0, Player & Native UX

Проверки выполнены в Codex Cloud 2026-10-25, без запуска GitHub Actions. Это проверенный debug-кандидат для испытаний; полная готовность PROJECT_CONTRACT.md и публикация в магазине не заявляются. Исторический отчёт: [0.1.0](docs/qa/v0.1.0.md).

## APK и доставка

- Package `ru.familycinema.app`, versionName **0.2.0**, versionCode **2**.
- Universal mobile/tablet/TV, minSdk 26, target/compileSdk 35. Размер **20 169 357 байт**.
- SHA-256: `9f8d4e7b55908264591f18aad3b109c1d4612e6003a4f7a82a2f7bd0b8ab9080`.
- Локальный артефакт: `artifacts/delivery-final/family-cinema-v0.2.0-universal-debug.apk`; оригинал `app/build/outputs/apk/debug/app-debug.apk`.
- Debug-подпись APK Signature Scheme v2: [проверка](docs/evidence/v0.2.0/apk-signature.txt). Ключ не коммитится; для магазина нужна отдельная release-подпись.
- **Drive PASS**: реальная загрузка в приватную папку, чтение размера/permissions, скачивание полного APK обратно, совпадение размера и SHA-256, действительная подпись. [Readback](docs/evidence/v0.2.0/delivery-readback.txt), [механизм](docs/APK_DELIVERY_STATUS.md). Приватные URL/ID переданы владельцу отдельно и хранятся только в игнорируемом receipt. Только owner permission, публичный доступ не включён. Предыдущий кандидат сохранён как rc1, не удалён.

## Результаты

| Проверка | Результат | Свидетельство |
| --- | --- | --- |
| Финальная сборка app + Android test APK | PASS | [build](docs/evidence/v0.2.0/build.txt) |
| Android Lint | PASS, 0 issues | [Lint](docs/evidence/v0.2.0/android-lint.txt) |
| JVM domain | PASS, 5 | [JUnit](docs/evidence/v0.2.0/TEST-ru.familycinema.domain.ApprovalTest.xml) |
| JVM parental | PASS, 2 | [JUnit](docs/evidence/v0.2.0/TEST-ru.familycinema.parental.PinHasherTest.xml) |
| Backend pytest | PASS, 9 | [JUnit](docs/evidence/v0.2.0/backend-tests.xml), [вывод](docs/evidence/v0.2.0/backend.txt) |
| Телефон, финальный APK | PASS, 2 теста, 354.204 s | [runner](docs/evidence/v0.2.0/phone-instrumentation.txt) |
| Планшет, финальный APK | PASS, 1 тест, 201.706 s | [runner](docs/evidence/v0.2.0/tablet-instrumentation.txt) |
| Android TV, финальный APK | PASS, 1 тест, 230.027 s | [runner](docs/evidence/v0.2.0/tv-instrumentation.txt) |
| Обновление 0.1.0 → 0.2.0 без очистки | PASS: legacy seed 11.968 s, verify 28.596 s | [seed](docs/evidence/v0.2.0/upgrade-legacy-seed.txt), [диск](docs/evidence/v0.2.0/upgrade-disk-checkpoint.txt), [verify](docs/evidence/v0.2.0/upgrade-verify.txt) |
| Повреждённая скачанная копия | PASS: несовпадение отклонено, ложный PASS не создан | [негативная проверка](docs/evidence/v0.2.0/delivery-negative.txt) |

JVM-тесты перезапущены с `--rerun-tasks`: проверяют изменение URI/SHA/эпизода и солёный PIN. Backend имеет одно предупреждение Starlette об устаревшем httpx TestClient, без ошибок. Это каркас: `/health` сообщает `database: not_configured`, `/catalog` пустой; APK от backend не зависит.

## Что проверяет Android-сценарий

`FamilyFlowTest.nativeFamilyFlowFullscreenRotationAndResume`: создание и подтверждение PIN, родительская карточка, подтверждение одобрения, детский каталог/карточка, реальное декодирование 960×540 и движение позиции Media3, fullscreen PlayerView и FIT, автоматическое скрытие всего overlay и настоящий pointer tap для возврата, pause/play и ±10 секунд, смена landscape на телефоне, принудительное пересоздание Activity с тем же объектом ExoPlayer, новый отрисованный кадр и проверка пикселя против чёрного экрана, аппаратные D-pad/OK/Back, освобождение плеера, сохранение и продолжение позиции, восстановление исходной ориентации телефона, пауза при CREATED/RESUMED, неверный PIN, успешный вход, отзыв и пустой каталог.

`pinLockoutAndParentBackgroundProtection`: закрытие родительского сеанса после фона, пять ошибок, блокировка даже правильного PIN. В TV-прогоне дополнительно проверяется настоящий `UI_MODE_TYPE_TELEVISION` и ввод PIN аппаратными Right/Down/Left/Up/OK; планшетный прогон требует smallestScreenWidth >=600 dp. Основной сценарий большинства экранов использует Compose semantics; он не заменяет полный ручной обход всех TV-экранов только пультом.

Фоновый переход проверен через реальный lifecycle ActivityScenario; уничтожение процесса Android системой отдельно не воспроизводилось. Удержание одного ExoPlayer, отвязка поверхности и release на выходе проверены сценариями/кодом; heap-профилирование не выполнено.

Только собственный 40-секундный CC0 H.264 Baseline 960×540/AAC LC: [происхождение](docs/TEST_MEDIA.md). Для медленной программной эмуляции тест явно устанавливает активную позицию 2 s перед проверкой auto-hide; факт декодирования и движения до этого проверяется отдельно.

## Эмуляторы и скриншоты

Скриншоты снимаются `UiAutomation.takeScreenshot()` после idle непосредственно из приложения; концептуальные изображения не используются. Иллюстрация внутри UI — собственная Canvas-графика, не подмена screenshot. [Галерея](docs/screenshots/v0.2.0/README.md).

- Телефон: Android 11/API 30, default x86_64, Pixel 2 AVD, 720×1280, density 280.
- Планшет: тот же AVD с реальной конфигурацией display 1280×800/density 160; Compose рендерит планшетный layout. Это изменение конфигурации эмулятора, не физический планшет и не растяжение PNG.
- TV: отдельный настоящий образ `android-30;android-tv;x86`, профиль tv_1080p, 1920×1080/density 320, UI mode television.
- Без KVM, TCG/SwiftShader, два CPU, 1536 MB на AVD; медленная облачная среда не подходит для оценки FPS. Анимации системы выключены в тестах; плавность на устройствах не измерена.

## Найденные проблемы и исправления

- Исходный PR #2 имел постоянно видимый выход поверх видео и не сохранял исходную ориентацию через пересоздание. Добавлены единый скрываемый слой и WindowPresentation в ViewModel.
- Обращение к Media3 getters во время фонового пересчёта Compose могло нарушать main-thread контракт: UI теперь читает снимок состояния, создание/операции плеера выполняются на main.
- Быстрый Back мог использовать старое состояние overlay: обработчик читает актуальное значение в момент события.
- Новый surface после paused Activity recreation давал чёрный кадр: SurfaceHolder callback выполняет точный seek текущей позиции без создания второго плеера. Тест проверяет новый renderedFrame и пиксель.
- На настоящем TV-образе таймер overlay в Compose не срабатывал при нативном playing=true: таймер перенесён в viewModelScope, scrubbing удерживает контролы, native snapshot синхронизируется listener/ticker.
- Визуальная проверка реальных PNG выявила слишком длинный PIN-экран телефона, повтор заголовка, невыразительную палитру клавиатуры и прокрутку верхней части TV-деталей к начальному фокусу. PIN уплотнён с сохранением крупных целей, цвета согласованы, TV-описание сокращено с раскрытием подробностей, постер вынесен из прокрутки.
- Одновременная заключительная загрузка двух AVD упёрлась в лимит памяти облачной среды и вызвала ANR System UI. Прогон прерван, TV остановлен, окончательные сценарии выполнены последовательно на прогретом эмуляторе; прерванный лог не учитывается как PASS.
- Первая проверка обновления обнаружила позицию 0 при сохранённых PIN/одобрении. Для проверки исходной fixture добавлен commit-barrier и чтение позиции с диска перед установкой обновления; повторная проверка подтвердила 12345 ms на диске до обновления и сохранность PIN, одобрения и позиции после него. Это был дефект подготовки тестовых данных, а не подтверждённая потеря позиции при обновлении.

Неудачные итерации не учитываются как PASS. Их логи сохранены в `artifacts/v02-*`; в публичном evidence опубликованы результаты финального APK.

## Воспроизведение проверок

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :domain:test :parental:testDebugUnitTest --rerun-tasks
cd backend
../.venv/bin/python -m pytest -q --junitxml=../artifacts/v02-backend.xml
# Из корня, на выделенном тестовом эмуляторе:
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w -e layout phone \
  -e class ru.familycinema.app.FamilyFlowTest \
  ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
```

Тесты FamilyFlow **очищают данные** перед каждым методом. Для tablet/tv выбрать соответствующую конфигурацию и `-e layout tablet|tv`; основной метод можно выбрать через `Class#method`. UpgradeSeedTest запускается отдельно на подписанном 0.1.0, затем 0.2.0 устанавливается с `adb install -r` без очистки и запускается UpgradeVerifyTest. Оба APK должны иметь один сертификат. Не запускайте все классы тестового пакета вперемешку: upgrade fixture предназначена для двух разных установленных версий.

Среда: полный JDK 21.0.8, Gradle 8.10.2, AGP 8.7.3, Kotlin 2.0.21, Android SDK 35/build-tools 34.0.0, FFmpeg libx264/DejaVu. Proxy и доверенный CA задавались параметрами среды; TLS не отключался. GitHub Actions в этой задаче не запускались; существующий workflow остаётся только manual dispatch.

## Объективные ограничения

- Один демонстрационный ролик, без настоящего каталога фильмов, поиска, P2P, torrent engine и серверной синхронизации. Будущие интерфейсы сохранены.
- Физические телефоны, планшеты, Android TV/Google TV, OEM-пульты, TalkBack, разные font scales, HDR/HEVC/DRM/субтитры/аудиодорожки и аппаратные декодеры не проверены. Упакованные ABI не означают испытание на ARM.
- Нет измерений производительности/heap и широкого OEM/API-матрицы; проигрывание длительных фильмов не испытано.
- Ошибки SHA/декодирования имеют русские экраны и выход, но fault injection на финальном APK не проводился. Аудиофокус/noisy реализованы Media3, внешними звонками/наушниками не испытаны.
- Несколько профилей, временные лимиты, восстановление PIN, Picture-in-Picture, background audio/casting не реализованы. PIN не защищает от root, очистки данных и разрешённого ADB к debug APK.
- Production-подпись, store release hardening и автоматический headless OAuth uploader отсутствуют. Реальная доставка через подключённый Drive выполнена без сохранения токенов.
