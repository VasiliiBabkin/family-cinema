# Выбор открытых решений для 0.3.0

Проверка GitHub releases/license API выполнена 2026-10-25; версии фиксированы, `+`/dynamic versions не используются. У SDK без интеграции в APK указан именно результат исследования.

| Решение | Версия | Лицензия / условия | Решение |
| --- | --- | --- | --- |
| Kotlin / Compose compiler | 2.0.21 | Apache-2.0 | Сохранены |
| Compose BOM | 2024.12.01 | Apache-2.0 | Нативные компоненты/фокус, сохранён стиль 0.2 |
| AndroidX Media3 | 1.5.1 | Apache-2.0 | Существующий проверенный плеер сохранён |
| AndroidX Activity Compose | 1.9.3 | Apache-2.0 | Activity/Compose integration |
| AndroidX Lifecycle | 2.8.7 | Apache-2.0 | ViewModel и lifecycle-aware UI |
| kotlinx.coroutines | 1.9.0 | Apache-2.0 | Отмена foreground операций, Room и состояние |
| Room | 2.7.2 | Apache-2.0 | Локальная БД, DAO, транзакции, экспорт схем и миграции |
| KSP | 2.0.21-1.0.28 | Apache-2.0 | Генерация Room без собственного SQLite ORM |
| OkHttp | 4.12.0 | Apache-2.0 | HTTPS, проверяющий DNS-resolver, запрет redirect, таймауты и лимиты |
| kotlinx.serialization JSON | 1.7.3 | Apache-2.0 | Разбор недоверенного addon JSON |
| Coil Compose | 2.7.0 | Apache-2.0 | Показ приватно закешированных обложек, ограниченный cache |
| Stremio Addon SDK / protocol | SDK npm 1.6.10 (GitHub release v1.1.4), опубликованный protocol.md | SDK MIT | Собственный Kotlin-клиент открытого JSON-протокола catalog/meta/stream/subtitles; Node.js SDK не встроен |
| Jellyfin Kotlin SDK | v1.9.3 | LGPL-3.0 | Исследован, в APK не включён. Только будущий необязательный источник; LAN/логин/сервер не требуются основной библиотеке |
| TMDB API | Hosted API, не библиотека | API key, атрибуция и проверка условий коммерческого использования | Не включён: ключ не предоставлен, официальная FAQ в этой среде вернула policy 403. Текущие условия этим проходом не подтверждены; чужие TMDB-постеры не встроены |
| libtorrent | v2.1.2 | BSD-3-Clause + bundled dependency notices | Поддерживаемый C++ движок, не писать свой протокол; в APK пока не включён |
| FrostWire jlibtorrent | 2.0.12.9 | Java MIT, native libtorrent BSD-3-Clause | Опубликованы Android ARM/ARM64/x86/x86_64 jars; выполнен настоящий отдельный Linux JNI-прототип. Android runtime/пиры/seek/cache не подтверждены, поэтому не включён как готовое воспроизведение |
| libtorrent4j | v2.1.0-39 | MIT (Alden Torres + FrostWire); файл прочитан, несмотря на NOASSERTION в API | Альтернатива исследована; не включена, без неподтверждённого заявления о лицензии |

GPL-код приложений-оболочек не копировался. Зависимости Java/Android загружаются из Google Maven/Maven Central, бинарные JNI SDK в Git/APK не добавлялись. Обновлённый Kotlin stdlib 2.1.10 приходит транзитивно от Room; точное дерево сохранено в build evidence.

## Источники проверки

- https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md
- https://github.com/jellyfin/jellyfin-sdk-kotlin/releases/tag/v1.9.3
- https://developer.themoviedb.org/docs/faq — доступ в Cloud заблокирован, требуется повторное чтение перед интеграцией.
- https://github.com/arvidn/libtorrent/blob/v2.1.2/LICENSE
- https://github.com/frostwire/frostwire-jlibtorrent/releases/tag/release%2F2.0.12.9
- https://github.com/frostwire/frostwire-jlibtorrent/blob/master/LICENSE.md
- https://github.com/aldenml/libtorrent4j/releases/tag/v2.1.0-39

## Почему torrent ещё не в APK

Работающая проверка загрузки JNI/metadata/start/pause/stop находится в `tools/torrent-probe`; результат — в evidence. Для выпуска нужны ARM64 runtime, 16 KB pages, Android lifecycle, разрешённый torrent без опасных tracker/web-seed URL, размер/путь файлов, приоритеты частей и seek, disk quota, удаление/остановка, тесты с реальными пирами. В Cloud отсутствуют разрешённые TCP destinations для P2P; успешный Linux offline-probe не доказывает эти свойства. В UI торрент-результат явно помечен недоступным, кнопки воспроизведения нет.

## Инструменты и вспомогательный backend

| Зависимость | Версия | Лицензия / область |
| --- | --- | --- |
| JUnit | 4.13.2 | EPL-1.0, только тесты |
| AndroidX Test runner / ext JUnit | 1.6.2 / 1.2.1 | Apache-2.0, test APK |
| Espresso core | 3.5.0 | Apache-2.0, только test APK; зафиксирована версия уже используемого Compose test runtime. Lint сообщает о доступном обновлении 3.5.1 |
| MockWebServer | 4.12.0 | Apache-2.0, JVM-тесты |
| FastAPI | 0.162.0 | MIT, отдельный backend |
| Uvicorn | 0.54.0 | BSD-3-Clause, отдельный backend |
| Pydantic | 2.14.1 | MIT, отдельный backend |
| HTTPX | 0.28.1 | BSD-3-Clause, backend/tests |
| pytest | 9.1.1 | MIT, backend tests |

Точное resolved дерево Android-зависимостей: [android-dependencies.txt](evidence/v0.3.0/android-dependencies.txt). FFmpeg/libx264 используется как отдельный установленный инструмент генерации собственного тестового видео, не линкуется и не включается в APK. Это не внедрение GPL-кода в приложение.
