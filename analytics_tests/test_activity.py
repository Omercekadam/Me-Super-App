from analytics.activity import genre_breakdown, steam_playtime_diff, weekly_balance


def snap(app_id, d, mins):
    return {"steamAppId": app_id, "date": d, "playtimeForeverMin": mins}


# --- steam_playtime_diff ---

def test_steam_playtime_diff_consecutive_days():
    payload = {
        "snapshots": [
            snap(10, "2026-07-17", 100),
            snap(10, "2026-07-18", 160),
            snap(10, "2026-07-19", 160),
        ]
    }
    result = steam_playtime_diff(payload)
    assert result == [
        {"steamAppId": 10, "date": "2026-07-18", "minutesPlayed": 60},
        {"steamAppId": 10, "date": "2026-07-19", "minutesPlayed": 0},
    ]


def test_steam_playtime_diff_negative_clipped_to_zero():
    payload = {
        "snapshots": [
            snap(10, "2026-07-17", 500),
            snap(10, "2026-07-18", 300),  # hesap tasindi/sifirlandi
        ]
    }
    result = steam_playtime_diff(payload)
    assert result[0]["minutesPlayed"] == 0


def test_steam_playtime_diff_single_snapshot_produces_nothing():
    payload = {"snapshots": [snap(10, "2026-07-17", 100)]}
    assert steam_playtime_diff(payload) == []


def test_steam_playtime_diff_multiple_games_independent():
    payload = {
        "snapshots": [
            snap(1, "2026-07-17", 0), snap(1, "2026-07-18", 30),
            snap(2, "2026-07-17", 100), snap(2, "2026-07-18", 110),
        ]
    }
    result = steam_playtime_diff(payload)
    assert len(result) == 2
    by_app = {r["steamAppId"]: r["minutesPlayed"] for r in result}
    assert by_app == {1: 30, 2: 10}


# --- genre_breakdown ---

def test_genre_breakdown_manual_and_steam_combined():
    payload = {
        "from": "2026-07-13", "to": "2026-07-19",
        "games": [
            {"id": 1, "genres": "RPG"},
            {"id": 2, "genres": "Yarış"},
        ],
        "manualLogs": [{"gameId": 1, "date": "2026-07-15", "minutes": 60}],
        "steamMinutes": [{"steamAppId": 99, "date": "2026-07-16", "minutesPlayed": 40}],
        "gameSteamAppIds": {"2": 99},
    }
    result = genre_breakdown(payload)
    by_genre = {r["genre"]: r["minutes"] for r in result}
    assert by_genre == {"RPG": 60, "Yarış": 40}
    ratios = {r["genre"]: r["ratio"] for r in result}
    assert ratios["RPG"] == 0.6
    assert ratios["Yarış"] == 0.4


def test_genre_breakdown_splits_multi_genre_evenly():
    payload = {
        "from": "2026-07-01", "to": "2026-07-31",
        "games": [{"id": 1, "genres": "Aksiyon, RPG"}],
        "manualLogs": [{"gameId": 1, "date": "2026-07-10", "minutes": 100}],
        "steamMinutes": [],
        "gameSteamAppIds": {},
    }
    result = genre_breakdown(payload)
    minutes = {r["genre"]: r["minutes"] for r in result}
    assert minutes == {"Aksiyon": 50, "RPG": 50}


def test_genre_breakdown_ignores_entries_outside_range():
    payload = {
        "from": "2026-07-13", "to": "2026-07-19",
        "games": [{"id": 1, "genres": "RPG"}],
        "manualLogs": [{"gameId": 1, "date": "2026-01-01", "minutes": 999}],
        "steamMinutes": [],
        "gameSteamAppIds": {},
    }
    assert genre_breakdown(payload) == []


def test_genre_breakdown_empty():
    payload = {"from": "2026-07-13", "to": "2026-07-19", "games": [], "manualLogs": [], "steamMinutes": [], "gameSteamAppIds": {}}
    assert genre_breakdown(payload) == []


# --- weekly_balance ---

def test_weekly_balance_ratio():
    payload = {
        "today": "2026-07-19",
        "gameMinutesByDate": {"2026-07-18": 60, "2026-07-19": 60, "2026-01-01": 999},
        "pomodoroSessions": [
            {"date": "2026-07-19", "durationMin": 120, "completed": True},
            {"date": "2026-07-19", "durationMin": 25, "completed": False},
        ],
    }
    result = weekly_balance(payload)
    assert result["gameMinutes"] == 120
    assert result["workMinutes"] == 120
    assert result["gameRatio"] == 0.5
    assert result["workRatio"] == 0.5


def test_weekly_balance_zero_total():
    result = weekly_balance({"today": "2026-07-19", "gameMinutesByDate": {}, "pomodoroSessions": []})
    assert result["gameRatio"] == 0.0
    assert result["workRatio"] == 0.0
