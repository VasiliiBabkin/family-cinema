"""Russian-first title matching primitives. No network calls or tracker access."""
from __future__ import annotations

import re
import unicodedata

_TRANSLIT = {
    "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "е": "e",
    "ё": "e", "ж": "zh", "з": "z", "и": "i", "й": "y", "к": "k",
    "л": "l", "м": "m", "н": "n", "о": "o", "п": "p", "р": "r",
    "с": "s", "т": "t", "у": "u", "ф": "f", "х": "kh", "ц": "ts",
    "ч": "ch", "ш": "sh", "щ": "shch", "ъ": "", "ы": "y",
    "ь": "", "э": "e", "ю": "yu", "я": "ya",
}
_CYRILLIC = re.compile("[а-яё]")


def normalize_title(value: str) -> str:
    """Normalize case, ё, punctuation, whitespace and Unicode width."""
    value = unicodedata.normalize("NFKC", value).casefold().replace("ё", "е")
    value = re.sub(r"[^\w]+", " ", value, flags=re.UNICODE)
    return " ".join(value.split())


def transliterate_ru(value: str) -> str:
    """Stable Cyrillic -> Latin search key; not a reversible transliteration."""
    value = normalize_title(value)
    return "".join(_TRANSLIT.get(char, char) for char in value)


def search_keys(title: str, aliases: tuple[str, ...] = ()) -> tuple[str, ...]:
    """Generate de-duplicated search keys, preserving original order."""
    result: list[str] = []
    for value in (title, *aliases):
        normalized = normalize_title(value)
        if not normalized:
            continue
        for key in (normalized, transliterate_ru(normalized)):
            if key and key not in result:
                result.append(key)
    return tuple(result)


def title_matches(query: str, title: str, aliases: tuple[str, ...] = ()) -> bool:
    """Conservative exact/prefix/token matching; not fuzzy ranking."""
    normalized = normalize_title(query)
    if not normalized:
        return False
    keys = search_keys(title, aliases)
    return any(key == normalized or key.startswith(normalized + " ") for key in keys)
