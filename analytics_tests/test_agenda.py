from analytics.agenda import github_streak, habit_streaks, pomodoro_stats


def tick(habit_id, d):
    return {"habitId": habit_id, "date": d}


# --- habit_streaks ---

def test_habit_streak_ticked_today_counts_consecutive_days():
    payload = {
        "today": "2026-07-19",
        "habits": [{"id": 1, "name": "Kitap oku"}],
        "ticks": [
            tick(1, "2026-07-17"),
            tick(1, "2026-07-18"),
            tick(1, "2026-07-19"),
        ],
    }
    result = habit_streaks(payload)[0]
    assert result["currentStreak"] == 3
    assert result["longestStreak"] == 3
    assert result["tickedToday"] is True


def test_habit_streak_alive_when_yesterday_ticked_but_not_today():
    payload = {
        "today": "2026-07-19",
        "habits": [{"id": 1, "name": "Spor"}],
        "ticks": [tick(1, "2026-07-18"), tick(1, "2026-07-17")],
    }
    result = habit_streaks(payload)[0]
    assert result["currentStreak"] == 2
    assert result["tickedToday"] is False


def test_habit_streak_broken_resets_to_zero():
    payload = {
        "today": "2026-07-19",
        "habits": [{"id": 1, "name": "Meditasyon"}],
        "ticks": [tick(1, "2026-07-10")],  # cok eski, zincir kopmus
    }
    result = habit_streaks(payload)[0]
    assert result["currentStreak"] == 0
    assert result["longestStreak"] == 1


def test_habit_streak_longest_survives_a_past_break():
    payload = {
        "today": "2026-07-19",
        "habits": [{"id": 1, "name": "Su ic"}],
        "ticks": [
            tick(1, "2026-07-01"), tick(1, "2026-07-02"), tick(1, "2026-07-03"),
            tick(1, "2026-07-04"), tick(1, "2026-07-05"),  # 5 gunluk eski zincir
            tick(1, "2026-07-19"),  # bugun tek basina
        ],
    }
    result = habit_streaks(payload)[0]
    assert result["longestStreak"] == 5
    assert result["currentStreak"] == 1


def test_habit_streak_no_ticks():
    payload = {"today": "2026-07-19", "habits": [{"id": 1, "name": "Yeni"}], "ticks": []}
    result = habit_streaks(payload)[0]
    assert result == {
        "habitId": 1, "name": "Yeni", "currentStreak": 0, "longestStreak": 0, "tickedToday": False,
    }


# --- pomodoro_stats ---

def session(d, minutes=25, completed=True):
    return {"date": d, "durationMin": minutes, "completed": completed}


def test_pomodoro_stats_today_and_week():
    payload = {
        "today": "2026-07-19",
        "sessions": [
            session("2026-07-19", 25),
            session("2026-07-19", 25),
            session("2026-07-15", 50),
            session("2026-07-01", 25),  # haftanin disinda
        ],
    }
    result = pomodoro_stats(payload)
    assert result["todayMinutes"] == 50
    assert result["todaySessions"] == 2
    assert result["weekMinutes"] == 100
    assert result["weekSessions"] == 3
    assert result["totalMinutes"] == 125
    assert result["totalSessions"] == 4
    assert result["completionRate"] == 1.0


def test_pomodoro_stats_completion_rate():
    payload = {
        "today": "2026-07-19",
        "sessions": [
            session("2026-07-19", 25, completed=True),
            session("2026-07-19", 10, completed=False),
        ],
    }
    assert pomodoro_stats(payload)["completionRate"] == 0.5


def test_pomodoro_stats_empty():
    result = pomodoro_stats({"today": "2026-07-19", "sessions": []})
    assert result["todayMinutes"] == 0
    assert result["completionRate"] == 0.0


# --- github_streak ---

def gh(d, count):
    return {"date": d, "count": count}


def test_github_streak_current_and_total():
    payload = {
        "today": "2026-07-19",
        "days": [
            gh("2026-07-17", 2), gh("2026-07-18", 1), gh("2026-07-19", 3),
            gh("2026-01-01", 5),
        ],
    }
    result = github_streak(payload)
    assert result["currentStreak"] == 3
    assert result["totalThisYear"] == 11


def test_github_streak_broken_chain():
    payload = {
        "today": "2026-07-19",
        "days": [gh("2026-07-10", 1)],
    }
    result = github_streak(payload)
    assert result["currentStreak"] == 0
    assert result["longestStreak"] == 1
