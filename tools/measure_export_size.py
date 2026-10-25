# -*- coding: utf-8 -*-
"""寸笺 / Field Notes · 导出体积测算

对比：全字段 JSON（A） / 紧凑键 JSON（B） / B + gzip。
数据模型与《07 · 数据导入导出设计》一致。
"""
import gzip
import json
import os
import random
import time

random.seed(20261026)

CATEGORIES = ["日用品", "服装", "电子设备", "证件", "药品", "其他"]
COLORS = ["#66CCFF", "#4DB6AC", "#81C784", "#FFD54F", "#FFB74D", "#F06292", "#9575CD", "#FF8A65"]

BASE_ITEMS = [
    ("牙刷", 0, "软毛，旅行装"), ("牙膏", 0, "100g 以内"), ("防晒霜", 0, "SPF50+"),
    ("驱蚊水", 0, "海边必备"), ("洗漱包", 0, ""), ("洗发水", 0, "分装瓶"),
    ("短袖", 1, "速干 2 件"), ("短裤", 1, "1 条"), ("薄外套", 1, "早晚温差"),
    ("泳衣", 1, ""), ("袜子", 1, "3 双"), ("帽子", 1, "遮阳"),
    ("充电器", 2, "65W"), ("相机", 2, "含备用电池"), ("充电宝", 2, "20000mAh"),
    ("转换插头", 2, "日本 A 型"), ("护照", 3, "有效期 6 个月以上"),
    ("身份证", 3, ""), ("驾照", 3, "备用"), ("晕车药", 4, ""),
    ("创可贴", 4, ""), ("退烧药", 4, "儿童装"), ("雨伞", 5, "折叠"),
    ("行李箱", 5, "20 寸"), ("颈枕", 5, ""), ("收纳袋", 5, "3 个"),
]

TEMPLATES = [
    ("夏季游", "海边 5 天，轻装出发", ["短袖", "短裤", "泳衣", "薄外套", "防晒霜", "驱蚊水",
                                      "洗漱包", "充电器", "相机", "护照", "帽子", "雨伞"]),
    ("短途出差", "2 天 1 夜，只带必要", ["短袖", "薄外套", "洗漱包", "充电器", "充电宝",
                                        "转换插头", "身份证", "颈枕"]),
    ("露营装备", "周末露营，装备优先", ["薄外套", "驱蚊水", "洗漱包", "充电器", "充电宝",
                                        "相机", "创可贴", "雨伞", "收纳袋", "帽子",
                                        "短裤", "袜子", "牙刷", "牙膏", "防晒霜", "颈枕"]),
    ("回老家", "探亲一周，少而全", ["短袖", "短裤", "薄外套", "袜子", "洗漱包", "防晒霜",
                                    "充电器", "身份证", "晕车药", "雨伞"]),
]

PLANS = [
    # (标题, 目的地, 出发 epochDay, 返程 epochDay, [(物品名, 已勾选), ...])
    # 与原型场景一致：东京 7 件收 2 件；大理 8 件未收；上海 10 件全收
    # 日期：2026-10-26→20752、10-30→20756、11-07→20764、11-09→20766、10-02→20728、10-05→20731
    ("东京五日游", "东京", 20752, 20756, [
        ("护照", True), ("身份证", False),               # 证件 1/2
        ("短袖", True), ("短裤", False), ("袜子", False),  # 服装 1/3
        ("充电器", False), ("相机", False),               # 电子设备 0/2
    ]),
    ("周末短途", "大理", 20764, 20766, [
        ("身份证", False), ("充电器", False), ("相机", False), ("防晒霜", False),
        ("短袖", False), ("短裤", False), ("洗漱包", False), ("雨伞", False),
    ]),
    ("上海出差", "上海", 20728, 20731, [
        ("身份证", True), ("充电器", True), ("薄外套", True), ("洗漱包", True),
        ("雨伞", True), ("颈枕", True), ("晕车药", True), ("牙刷", True),
        ("牙膏", True), ("袜子", True),
    ]),
]

CAT_INDEX = {c: i for i, c in enumerate(CATEGORIES)}
NOW = 1792905878


def names_for(template_items):
    return set(template_items)


def build_item(name, idx=None, rnd=random):
    cat = next((c for n, c, _ in BASE_ITEMS if n == name), 0)
    note = next((r for n, _, r in BASE_ITEMS if n == name), "")
    return {
        "name": name,
        "category": CATEGORIES[cat],
        "note": note,
        "createdAt": NOW - rnd.randint(100, 900) * 86400,
        "updatedAt": NOW - rnd.randint(1, 90) * 86400,
    }


def dataset(item_count=None, template_count=4, plan_count=3):
    """生成示例数据；item_count 用于规模化测算。"""
    base = [build_item(n) for n, _, _ in BASE_ITEMS]
    if item_count and item_count > len(base):
        extra = []
        for i in range(item_count - len(base)):
            cat = CATEGORIES[i % len(CATEGORIES)]
            extra.append({
                "name": "物品%03d" % (i + 1),
                "category": cat,
                "note": "备注 %d" % (i % 7) if i % 3 == 0 else "",
                "createdAt": NOW - (i % 500) * 86400,
                "updatedAt": NOW - (i % 30) * 86400,
            })
        base += extra
    items = base
    by_name = {it["name"]: it for it in items}

    tmpls = []
    for name, desc, members in TEMPLATES[:template_count]:
        entries = []
        for m in members:
            if m in by_name:
                entries.append({"name": m, "category": by_name[m]["category"]})
            else:
                entries.append({"name": m, "category": "日用品"})
        tmpls.append({
            "name": name, "description": desc, "color": "#4DB6AC",
            "createdAt": NOW - 120 * 86400, "updatedAt": NOW - 10 * 86400,
            "lastUsedAt": NOW - 5 * 86400, "items": entries,
        })

    plans = []
    for name, dest, start, end, members in PLANS[:plan_count]:
        entries = []
        for item_name, checked in members:
            if item_name in by_name:
                entries.append({"name": item_name, "category": by_name[item_name]["category"],
                                "checked": 1 if checked else 0})
            else:
                entries.append({"name": item_name, "category": "日用品", "checked": 0})
        plans.append({
            "title": name, "destination": dest, "startEpochDay": start,
            "endEpochDay": end, "color": "#66CCFF", "note": "看展、购物，顺便去镰仓看海。",
            "createdAt": NOW - 40 * 86400, "updatedAt": NOW - 2 * 86400, "items": entries,
        })

    cats = [{"name": c, "color": COLORS[i], "createdAt": NOW - 200 * 86400}
            for i, c in enumerate(CATEGORIES)]
    return cats, items, tmpls, plans


def compact(cats, items, tmpls, plans):
    cat_idx = {c["name"]: i for i, c in enumerate(cats)}
    out = {
        "format": "field-notes-export",
        "version": 1,
        "exportedAt": NOW,
        "appVersion": "1.0",
        "categories": [{"n": c["name"], "c": c["color"], "a": c["createdAt"]} for c in cats],
        "items": [],
        "templates": [],
        "plans": [],
    }
    for it in items:
        obj = {"n": it["name"], "k": cat_idx[it["category"]],
               "a": it["createdAt"], "u": it["updatedAt"]}
        if it["note"]:
            obj["r"] = it["note"]
        out["items"].append(obj)
    for t in tmpls:
        obj = {"n": t["name"], "d": t["description"], "c": t["color"],
               "a": t["createdAt"], "u": t["updatedAt"], "l": t["lastUsedAt"],
               "it": [{"n": e["name"], "g": e["category"]} for e in t["items"]]}
        out["templates"].append(obj)
    for p in plans:
        obj = {"t": p["title"], "dst": p["destination"], "s": p["startEpochDay"],
               "e": p["endEpochDay"], "c": p["color"], "m": p["note"],
               "a": p["createdAt"], "u": p["updatedAt"], "it": []}
        for e in p["items"]:
            item = {"n": e["name"], "g": e["category"]}
            if e["checked"]:
                item["x"] = 1
            obj["it"].append(item)
        out["plans"].append(obj)
    return out


def verbose(cats, items, tmpls, plans):
    out = {
        "format": "field-notes-export",
        "version": 1,
        "exportedAt": NOW,
        "appVersion": "1.0",
        "categories": [{"name": c["name"], "colorHex": c["color"],
                        "createdAt": c["createdAt"]} for c in cats],
        "items": [],
        "templates": [],
        "plans": [],
    }
    for it in items:
        out["items"].append({"name": it["name"], "category": it["category"],
                             "note": it["note"], "createdAt": it["createdAt"],
                             "updatedAt": it["updatedAt"]})
    for t in tmpls:
        out["templates"].append({
            "name": t["name"], "description": t["description"], "colorHex": t["color"],
            "createdAt": t["createdAt"], "updatedAt": t["updatedAt"],
            "lastUsedAt": t["lastUsedAt"],
            "items": [{"name": e["name"], "categoryName": e["category"]} for e in t["items"]],
        })
    for p in plans:
        out["plans"].append({
            "title": p["title"], "destination": p["destination"],
            "startEpochDay": p["startEpochDay"], "endEpochDay": p["endEpochDay"],
            "colorHex": p["color"], "note": p["note"],
            "createdAt": p["createdAt"], "updatedAt": p["updatedAt"],
            "items": [{"name": e["name"], "categoryName": e["category"],
                       "checked": bool(e["checked"])} for e in p["items"]],
        })
    return out


def size(obj):
    return len(json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))


def gz(obj):
    raw = json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return len(gzip.compress(raw, compresslevel=9))


def main():
    print("| 数据规模 | 全字段 JSON | 紧凑 JSON | 紧凑 + gzip | 紧凑相对全字段 | gzip 相对全字段 |")
    print("| --- | --- | --- | --- | --- | --- |")
    for n in (26, 100, 200, 500, 1000):
        cats, items, tmpls, plans = dataset(item_count=n)
        a = size(verbose(cats, items, tmpls, plans))
        b = size(compact(cats, items, tmpls, plans))
        c = gz(compact(cats, items, tmpls, plans))
        print("| %d 物品 / 4 模板 / 3 计划 | %s B | %s B | %s B | %.1f%% | %.1f%% |"
              % (n, f"{a:,}", f"{b:,}", f"{c:,}", (a - b) / a * 100, (a - c) / a * 100))
    cats, items, tmpls, plans = dataset()
    b = compact(cats, items, tmpls, plans)
    print()
    print("示例集：%d 分类 / %d 物品 / %d 模板 / %d 计划 / %d 模板条目 / %d 计划条目"
          % (len(cats), len(items), len(tmpls), len(plans),
             sum(len(t["items"]) for t in tmpls), sum(len(p["items"]) for p in plans)))
    print("紧凑 JSON:", f"{size(b):,}", "B；gzip:", f"{gz(b):,}", "B")
    sample_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                              "docs", "samples")
    os.makedirs(sample_dir, exist_ok=True)
    with open(os.path.join(sample_dir, "export-sample.json"), "w", encoding="utf-8") as f:
        json.dump(b, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print()
    print(json.dumps(b, ensure_ascii=False, indent=2)[:2200])


if __name__ == "__main__":
    main()
