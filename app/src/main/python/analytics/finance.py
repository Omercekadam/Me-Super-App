"""Finans analizleri. Tum tutarlar kurus cinsinden tamsayidir (100 kurus = 1 TL)."""

from datetime import date

import pandas as pd


def _tl(kurus):
    """Kurus -> '1.234 TL' (insan okunur, tam sayi TL)."""
    return format(int(round(kurus / 100)), ",").replace(",", ".") + " TL"


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


def _month_expenses(txns, month):
    """Verilen aya ait gider islemlerini DataFrame olarak dondurur."""
    if not txns:
        return pd.DataFrame(columns=["amountKurus", "type", "category", "occurredAt"])
    df = pd.DataFrame(txns)
    return df[(df["occurredAt"].str.startswith(month)) & (df["type"] == "EXPENSE")]


def forecast(payload):
    """Ay sonu gider tahmini.

    payload = {
        "month": "2026-07", "today": "2026-07-19", "daysInMonth": 31,
        "txns": [...],  # summary ile ayni format
        "subscriptions": [{"amountKurus": int, "billingDay": int,
                           "period": "MONTHLY"|"YEARLY", "billingMonth": int|None,
                           "isActive": bool}, ...]
    }

    Tahmin = simdiye kadar harcanan
           + gunluk ortalama hiz x kalan gun
           + vadesi henuz gelmemis aktif abonelikler.
    Not: abonelik odemesi islem olarak da girilirse çifte sayilabilir;
    bu bilinçli bir sadelestirme, kullanici abonelikleri islem olarak girmez.
    """
    month = payload["month"]
    today = date.fromisoformat(payload["today"])
    days_in_month = int(payload["daysInMonth"])

    expenses = _month_expenses(payload.get("txns") or [], month)
    spent = int(expenses["amountKurus"].sum()) if not expenses.empty else 0

    days_elapsed = max(today.day, 1)
    days_left = max(days_in_month - today.day, 0)
    daily_pace = spent / days_elapsed

    month_no = int(month.split("-")[1])
    remaining_subs = 0
    for sub in payload.get("subscriptions") or []:
        if not sub.get("isActive", True):
            continue
        due_this_month = (
            sub.get("period", "MONTHLY") == "MONTHLY"
            or sub.get("billingMonth") == month_no
        )
        if due_this_month and sub["billingDay"] > today.day:
            remaining_subs += sub["amountKurus"]

    projected = spent + round(daily_pace * days_left) + remaining_subs
    return {
        "month": month,
        "spentKurus": spent,
        "dailyPaceKurus": round(daily_pace),
        "remainingSubsKurus": remaining_subs,
        "daysLeft": days_left,
        "projectedKurus": int(projected),
    }


# Icgorü esikleri
_SHARE_INFO = 0.35      # kategori payi bunu asarsa bilgi ver
_SHARE_WARN = 0.50      # bunu asarsa uyari
_MOM_RATIO = 1.25       # kategori gideri gecen ayin %25 ustune cikarsa
_MOM_MIN_KURUS = 20_000  # ...ve fark en az 200 TL ise
_BUDGET_WARN = 0.85     # limit doluluk uyari esigi


def insights(payload):
    """Kural tabanli Turkce icgoruler.

    payload = {
        "month": "2026-07", "prevMonth": "2026-06",
        "txns": [...],  # iki ayi da kapsayan islemler
        "budgets": [{"categoryName": str|None, "monthlyLimitKurus": int}, ...]
    }
    Cikti: [{"severity": "info"|"warn"|"alert", "text": str}, ...] (agirdan hafife sirali)
    """
    month = payload["month"]
    prev_month = payload.get("prevMonth", "")
    cur = _month_expenses(payload.get("txns") or [], month)
    prev = _month_expenses(payload.get("txns") or [], prev_month) if prev_month else cur.iloc[0:0]

    results = []
    cur_total = int(cur["amountKurus"].sum()) if not cur.empty else 0
    prev_total = int(prev["amountKurus"].sum()) if not prev.empty else 0

    # 1) Baskin kategori
    if cur_total > 0:
        grouped = cur.groupby("category")["amountKurus"].sum().sort_values(ascending=False)
        top_cat, top_val = grouped.index[0], int(grouped.iloc[0])
        share = top_val / cur_total
        if share >= _SHARE_INFO:
            severity = "warn" if share >= _SHARE_WARN else "info"
            results.append({
                "severity": severity,
                "text": f"Bu ay harcamalarının %{share * 100:.0f}'i {top_cat} kategorisine gitti ({_tl(top_val)}).",
            })

    # 2) Kategori bazinda aylik artis
    if not cur.empty and not prev.empty:
        cur_by_cat = cur.groupby("category")["amountKurus"].sum()
        prev_by_cat = prev.groupby("category")["amountKurus"].sum()
        for cat in cur_by_cat.index:
            c, p = int(cur_by_cat[cat]), int(prev_by_cat.get(cat, 0))
            if p > 0 and c >= p * _MOM_RATIO and (c - p) >= _MOM_MIN_KURUS:
                results.append({
                    "severity": "warn",
                    "text": f"{cat} harcaman geçen aydan {_tl(c - p)} fazla — dikkat!",
                })

    # 3) Bütce limitleri
    cur_by_cat = cur.groupby("category")["amountKurus"].sum() if not cur.empty else pd.Series(dtype="int64")
    for budget in payload.get("budgets") or []:
        limit = budget["monthlyLimitKurus"]
        if limit <= 0:
            continue
        cat_name = budget.get("categoryName")
        spent = cur_total if cat_name is None else int(cur_by_cat.get(cat_name, 0))
        label = "Genel" if cat_name is None else cat_name
        ratio = spent / limit
        if ratio >= 1.0:
            results.append({
                "severity": "alert",
                "text": f"{label} limitini {_tl(spent - limit)} aştın ({_tl(spent)} / {_tl(limit)}).",
            })
        elif ratio >= _BUDGET_WARN:
            results.append({
                "severity": "warn",
                "text": f"{label} limitinin %{ratio * 100:.0f}'i doldu ({_tl(spent)} / {_tl(limit)}).",
            })

    # 4) Toplam gider artisi
    if prev_total > 0 and cur_total >= prev_total * _MOM_RATIO and (cur_total - prev_total) >= 50_000:
        results.append({
            "severity": "warn",
            "text": f"Toplam giderin geçen aydan {_tl(cur_total - prev_total)} fazla.",
        })

    order = {"alert": 0, "warn": 1, "info": 2}
    results.sort(key=lambda r: order[r["severity"]])
    return results
