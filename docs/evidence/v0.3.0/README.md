# Проверки 0.3.0

Итоговая сводка: [test-summary.json](test-summary.json). Интерпретация, матрица десяти критериев и ограничения — в [QA](../../../QA.md). Снимки: [49 исходных PNG](../../screenshots/v0.3.0/README.md).

## Актуальные результаты

| Область | Логи |
| --- | --- |
| Телефон, 3 новых UI-сценария | [phone-final-flow.txt](phone-final-flow.txt) |
| Телефон, PIN/плеер, 2 регрессии | [phone-final-player.txt](phone-final-player.txt) |
| Планшет, 3 новых UI-сценария | [tablet-final-flow.txt](tablet-final-flow.txt) |
| Планшет, плеер | [tablet-final-player.txt](tablet-final-player.txt) |
| TV, медиатека/контроль/видео | [tv-final-main.txt](tv-final-main.txt) |
| TV, разрешённые эпизоды | [tv-final-series.txt](tv-final-series.txt) |
| TV, плеер/D-pad | [tv-final-player.txt](tv-final-player.txt) |
| TV, системный URL editor | [tv-source-editor-regression.txt](tv-source-editor-regression.txt) |
| TV, длинная карточка/resume без обрезания | [tv-card-fit.txt](tv-card-fit.txt) |
| Настоящий 0.2 APK, seed | [phone-final-upgrade-seed.txt](phone-final-upgrade-seed.txt) |
| Итоговый 0.3 APK, update verify | [phone-final-upgrade-verify.txt](phone-final-upgrade-verify.txt) |
| Room migration/policy/hash tamper/progress | [phone-final-room.txt](phone-final-room.txt) |
| JVM, все задачи повторены без up-to-date пропуска | [jvm-tests.log](jvm-tests.log), TEST-*.xml |
| Вспомогательный backend | [backend-tests.txt](backend-tests.txt), [XML](backend-tests.xml) |
| Сборка/Lint | [android-build.log](android-build.log), [android-lint.txt](android-lint.txt) |
| APK подпись/ABI/суммы | [подпись](apk-signature.txt), [ABI](abi-validation.txt), [SHA256SUMS](apk-SHA256SUMS.txt) |
| Реальный Drive download/readback | [ARM64](apk-drive-readback.txt), [universal](apk-universal-drive-readback.txt) |
| Linux JNI torrent experiment, вне APK | [torrent-native-probe.txt](torrent-native-probe.txt) |

Native итог: 18 успешных выполнений сценариев в нескольких конфигурациях, не 18 различных тестовых методов. TV workflow/source/player проверены до последней чисто визуальной правки высоты TV tiles; финальная геометрия проверена отдельным measured-bounds тестом. Телефон/планшет/обновление/Room используют итоговые bytes. Физический ARM64 runtime не проверен.

Другие файлы в этой папке — история предыдущих кандидатов и значимых неудачных итераций. В частности, FocusRequester/Compose TV URL-field failures сохранены; их не выдают за успешные запуски. Live protocol XML проверяет один собственный hosted fixture через фиксированный proxy-transport JVM, не production Android networking.

## Повторение

Тесты очищают PIN/библиотеку: использовать отдельный QA-эмулятор. Установить universal APK и test APK одной сборки.

```sh
adb -s SERIAL shell am instrument -w -e layout phone \
  -e class ru.familycinema.app.FlowV03Test \
  ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s SERIAL shell am instrument -w -e layout phone \
  -e class ru.familycinema.app.FamilyFlowTest \
  ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s SERIAL shell am instrument -w \
  -e class ru.familycinema.app.V03DataTest \
  ru.familycinema.app.test/androidx.test.runner.AndroidJUnitRunner
```

`layout=tablet` требует display smallest width ≥600 dp, `layout=tv` — настоящий TV system image/uiMode. Параметр не превращает телефон в TV. Для TV layout отдельно запускается `ru.familycinema.app.TvLayoutTest`. Для планшета достаточно основного метода FamilyFlowTest.

Обновление проверяется отдельной последовательностью: настоящий 0.2 APK → UpgradeSeedTest → проверка диска → install -r итогового 0.3 APK без очистки данных → UpgradeVerifyTest. У 0.2 не было Room; остаточную DB от предыдущего QA очищать только на disposable эмуляторе ДО seed. Не запускать все test classes без выбора.

PNG сняты UiAutomation.takeScreenshot и прочитаны adb exec-out run-as, без редактирования. Размеры и SHA-256 каждого снимка: [screenshots.json](screenshots.json). Приватные Drive URL/ID/ключи в evidence не публикуются.
