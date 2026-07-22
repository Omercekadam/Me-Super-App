"""Aktivite analizleri: Steam oynama farkı, tür kırılımı, haftalık oyun/çalışma dengesi."""

from datetime import date, timedelta


def steam_playtime_diff(payload):
    """Steam'in kümülatif playtime_forever anlık görüntülerinden günlük fark (dakika) hesaplar.

    payload = {
        "snapshots": [{"steamAppId": int, "date": "YYYY-MM-DD", "playtimeForeverMin": int}, ...]
    }
    İlk anlık görüntü taban alınır, fark üretmez. Negatif fark (ör. hesap taşınması) 0'a kırpılır.
    """
    by_game = {}
    for snap in payload.get("snapshots") or []:
        by_game.setdefault(snap["steamAppId"], []).append(snap)

    results = []
    for app_id, snaps in by_game.items():
        ordered = sorted(snaps, key=lambda s: s["date"])
        for prev, cur in zip(ordered, ordered[1:]):
            diff = cur["playtimeForeverMin"] - prev["playtimeForeverMin"]
            results.append({
                "steamAppId": app_id,
                "date": cur["date"],
                "minutesPlayed": max(diff, 0),
            })
    return results


def genre_breakdown(payload):
    """Bir tarih aralığında tür bazlı toplam oynama süresi (dakika) kırılımı.

    payload = {
        "from": "YYYY-MM-DD", "to": "YYYY-MM-DD",
        "games": [{"id": int, "genres": "Aksiyon, RPG"}, ...],
        "manualLogs": [{"gameId": int, "date": "YYYY-MM-DD", "minutes": int}, ...],
        "steamMinutes": [{"steamAppId": int, "date": "YYYY-MM-DD", "minutesPlayed": int}, ...],
        "gameSteamAppIds": {"<gameId>": steamAppId, ...}
    }
    Çok türlü oyunların süresi türleri arasında eşit paylaştırılır.
    """
    date_from = date.fromisoformat(payload["from"])
    date_to = date.fromisoformat(payload["to"])
    games_by_id = {g["id"]: g for g in payload.get("games") or []}
    app_to_game = {v: int(k) for k, v in (payload.get("gameSteamAppIds") or {}).items()}

    minutes_by_game = {}
    for log in payload.get("manualLogs") or []:
        d = date.fromisoformat(log["date"])
        if date_from <= d <= date_to:
            minutes_by_game[log["gameId"]] = minutes_by_game.get(log["gameId"], 0) + log["minutes"]

    for entry in payload.get("steamMinutes") or []:
        d = date.fromisoformat(entry["date"])
        if date_from <= d <= date_to:
            game_id = app_to_game.get(entry["steamAppId"])
            if game_id is not None:
                minutes_by_game[game_id] = minutes_by_game.get(game_id, 0) + entry["minutesPlayed"]

    genre_minutes = {}
    for game_id, minutes in minutes_by_game.items():
        game = games_by_id.get(game_id)
        if not game:
            continue
        genres = [g.strip() for g in (game.get("genres") or "").split(",") if g.strip()] or ["Diğer"]
        share = minutes / len(genres)
        for genre in genres:
            genre_minutes[genre] = genre_minutes.get(genre, 0) + share

    total = sum(genre_minutes.values())
    return [
        {
            "genre": genre,
            "minutes": round(minutes),
            "ratio": round(minutes / total, 4) if total else 0.0,
        }
        for genre, minutes in sorted(genre_minutes.items(), key=lambda kv: -kv[1])
    ]


def weekly_balance(payload):
    """Son 7 gün oyun (dakika) vs pomodoro/çalışma (dakika) dengesi.

    payload = {
        "today": "YYYY-MM-DD",
        "gameMinutesByDate": {"YYYY-MM-DD": int, ...},
        "pomodoroSessions": [{"date": "YYYY-MM-DD", "durationMin": int, "completed": bool}, ...]
    }
    """
    today = date.fromisoformat(payload["today"])
    week_start = today - timedelta(days=6)

    game_minutes = sum(
        minutes for d_str, minutes in (payload.get("gameMinutesByDate") or {}).items()
        if week_start <= date.fromisoformat(d_str) <= today
    )
    work_minutes = sum(
        s["durationMin"] for s in (payload.get("pomodoroSessions") or [])
        if s.get("completed") and week_start <= date.fromisoformat(s["date"]) <= today
    )

    total = game_minutes + work_minutes
    return {
        "gameMinutes": game_minutes,
        "workMinutes": work_minutes,
        "gameRatio": round(game_minutes / total, 4) if total else 0.0,
        "workRatio": round(work_minutes / total, 4) if total else 0.0,
    }
