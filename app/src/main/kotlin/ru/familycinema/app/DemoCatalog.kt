package ru.familycinema.app

import ru.familycinema.domain.*

object DemoCatalog : CatalogRepository {
    override fun films() =
        listOf(
            Film(
                "quiet-evening",
                "Тихий вечер",
                "Собственный тестовый ролик: спокойные цвета, движение и тихий звук. Это техническое видео, не результат поиска и не полнометражный фильм. Родитель должен просмотреть его перед одобрением.",
                0,
                "Оригинальное процедурное видео проекта, CC0 1.0",
                VideoVersion("demo-v1", "asset:///quiet-evening.mp4", GeneratedDemo.sha256),
            )
        )
}
