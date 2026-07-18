"""Finans analizleri. Tum tutarlar kurus cinsinden tamsayidir (100 kurus = 1 TL)."""

import pandas as pd


def _empty_summary(month):
    return {
        "month": month,
        "incomeKurus": 0,
        "expenseKurus": 0,
        "balanceKurus": 0,
        "byCategory": [],
        "engine": f"pandas {pd.__version__}",
    }


def summary(payload):
    """Ay ozeti: gelir/gider/bakiye toplamlari + kategori kirilimi.

    payload = {
        "month": "2026-07",
        "txns": [{"amountKurus": int, "type": "INCOME"|"EXPENSE",
                  "category": str, "occurredAt": "YYYY-MM-DD"}, ...]
    }
    """
    month = payload["month"]
    txns = payload.get("txns") or []
    if not txns:
        return _empty_summary(month)

    df = pd.DataFrame(txns)
    df = df[df["occurredAt"].str.startswith(month)]
    if df.empty:
        return _empty_summary(month)

    income = int(df.loc[df["type"] == "INCOME", "amountKurus"].sum())
    expense = int(df.loc[df["type"] == "EXPENSE", "amountKurus"].sum())

    by_category = []
    expenses = df[df["type"] == "EXPENSE"]
    if not expenses.empty:
        grouped = (
            expenses.groupby("category")["amountKurus"].sum().sort_values(ascending=False)
        )
        total = float(grouped.sum())
        by_category = [
            {
                "category": category,
                "totalKurus": int(amount),
                "share": round(float(amount) / total, 4) if total else 0.0,
            }
            for category, amount in grouped.items()
        ]

    return {
        "month": month,
        "incomeKurus": income,
        "expenseKurus": expense,
        "balanceKurus": income - expense,
        "byCategory": by_category,
        "engine": f"pandas {pd.__version__}",
    }
