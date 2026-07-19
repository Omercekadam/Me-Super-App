from analytics.finance import forecast, insights


def tx(amount, type_="EXPENSE", category="Yemek", date="2026-07-10"):
    return {"amountKurus": amount, "type": type_, "category": category, "occurredAt": date}


def sub(amount, billing_day, period="MONTHLY", billing_month=None, active=True):
    return {
        "amountKurus": amount,
        "billingDay": billing_day,
        "period": period,
        "billingMonth": billing_month,
        "isActive": active,
    }


# --- forecast ---

def test_forecast_plan_scenario():
    """Plandaki kapi senaryosu: 15 gunde 3.000 TL + vadesi gelmemis 500 TL abonelik."""
    txns = [tx(20_000, date=f"2026-07-{d:02d}") for d in range(1, 16)]  # 15 x 200 TL
    payload = {
        "month": "2026-07",
        "today": "2026-07-15",
        "daysInMonth": 31,
        "txns": txns,
        "subscriptions": [sub(50_000, billing_day=25)],
    }
    result = forecast(payload)
    assert result["spentKurus"] == 300_000
    assert result["dailyPaceKurus"] == 20_000
    assert result["daysLeft"] == 16
    assert result["remainingSubsKurus"] == 50_000
    # 300.000 + 20.000*16 + 50.000 = 670.000 kurus
    assert result["projectedKurus"] == 670_000


def test_forecast_paid_subscription_not_counted():
    payload = {
        "month": "2026-07",
        "today": "2026-07-15",
        "daysInMonth": 31,
        "txns": [],
        "subscriptions": [
            sub(50_000, billing_day=10),               # vadesi gecti
            sub(30_000, billing_day=20, active=False),  # pasif
            sub(70_000, billing_day=20, period="YEARLY", billing_month=12),  # baska ay
        ],
    }
    assert forecast(payload)["remainingSubsKurus"] == 0


def test_forecast_yearly_sub_due_this_month():
    payload = {
        "month": "2026-07",
        "today": "2026-07-15",
        "daysInMonth": 31,
        "txns": [],
        "subscriptions": [sub(120_000, billing_day=20, period="YEARLY", billing_month=7)],
    }
    assert forecast(payload)["remainingSubsKurus"] == 120_000


def test_forecast_income_ignored():
    payload = {
        "month": "2026-07",
        "today": "2026-07-10",
        "daysInMonth": 31,
        "txns": [tx(1_000_000, type_="INCOME", category="Maaş", date="2026-07-05")],
        "subscriptions": [],
    }
    assert forecast(payload)["spentKurus"] == 0


# --- insights ---

def test_insight_dominant_category():
    txns = [
        tx(40_000, category="Oyun"),
        tx(30_000, category="Yemek"),
        tx(30_000, category="Ulaşım"),
    ]
    result = insights({"month": "2026-07", "prevMonth": "2026-06", "txns": txns, "budgets": []})
    assert any("%40" in r["text"] and "Oyun" in r["text"] for r in result)


def test_insight_month_over_month_increase():
    txns = [
        tx(150_000, category="Oyun", date="2026-07-10"),
        tx(50_000, category="Oyun", date="2026-06-10"),
    ]
    result = insights({"month": "2026-07", "prevMonth": "2026-06", "txns": txns, "budgets": []})
    texts = [r["text"] for r in result if r["severity"] == "warn"]
    assert any("Oyun" in t and "1.000 TL fazla" in t for t in texts)


def test_insight_budget_overrun_is_alert_and_sorted_first():
    txns = [tx(120_000, category="Yemek")]
    budgets = [{"categoryName": "Yemek", "monthlyLimitKurus": 100_000}]
    result = insights({"month": "2026-07", "prevMonth": "", "txns": txns, "budgets": budgets})
    assert result[0]["severity"] == "alert"
    assert "aştın" in result[0]["text"]


def test_insight_general_budget_warning():
    txns = [tx(90_000, category="Yemek")]
    budgets = [{"categoryName": None, "monthlyLimitKurus": 100_000}]
    result = insights({"month": "2026-07", "prevMonth": "", "txns": txns, "budgets": budgets})
    assert any(r["severity"] == "warn" and "Genel" in r["text"] for r in result)


def test_no_insights_on_empty_month():
    assert insights({"month": "2026-07", "prevMonth": "2026-06", "txns": [], "budgets": []}) == []
