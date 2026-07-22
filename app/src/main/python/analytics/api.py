"""Kotlin <-> Python koprusunun tek giris noktasi.

Sozlesme: run(fn, payload_json) -> result_json
- fn: "finance.summary" gibi noktali fonksiyon adi
- payload/result: JSON string (UTF-8, ensure_ascii=False)

Bu modul Android'e gomulu calisir ama saf Python'dur;
ayni kod masaustunde pytest ile test edilir.
"""

import json

from . import activity, agenda, finance


def _ping(payload):
    import pandas
    return {"status": "ok", "engine": f"pandas {pandas.__version__}"}


_HANDLERS = {
    "ping": _ping,
    "finance.summary": finance.summary,
    "finance.forecast": finance.forecast,
    "finance.insights": finance.insights,
    "agenda.habitStreaks": agenda.habit_streaks,
    "agenda.pomodoroStats": agenda.pomodoro_stats,
    "agenda.githubStreak": agenda.github_streak,
    "activity.steamPlaytimeDiff": activity.steam_playtime_diff,
    "activity.genreBreakdown": activity.genre_breakdown,
    "activity.weeklyBalance": activity.weekly_balance,
}


def run(fn: str, payload_json: str) -> str:
    payload = json.loads(payload_json)
    try:
        handler = _HANDLERS[fn]
    except KeyError:
        raise ValueError(f"Bilinmeyen analiz fonksiyonu: {fn!r}")
    return json.dumps(handler(payload), ensure_ascii=False)
