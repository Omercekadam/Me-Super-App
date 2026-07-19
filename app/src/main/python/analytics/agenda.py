"""Ajanda analizleri: alışkanlık zinciri, pomodoro istatistikleri, GitHub commit streak'i."""

from datetime import date, timedelta


def _longest_streak(active_dates):
    """Verilen tarih listesindeki en uzun kesintisiz gün zincirini hesaplar."""
    if not active_dates:
        return 0

    dates = sorted(active_dates)
    longest = 1
    run = 1
    for prev, cur in zip(dates, dates[1:]):
        if cur == prev + timedelta(days=1):
            run += 1
        elif cur == prev:
            continue  # yinelenen tarih (olmamalı ama savunmacı)
        else:
            run = 1
        longest = max(longest, run)

    return longest


def _current_streak(active_dates, today):
    date_set = set(active_dates)
    if today in date_set:
        anchor = today
    elif today - timedelta(days=1) in date_set:
        anchor = today - timedelta(days=1)
    else:
        return 0

    streak = 0
    cursor = anchor
    while cursor in date_set:
        streak += 1
        cursor -= timedelta(days=1)
    return streak


def habit_streaks(payload):
    """Her alışkanlık için mevcut/en uzun zincir ve bugün durumu.

    payload = {
        "today": "2026-07-19",
        "habits": [{"id": int, "name": str}, ...],
        "ticks": [{"habitId": int, "date": "YYYY-MM-DD"}, ...]
    }
    """
    today = date.fromisoformat(payload["today"])
    by_habit = {}
    for tick in payload.get("ticks") or []:
        by_habit.setdefault(tick["habitId"], []).append(date.fromisoformat(tick["date"]))

    results = []
    for habit in payload.get("habits") or []:
        habit_dates = by_habit.get(habit["id"], [])
        longest = _longest_streak(habit_dates)
        current = _current_streak(habit_dates, today)
        results.append({
            "habitId": habit["id"],
            "name": habit["name"],
            "currentStreak": current,
            "longestStreak": max(longest, current),
            "tickedToday": today in set(habit_dates),
        })
    return results


def pomodoro_stats(payload):
    """Bugün/bu hafta/toplam pomodoro istatistikleri.

    payload = {
        "today": "2026-07-19",
        "sessions": [{"date": "YYYY-MM-DD", "durationMin": int, "completed": bool}, ...]
    }
    """
    today = date.fromisoformat(payload["today"])
    week_start = today - timedelta(days=6)
    sessions = payload.get("sessions") or []

    today_sessions = [s for s in sessions if date.fromisoformat(s["date"]) == today]
    week_sessions = [s for s in sessions if week_start <= date.fromisoformat(s["date"]) <= today]
    completed_count = sum(1 for s in sessions if s.get("completed"))

    return {
        "todayMinutes": sum(s["durationMin"] for s in today_sessions),
        "todaySessions": len(today_sessions),
        "weekMinutes": sum(s["durationMin"] for s in week_sessions),
        "weekSessions": len(week_sessions),
        "totalMinutes": sum(s["durationMin"] for s in sessions),
        "totalSessions": len(sessions),
        "completionRate": round(completed_count / len(sessions), 4) if sessions else 0.0,
    }


def github_streak(payload):
    """GitHub günlük commit önbelleğinden streak + bu yılki toplam.

    payload = {
        "today": "2026-07-19",
        "days": [{"date": "YYYY-MM-DD", "count": int}, ...]
    }
    """
    today = date.fromisoformat(payload["today"])
    active_dates = [date.fromisoformat(d["date"]) for d in (payload.get("days") or []) if d["count"] > 0]

    total_this_year = sum(
        d["count"] for d in (payload.get("days") or [])
        if date.fromisoformat(d["date"]).year == today.year
    )

    longest = _longest_streak(active_dates)
    current = _current_streak(active_dates, today)
    return {
        "currentStreak": current,
        "longestStreak": max(longest, current),
        "totalThisYear": total_this_year,
    }
