# Доставка 0.3.0

Оба итоговых Cloud APK обновлены в согласованной приватной папке Family Cinema. Публичный доступ не включался, metadata показывает только owner. Private Drive URL/ID содержатся только в игнорируемых receipts `artifacts/delivery-v03/delivery.json` и `artifacts/delivery-v03-universal/delivery.json` и сообщаются владельцу непосредственно.

| APK | Размер, байт | SHA-256 |
| --- | ---: | --- |
| ARM64 debug | 21216663 | `bf76c476b0c232193c385a9eac99dcb74684beb1ff202473aa17232cb1e9c719` |
| Universal debug | 21266736 | `d1c4241571e5a54ac23f74caaa0295acadc04ea7f0c5ed3b698c81d1249fe5a4` |

VersionName 0.3.0 / code 3; apksigner PASS. Сертификат совпадает с настоящим APK 0.2.0. ARM64 содержит только arm64-v8a native libraries; universal — arm64-v8a, armeabi-v7a, x86, x86_64. DEX обоих APK идентичны. Упаковка ABI не равна проверке физического ARM64 runtime.

Для каждого файла выполнены: замена bytes → metadata/permissions readback → настоящее скачивание → совпадение размера и SHA-256. Размер сам по себе не принимался за доказательство. Старые кандидатные bytes заменены, ссылки сохранены. [ARM64 readback](evidence/v0.3.0/apk-drive-readback.txt), [universal readback](evidence/v0.3.0/apk-universal-drive-readback.txt), [подпись](evidence/v0.3.0/apk-signature.txt), [суммы](evidence/v0.3.0/apk-SHA256SUMS.txt), [ABI](evidence/v0.3.0/abi-validation.txt).

Статус функций/испытаний — в [QA](../QA.md). Это debug APK для испытаний, не Play/production signing. Для следующих обновлений нужен тот же signing key либо корректная ротация. Ключи не публикуются в репозитории. GitHub Actions не использовались.
