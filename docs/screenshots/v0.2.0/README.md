# Реальные скриншоты 0.2.0

Неизменённые PNG, снятые UiAutomation из финального APK (SHA-256 в [QA](../../../QA.md)). Это работающие экраны эмуляторов, не макеты. Собственный лес/луна — графический ресурс приложения CC0. Контент видео — собственный технический ролик, не лицензированная кинолента.

| Экран | Телефон | Планшет | Android TV |
| --- | --- | --- | --- |
| Создание PIN | [PNG](phone-pin.png) | [PNG](tablet-pin.png) | [PNG](tv-pin.png) |
| Родительская медиатека | [PNG](phone-parent.png) | [PNG](tablet-parent.png) | [PNG](tv-parent.png) |
| Родительская карточка | [PNG](phone-parent-details.png) | [PNG](tablet-parent-details.png) | [PNG](tv-parent-details.png) |
| Подтверждение одобрения | [PNG](phone-approval.png) | [PNG](tablet-approval.png) | [PNG](tv-approval.png) |
| Детский каталог | [PNG](phone-child-catalog.png) | [PNG](tablet-child-catalog.png) | [PNG](tv-child-catalog.png) |
| Детская карточка | [PNG](phone-child-details.png) | [PNG](tablet-child-details.png) | [PNG](tv-child-details.png) |
| Автоматическое скрытие контролов | [PNG](phone-player-autohide.png) | [PNG](tablet-player-autohide.png) | [PNG](tv-player-autohide.png) |
| Контролы воспроизведения | [PNG](phone-player-controls.png) | [PNG](tablet-player-controls.png) | [PNG](tv-player-controls.png) |
| Кадр после Activity recreation | [PNG](phone-player-clean.png) | [PNG](tablet-player-clean.png) | [PNG](tv-player-clean.png) |
| Пустой каталог после отзыва | [PNG](phone-empty.png) | [PNG](tablet-empty.png) | [PNG](tv-empty.png) |
| Блокировка PIN | [PNG](phone-pin-locked.png) | — | — |

Телефон: API 30/default x86_64, Pixel 2 AVD, 720×1280/density 280; плеер горизонтальный. Планшет: тот же AVD с display 1280×800/density 160, реальный двухколоночный Compose layout, без растяжения изображения. TV: отдельный настоящий Android TV API 30/x86, 1920×1080/density 320, режим UI_MODE_TYPE_TELEVISION. Физические устройства не использовались.

Визуальная проверка проводилась по этим изображениям с исправлениями до финальной сдачи; детали и границы проверки — в QA.
