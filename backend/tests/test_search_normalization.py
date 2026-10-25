from app.search_normalization import normalize_title, search_keys, title_matches, transliterate_ru


def test_normalize_yo_and_punctuation():
    assert normalize_title("  Ёжик:   в  тумане! ") == "ежик в тумане"


def test_unicode_width_and_case():
    assert normalize_title("ＦＩＬＭ 2026") == "film 2026"


def test_transliteration():
    assert transliterate_ru("Ёжик в тумане") == "ezhik v tumane"


def test_aliases_and_deduplication():
    assert search_keys("Тест", ("Тест", "Test")) == ("тест", "test")


def test_russian_prefix_and_latin_transliteration():
    assert title_matches("ежик", "Ёжик в тумане")
    assert title_matches("ezhik", "Ёжик в тумане")
    assert title_matches("hedgehog", "Ёжик в тумане", ("Hedgehog in the Fog",))


def test_no_substring_or_empty_match():
    assert not title_matches("", "Ёжик в тумане")
    assert not title_matches("жик", "Ёжик в тумане")
    assert not title_matches("кино", "Ёжик в тумане")
