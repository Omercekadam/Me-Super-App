import json

import pytest

from analytics import api
from analytics.finance import summary


def tx(amount, type_="EXPENSE", category="Yemek", date="2026-07-10"):
    return {"amountKurus": amount, "type": type_, "category": category, "occurredAt": date}


def test_empty_month():
    result = summary({"month": "2026-07", "txns": []})
    assert result["incomeKurus"] == 0
    assert result["expenseKurus"] == 0
    assert result["balanceKurus"] == 0
    assert result["byCategory"] == []


def test_totals_and_balance():
    payload = {
        "month": "2026-07",
        "txns": [
            tx(100_000, "INCOME", "Maaş"),
            tx(30_000, "EXPENSE", "Yemek"),
            tx(20_000, "EXPENSE", "Oyun"),
        ],
    }
    result = summary(payload)
    assert result["incomeKurus"] == 100_000
    assert result["expenseKurus"] == 50_000
    assert result["balanceKurus"] == 50_000


def test_only_requested_month_is_counted():
    payload = {
        "month": "2026-07",
        "txns": [
            tx(10_000, date="2026-07-01"),
            tx(99_000, date="2026-06-30"),
        ],
    }
    result = summary(payload)
    assert result["expenseKurus"] == 10_000


def test_category_breakdown_sorted_with_shares():
    payload = {
        "month": "2026-07",
        "txns": [
            tx(10_000, category="Yemek"),
            tx(30_000, category="Oyun"),
            tx(10_000, category="Oyun"),
        ],
    }
    result = summary(payload)
    categories = result["byCategory"]
    assert [c["category"] for c in categories] == ["Oyun", "Yemek"]
    assert categories[0]["totalKurus"] == 40_000
    assert categories[0]["share"] == 0.8
    assert categories[1]["share"] == 0.2


def test_api_dispatch_roundtrip():
    payload = {"month": "2026-07", "txns": [tx(5_000)]}
    raw = api.run("finance.summary", json.dumps(payload))
    result = json.loads(raw)
    assert result["expenseKurus"] == 5_000
    assert result["engine"].startswith("pandas")


def test_api_unknown_function():
    with pytest.raises(ValueError):
        api.run("yok.boyle.bir.fonksiyon", "{}")
