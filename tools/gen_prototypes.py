# -*- coding: utf-8 -*-
"""寸笺 / Field Notes · 界面原型图生成脚本

风格沿用参考项目「时记」：半平面风（纯色块 + 大圆角 + 轻投影），主色 #66CCFF。
画布 780 x 1688（390 x 844 dp @2x），输出到 docs/images/。
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "images")
os.makedirs(OUT, exist_ok=True)

FONT = "C:/Windows/Fonts/msyh.ttc"
FONT_B = "C:/Windows/Fonts/msyhbd.ttc"

PRIMARY = "#66CCFF"
PRIMARY_DEEP = "#3FA9F5"
PRIMARY_LIGHT = "#E3F4FE"
BG = "#F6FAFD"
INK = "#22313F"
INK2 = "#7C93A3"
INK3 = "#5A6B78"
FIELD_BG = "#EFF5FA"
LINE = "#E5EDF3"
PLACEHOLDER = "#9AB0C0"
DANGER = "#FF5252"
GREEN = "#2E9E6B"
GREEN_BG = "#E8F7EF"
GRAY_BG = "#F0F3F6"

PALETTE = ["#66CCFF", "#4DB6AC", "#81C784", "#FFD54F",
           "#FFB74D", "#F06292", "#9575CD", "#FF8A65"]

W, H = 780, 1688


def F(size, bold=False):
    return ImageFont.truetype(FONT_B if bold else FONT, size)


def mix(h1, h2, t):
    c1 = [int(h1[i:i + 2], 16) for i in (1, 3, 5)]
    c2 = [int(h2[i:i + 2], 16) for i in (1, 3, 5)]
    return "#%02X%02X%02X" % tuple(round(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def canvas(bg=BG):
    return Image.new("RGBA", (W, H), bg)


def soft_shadow(img, box, radius, blur=16, dy=10, alpha=30):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle([box[0], box[1] + dy, box[2], box[3] + dy],
                        radius=radius, fill=(40, 80, 120, alpha))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def soft_shadow_circle(img, cx, cy, r, blur=14, dy=8, alpha=48):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - r, cy - r + dy, cx + r, cy + r + dy], fill=(40, 80, 120, alpha))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def ellipsize(d, text, font, max_w):
    if d.textlength(text, font=font) <= max_w:
        return text
    while text and d.textlength(text + "…", font=font) > max_w:
        text = text[:-1]
    return text + "…"


def wrap_text(d, text, font, max_w):
    lines = []
    for para in text.split("\n"):
        cur = ""
        for ch in para:
            if d.textlength(cur + ch, font=font) <= max_w:
                cur += ch
            else:
                lines.append(cur)
                cur = ch
        lines.append(cur)
    return lines


# ---------------- 系统栏 ----------------
def status_bar(d):
    d.text((44, 27), "9:41", font=F(26, True), fill=INK, anchor="lm")
    for i in range(4):
        h = 8 + i * 6
        d.rounded_rectangle([608 + i * 14, 40 - h, 618 + i * 14, 40], radius=3, fill=INK)
    d.text((670, 27), "5G", font=F(22, True), fill=INK, anchor="lm")
    d.rounded_rectangle([708, 16, 748, 38], radius=7, outline=INK, width=3)
    d.rectangle([750, 22, 755, 32], fill=INK)
    d.rounded_rectangle([711, 19, 736, 35], radius=4, fill=INK)


def gesture_bar(d, y=1670):
    d.rounded_rectangle([300, y, 480, y + 8], radius=4, fill="#C9D6E0")


# ---------------- 图标 ----------------
def icon_search(d, cx, cy, color=PRIMARY_DEEP, s=1.0):
    r = int(15 * s)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=max(3, int(5 * s)))
    d.line([(cx + int(11 * s), cy + int(11 * s)), (cx + int(22 * s), cy + int(22 * s))],
           fill=color, width=max(3, int(6 * s)))


def icon_plus(d, cx, cy, color="#FFFFFF", s=1.0):
    arm = int(26 * s)
    d.line([(cx - arm, cy), (cx + arm, cy)], fill=color, width=max(4, int(10 * s)))
    d.line([(cx, cy - arm), (cx, cy + arm)], fill=color, width=max(4, int(10 * s)))


def icon_close(d, cx, cy, color=INK2, s=1.0):
    arm = int(13 * s)
    d.line([(cx - arm, cy - arm), (cx + arm, cy + arm)], fill=color, width=max(3, int(6 * s)))
    d.line([(cx - arm, cy + arm), (cx + arm, cy - arm)], fill=color, width=max(3, int(6 * s)))


def icon_check(d, cx, cy, color="#FFFFFF", s=1.0, width=None):
    d.line([(cx - int(10 * s), cy + int(1 * s)), (cx - int(3 * s), cy + int(8 * s)),
            (cx + int(12 * s), cy - int(9 * s))],
           fill=color, width=width or max(3, int(7 * s)), joint="curve")


def icon_back(d, cx, cy, color=INK, s=1.0):
    d.line([(cx + int(12 * s), cy - int(16 * s)), (cx - int(6 * s), cy),
            (cx + int(12 * s), cy + int(16 * s))],
           fill=color, width=max(4, int(7 * s)), joint="curve")


def icon_chevron(d, cx, cy, color=INK2, s=1.0, down=False):
    dy = int(7 * s) if down else 0
    if down:
        d.line([(cx - int(13 * s), cy - int(7 * s)), (cx, cy + int(7 * s)),
                (cx + int(13 * s), cy - int(7 * s))],
               fill=color, width=max(3, int(6 * s)), joint="curve")
    else:
        d.line([(cx - int(7 * s), cy - int(13 * s)), (cx + int(7 * s), cy),
                (cx - int(7 * s), cy + int(13 * s))],
               fill=color, width=max(3, int(6 * s)), joint="curve")


def icon_more(d, cx, cy, color=INK2):
    for dy in (-14, 0, 14):
        d.ellipse([cx - 4, cy + dy - 4, cx + 4, cy + dy + 4], fill=color)


def icon_calendar(d, cx, cy, color=INK2, s=1.0, width=None):
    w = width or max(3, int(4 * s))
    d.rounded_rectangle([cx - int(24 * s), cy - int(21 * s), cx + int(24 * s), cy + int(23 * s)],
                        radius=7, outline=color, width=w)
    d.line([(cx - int(24 * s), cy - int(8 * s)), (cx + int(24 * s), cy - int(8 * s))],
           fill=color, width=w)
    d.line([(cx - int(11 * s), cy - int(29 * s)), (cx - int(11 * s), cy - int(14 * s))],
           fill=color, width=w)
    d.line([(cx + int(11 * s), cy - int(29 * s)), (cx + int(11 * s), cy - int(14 * s))],
           fill=color, width=w)
    for dx in (-11, 2, 15):
        for dy in (5, 16):
            d.ellipse([cx + int(dx * s) - 2, cy + int(dy * s) - 2,
                       cx + int(dx * s) + 2, cy + int(dy * s) + 2], fill=color)


def icon_suitcase(d, cx, cy, color="#FFFFFF", s=1.0):
    w, h = int(34 * s), int(28 * s)
    r = max(3, int(6 * s))
    d.rounded_rectangle([cx - w, cy - h + int(6 * s), cx + w, cy + h], radius=r,
                        outline=color, width=max(3, int(5 * s)))
    d.rounded_rectangle([cx - int(13 * s), cy - h - int(6 * s), cx + int(13 * s), cy - h + int(8 * s)],
                        radius=max(2, int(4 * s)), outline=color, width=max(2, int(4 * s)))
    d.line([(cx - w + int(8 * s), cy - h + int(14 * s)),
            (cx - w + int(8 * s), cy + h - int(6 * s))], fill=color, width=max(2, int(3 * s)))
    d.line([(cx + w - int(8 * s), cy - h + int(14 * s)),
            (cx + w - int(8 * s), cy + h - int(6 * s))], fill=color, width=max(2, int(3 * s)))


def icon_layers(d, cx, cy, color="#FFFFFF", s=1.0):
    """模板图标：带夹子的清单板"""
    w, h = int(20 * s), int(26 * s)
    d.rounded_rectangle([cx - w, cy - h, cx + w, cy + h], radius=max(3, int(6 * s)),
                        outline=color, width=max(3, int(4 * s)))
    d.rounded_rectangle([cx - int(8 * s), cy - h - int(6 * s),
                         cx + int(8 * s), cy - h + int(8 * s)],
                        radius=max(2, int(3 * s)), fill=color)
    d.line([(cx - int(10 * s), cy - int(2 * s)), (cx + int(10 * s), cy - int(2 * s))],
           fill=color, width=max(2, int(3 * s)))
    d.line([(cx - int(10 * s), cy + int(10 * s)), (cx + int(4 * s), cy + int(10 * s))],
           fill=color, width=max(2, int(3 * s)))


def icon_box(d, cx, cy, color="#FFFFFF", s=1.0):
    w = int(25 * s)
    d.polygon([(cx - w, cy - int(10 * s)), (cx, cy - int(24 * s)),
               (cx + w, cy - int(10 * s)), (cx, cy + int(4 * s))],
              outline=color, width=max(3, int(4 * s)))
    d.line([(cx - w, cy - int(10 * s)), (cx - w, cy + int(16 * s)),
            (cx, cy + int(30 * s)), (cx + w, cy + int(16 * s)),
            (cx + w, cy - int(10 * s))], fill=color, width=max(3, int(4 * s)), joint="curve")
    d.line([(cx, cy + int(4 * s)), (cx, cy + int(30 * s))], fill=color, width=max(2, int(3 * s)))


def icon_person(d, cx, cy, color="#FFFFFF", s=1.0):
    d.ellipse([cx - int(11 * s), cy - int(26 * s), cx + int(11 * s), cy - int(4 * s)],
              outline=color, width=max(3, int(4 * s)))
    d.arc([cx - int(24 * s), cy - int(6 * s), cx + int(24 * s), cy + int(28 * s)],
          start=200, end=340, fill=color, width=max(3, int(4 * s)))


def icon_pencil(d, cx, cy, color=PRIMARY_DEEP, s=1.0):
    d.line([(cx - int(11 * s), cy + int(11 * s)), (cx + int(11 * s), cy - int(11 * s))],
           fill=color, width=max(3, int(8 * s)))
    d.line([(cx + int(7 * s), cy - int(17 * s)), (cx + int(17 * s), cy - int(7 * s))],
           fill=color, width=max(3, int(6 * s)))
    d.polygon([(cx - int(15 * s), cy + int(15 * s)), (cx - int(5 * s), cy + int(9 * s)),
               (cx - int(9 * s), cy + int(5 * s))], fill=color)


def icon_trash(d, cx, cy, color=DANGER, s=1.0):
    d.rounded_rectangle([cx - int(13 * s), cy - int(6 * s), cx + int(13 * s), cy + int(18 * s)],
                        radius=4, outline=color, width=max(2, int(4 * s)))
    d.line([(cx - int(17 * s), cy - int(10 * s)), (cx + int(17 * s), cy - int(10 * s))],
           fill=color, width=max(2, int(5 * s)))
    d.line([(cx - int(6 * s), cy - int(16 * s)), (cx + int(6 * s), cy - int(16 * s))],
           fill=color, width=max(2, int(5 * s)))


def icon_download(d, cx, cy, color=PRIMARY_DEEP, s=1.0):
    d.line([(cx, cy - int(18 * s)), (cx, cy + int(8 * s))], fill=color, width=max(3, int(5 * s)))
    d.line([(cx - int(11 * s), cy - int(3 * s)), (cx, cy + int(9 * s)),
            (cx + int(11 * s), cy - int(3 * s))], fill=color, width=max(3, int(5 * s)), joint="curve")
    d.line([(cx - int(18 * s), cy + int(20 * s)), (cx + int(18 * s), cy + int(20 * s))],
           fill=color, width=max(3, int(5 * s)))


def icon_upload(d, cx, cy, color=PRIMARY_DEEP, s=1.0):
    d.line([(cx, cy + int(18 * s)), (cx, cy - int(8 * s))], fill=color, width=max(3, int(5 * s)))
    d.line([(cx - int(11 * s), cy + int(3 * s)), (cx, cy - int(9 * s)),
            (cx + int(11 * s), cy + int(3 * s))], fill=color, width=max(3, int(5 * s)), joint="curve")
    d.line([(cx - int(18 * s), cy - int(20 * s)), (cx + int(18 * s), cy - int(20 * s))],
           fill=color, width=max(3, int(5 * s)))


def icon_alert(d, cx, cy, color="#FFFFFF", s=1.0):
    d.text((cx, cy), "!", font=F(int(48 * s), True), fill=color, anchor="mm")


def red_dot(d, cx, cy, r=8):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=DANGER)


# ---------------- 通用组件 ----------------
def bottom_nav(d, active):
    y0 = 1560
    d.rectangle([0, y0, W, H], fill="#FFFFFF")
    d.line([(0, y0), (W, y0)], fill=LINE, width=2)
    items = [("plan", "出行"), ("template", "模板"), ("item", "物品"), ("me", "我的")]
    for i, (kind, label) in enumerate(items):
        cx = 97 + i * 195
        on = (i == active)
        color = PRIMARY_DEEP if on else "#A9B9C6"
        cy = 1600
        if kind == "plan":
            icon_suitcase(d, cx, cy, color, 0.74)
        elif kind == "template":
            icon_layers(d, cx, cy, color, 0.70)
        elif kind == "item":
            icon_box(d, cx, cy, color, 0.70)
        else:
            icon_person(d, cx, cy + 2, color, 0.78)
        d.text((cx, 1648), label, font=F(21, on), fill=color, anchor="mm")
    gesture_bar(d)


def fab(d, cy=1450):
    cx, r = 688, 54
    soft_shadow_circle(_IMG, cx, cy, r)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=PRIMARY)
    icon_plus(d, cx, cy, "#FFFFFF", 0.92)


def header(d, title, subtitle, right="search"):
    d.text((34, 94), title, font=F(52, True), fill=INK)
    d.text((36, 164), subtitle, font=F(23), fill=INK2)
    if right == "search":
        d.ellipse([650, 78, 730, 158], fill=PRIMARY_LIGHT)
        icon_search(d, 690, 117, PRIMARY_DEEP, 0.95)


def top_bar(d, title, right="more"):
    d.line([(64, 94), (38, 120), (64, 146)], fill=INK, width=7, joint="curve")
    d.text((96, 118), title, font=F(36, True), fill=INK, anchor="lm")
    if right == "more":
        icon_more(d, 712, 118, INK2)
    elif right == "pencil":
        d.ellipse([650, 80, 726, 156], fill=PRIMARY_LIGHT)
        icon_pencil(d, 688, 118, PRIMARY_DEEP, 0.9)


def section_title(d, x, y, title, right=None):
    d.rounded_rectangle([x, y + 6, x + 8, y + 34], radius=4, fill=PRIMARY)
    d.text((x + 22, y + 20), title, font=F(30, True), fill=INK, anchor="lm")
    if right:
        d.text((748, y + 20), right, font=F(22), fill=INK2, anchor="rm")


def chip(d, x, y, text, active=False, h=56, fs=23, pad=26):
    font = F(fs, active)
    tw = d.textlength(text, font=font)
    w = int(tw + pad * 2)
    if active:
        d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill=PRIMARY)
        d.text((x + w // 2, y + h // 2 - 2), text, font=font, fill="#FFFFFF", anchor="mm")
    else:
        d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill="#FFFFFF",
                            outline="#D8E3EC", width=2)
        d.text((x + w // 2, y + h // 2 - 2), text, font=font, fill=INK3, anchor="mm")
    return w


def progress_bar(d, x, y, w, ratio, color=PRIMARY, h=10):
    d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill="#E6EEF4")
    if ratio > 0:
        d.rounded_rectangle([x, y, x + max(h, int(w * ratio)), y + h],
                            radius=h // 2, fill=color)


def checkbox(d, cx, cy, checked, r=18):
    if checked:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=PRIMARY)
        icon_check(d, cx, cy, "#FFFFFF", 0.85, width=5)
    else:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline="#C9D6E0", width=3)


def strike_text(d, x, y, text, font, color, line_color, max_w=None):
    if max_w:
        text = ellipsize(d, text, font, max_w)
    d.text((x, y), text, font=font, fill=color, anchor="lm")
    tw = d.textlength(text, font=font)
    d.line([(x, y), (x + tw, y)], fill=line_color, width=3)


def pill(d, x, y, text, bg, fg, fs=20, pad=18, h=42):
    font = F(fs, True)
    tw = d.textlength(text, font=font)
    w = int(tw + pad * 2)
    d.rounded_rectangle([x, y, x + w, y + h], radius=h // 2, fill=bg)
    d.text((x + w // 2, y + h // 2 - 1), text, font=font, fill=fg, anchor="mm")
    return w


def status_pill(d, x_right, y, kind):
    """kind: ongoing / upcoming / past / done"""
    if kind == "ongoing":
        text, bg, fg = "进行中 · 第 1 天", GREEN_BG, GREEN
    elif kind == "upcoming":
        text, bg, fg = "还有 12 天出发", PRIMARY_LIGHT, PRIMARY_DEEP
    elif kind == "past":
        text, bg, fg = "已结束 21 天", GRAY_BG, INK2
    else:
        text, bg, fg = "已完成", GREEN_BG, GREEN
    font = F(20, True)
    w = int(d.textlength(text, font=font) + 32)
    d.rounded_rectangle([x_right - w, y, x_right, y + 42], radius=21, fill=bg)
    d.text((x_right - w / 2, y + 20), text, font=font, fill=fg, anchor="mm")
    return w


def check_row(d, x, y, w, name, checked, h=82, fs=28):
    cy = y + h // 2
    checkbox(d, x + 30, cy, checked, 18)
    color = PLACEHOLDER if checked else INK
    font = F(fs, checked is False)
    if checked:
        strike_text(d, x + 66, cy, name, font, PLACEHOLDER, PLACEHOLDER, max_w=w - 120)
    else:
        d.text((x + 66, cy), ellipsize(d, name, font, w - 120), font=font, fill=color, anchor="lm")


def list_divider(d, x1, x2, y):
    d.line([(x1, y), (x2, y)], fill="#EDF3F8", width=2)


# ---------------- 主控图（供弹层复用） ----------------
_IMG = None


# ================= 01 计划列表 =================
def draw_plan_card(img, d, x, y, w, h, data):
    soft_shadow(img, (x, y, x + w, y + h), 30, blur=18, dy=8, alpha=28)
    d.rounded_rectangle([x, y, x + w, y + h], radius=30, fill="#FFFFFF")
    color = data["color"]
    # 左侧色块
    d.rounded_rectangle([x + 24, y + 26, x + 108, y + 110], radius=24, fill=color)
    icon_suitcase(d, x + 66, y + 66, "#FFFFFF", 0.78)
    # 目的地 + 红点
    f_dest = F(38, True)
    dest = ellipsize(d, data["dest"], f_dest, 300)
    d.text((x + 128, y + 56), dest, font=f_dest, fill=INK, anchor="lm")
    if data["dot"]:
        red_dot(d, x + 128 + d.textlength(dest, font=f_dest) + 18, y + 54, 8)
    # 标题
    d.text((x + 128, y + 100), ellipsize(d, data["title"], F(23), 300),
           font=F(23), fill=INK2, anchor="lm")
    # 状态
    status_pill(d, x + w - 24, y + 26, data["kind"])
    # 日期
    icon_calendar(d, x + 38, y + 154, INK2, 0.62, 3)
    d.text((x + 62, y + 154), data["dates"], font=F(23), fill=INK3, anchor="lm")
    # 进度
    d.text((x + w - 24, y + 154), "%d/%d 已收" % (data["checked"], data["total"]),
           font=F(22, True), fill=INK2, anchor="rm")
    progress_bar(d, x + 24, y + 196, w - 48, data["checked"] / max(1, data["total"]))
    # 物品预览
    d.text((x + 24, y + 246), ellipsize(d, data["preview"], F(21), w - 48),
           font=F(21), fill=INK2, anchor="lm")


def screen_plans():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, "寸笺", "一纸清单，装下整段旅程")
    d.text((34, 218), "全部计划", font=F(26, True), fill=INK, anchor="lm")
    d.text((748, 218), "待收 13 件 · 共 3 个计划", font=F(22), fill=INK2, anchor="rm")
    x = 32
    for text, active in [("全部", True), ("进行中", False), ("未开始", False), ("已结束", False)]:
        x += chip(d, x, 250, text, active, h=56, fs=23, pad=28) + 14
    cards = [
        dict(dest="东京", title="东京五日游", color="#66CCFF", kind="ongoing",
             dates="2026.10.26 – 10.30 · 5 天", checked=2, total=7,
             preview="身份证 · 短裤 · 袜子 · 充电器  +1", dot=True),
        dict(dest="大理", title="周末短途", color="#4DB6AC", kind="upcoming",
             dates="2026.11.07 – 11.09 · 3 天", checked=0, total=8,
             preview="防晒霜 · 相机 · 短袖 · 短裤  +4", dot=True),
        dict(dest="上海", title="", color="#9575CD", kind="past",
             dates="2026.10.02 – 10.05 · 4 天", checked=10, total=10,
             preview="已全部打包", dot=False),
    ]
    y = 342
    for c in cards:
        draw_plan_card(img, d, 32, y, 716, 298, c)
        y += 322
    d.text((390, 1344), "数据仅保存在本机 · 换机请用「我的 → 导出数据」",
           font=F(21), fill=PLACEHOLDER, anchor="mm")
    fab(d, 1452)
    bottom_nav(d, 0)
    img.convert("RGB").save(os.path.join(OUT, "proto-01-plans.png"))


# ================= 02 计划详情 =================
def draw_plan_detail_base():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    top_bar(d, "东京五日游", "more")
    # Hero
    soft_shadow(img, (32, 176, 748, 430), 30, blur=20, dy=10, alpha=40)
    d.rounded_rectangle([32, 176, 748, 430], radius=30, fill="#66CCFF")
    # 装饰行李箱
    icon_suitcase(d, 640, 330, mix("#66CCFF", "#FFFFFF", 0.28), 2.1)
    d.text((64, 216), "东京", font=F(54, True), fill="#FFFFFF")
    d.text((66, 292), "2026.10.26 – 10.30 · 5 天", font=F(25), fill=mix("#66CCFF", "#FFFFFF", 0.88))
    d.rounded_rectangle([64, 338, 310, 390], radius=26, fill=mix("#66CCFF", "#FFFFFF", 0.25))
    d.text((187, 363), "进行中 · 第 1 天", font=F(22, True), fill="#FFFFFF", anchor="mm")
    # 进度卡
    soft_shadow(img, (32, 452, 748, 584), 24, blur=14, dy=6, alpha=24)
    d.rounded_rectangle([32, 452, 748, 584], radius=24, fill="#FFFFFF")
    d.text((60, 486), "已收 2 / 7 件", font=F(30, True), fill=INK, anchor="lm")
    pill(d, 424, 470, "未收 5 件", "#FFECEC", DANGER, 20, 16, 42)
    d.text((748 - 24, 486), "全部勾选", font=F(23, True), fill=PRIMARY_DEEP, anchor="rm")
    progress_bar(d, 60, 532, 420, 2 / 7, h=12)
    d.text((748 - 24, 538), "29%", font=F(22, True), fill=INK2, anchor="rm")
    # 清单标题
    section_title(d, 32, 620, "出行清单", "按分类分组")
    # 分组卡
    groups = [
        ("证件", "1/2", [("护照", True), ("身份证", False)]),
        ("服装", "1/3", [("短袖", True), ("短裤", False), ("袜子", False)]),
        ("电子设备", "0/2", [("充电器", False), ("相机", False)]),
    ]
    y = 676
    for gi, (gname, gcount, rows) in enumerate(groups):
        h = 66 + len(rows) * 82 + 8
        soft_shadow(img, (32, y, 748, y + h), 22, blur=12, dy=5, alpha=18)
        d.rounded_rectangle([32, y, 748, y + h], radius=22, fill="#FFFFFF")
        d.text((60, y + 36), gname, font=F(27, True), fill=INK, anchor="lm")
        d.text((748 - 26, y + 36), gcount, font=F(22, True), fill=INK2, anchor="rm")
        for ri, (name, checked) in enumerate(rows):
            cy = y + 66 + 41 + ri * 82
            if ri:
                list_divider(d, 60, 720, cy - 41)
            check_row(d, 44, cy - 41, 680, name, checked)
        y += h + 18
    # 底部按钮
    d.rectangle([0, 1512, W, H], fill=BG)
    d.rounded_rectangle([32, 1532, 366, 1632], radius=28, outline=PRIMARY_DEEP, width=3)
    d.text((199, 1582), "导入模板", font=F(30, True), fill=PRIMARY_DEEP, anchor="mm")
    soft_shadow(img, (402, 1532, 748, 1632), 28, blur=16, dy=8, alpha=50)
    d.rounded_rectangle([402, 1532, 748, 1632], radius=28, fill=PRIMARY)
    d.text((575, 1582), "添加物品", font=F(30, True), fill="#FFFFFF", anchor="mm")
    gesture_bar(d)
    return img, d


def screen_plan_detail():
    img, d = draw_plan_detail_base()
    img.convert("RGB").save(os.path.join(OUT, "proto-02-plan-detail.png"))


def dim(img):
    img.alpha_composite(Image.new("RGBA", (W, H), (15, 30, 45, 112)))


def sheet(img, top, radius=40):
    soft_shadow(img, (0, top, W, H + 40), radius, blur=30, dy=-8, alpha=70)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, top, W, H + 60], radius=radius, fill="#FFFFFF",
                        corners=(True, True, False, False))
    d.rounded_rectangle([364, top + 18, 416, top + 26], radius=4, fill="#D8E3EC")
    return d


# ================= 03 添加物品弹层 =================
def screen_item_picker():
    img, _ = draw_plan_detail_base()
    dim(img)
    top = 396
    d = sheet(img, top)
    d.text((40, top + 56), "添加物品", font=F(38, True), fill=INK, anchor="lm")
    icon_close(d, 712, top + 58, INK2, 0.95)
    d.text((40, top + 104), "可多选；已在清单中的物品会自动跳过", font=F(21), fill=INK2, anchor="lm")
    # 搜索
    d.rounded_rectangle([32, top + 140, 748, top + 216], radius=24, fill=FIELD_BG)
    icon_search(d, 78, top + 178, INK2, 0.9)
    d.text((120, top + 178), "搜索物品…", font=F(26), fill=PLACEHOLDER, anchor="lm")
    # 分类 chips
    cx = 32
    for text, active in [("全部", True), ("日用品", False), ("服装", False), ("电子设备", False), ("证件", False)]:
        cx += chip(d, cx, top + 238, text, active, h=54, fs=22, pad=24) + 12
    # 列表
    rows = [
        ("牙刷", "日用品", "selected"),
        ("牙膏", "日用品", "selected"),
        ("短袖", "服装", "disabled"),
        ("短裤", "服装", "disabled"),
        ("驱蚊水", "日用品", "selected"),
        ("充电器", "电子设备", "disabled"),
        ("护照", "证件", "disabled"),
    ]
    y = top + 318
    for name, cat, state in rows:
        selected = state == "selected"
        disabled = state == "disabled"
        if selected:
            d.rounded_rectangle([24, y - 8, 756, y + 80], radius=18, fill="#F2FAFF")
        d.line([(56, y - 8), (56, y + 80)], fill="#FFFFFF", width=0)
        col = PLACEHOLDER if disabled else INK
        d.text((48, y + 18), name, font=F(28, selected or disabled), fill=col, anchor="lm")
        d.text((48, y + 56), cat, font=F(21), fill=PLACEHOLDER, anchor="lm")
        if disabled:
            d.text((700, y + 36), "已在清单", font=F(21), fill=PLACEHOLDER, anchor="rm")
        else:
            checkbox(d, 706, y + 36, selected, 18)
        if name != rows[-1][0]:
            list_divider(d, 48, 720, y + 84)
        y += 94
    # 新建物品
    d.line([(40, top + 990), (740, top + 990)], fill=LINE, width=2)
    d.text((48, top + 1036), "＋ 新建物品「腰包」", font=F(27, True), fill=PRIMARY_DEEP, anchor="lm")
    # 按钮
    soft_shadow(img, (32, 1520, 748, 1624), 28, blur=16, dy=8, alpha=50)
    d.rounded_rectangle([32, 1520, 748, 1624], radius=28, fill=PRIMARY)
    d.text((390, 1572), "添加 3 件", font=F(30, True), fill="#FFFFFF", anchor="mm")
    gesture_bar(d, 1656)
    img.convert("RGB").save(os.path.join(OUT, "proto-03-item-picker.png"))


# ================= 04 导入模板弹层 =================
def screen_template_import():
    img, _ = draw_plan_detail_base()
    dim(img)
    top = 306
    d = sheet(img, top)
    d.text((40, top + 58), "导入模板", font=F(38, True), fill=INK, anchor="lm")
    icon_close(d, 712, top + 60, INK2, 0.95)
    d.text((40, top + 106), "选一份模板，一次性把物品加入本次清单", font=F(21), fill=INK2, anchor="lm")
    cards = [
        ("夏季游", "海边 5 天 · 12 件", "服装 5 · 日用品 4 · 电子设备 3", True, "#4DB6AC"),
        ("短途出差", "2 天 1 夜 · 8 件", "服装 3 · 电子设备 3 · 证件 2", False, "#66CCFF"),
        ("露营装备", "周末露营 · 16 件", "装备 7 · 食品 4 · 日用品 5", False, "#81C784"),
    ]
    y = top + 148
    for name, meta, cats, selected, color in cards:
        h = 118
        if selected:
            d.rounded_rectangle([32, y, 748, y + h], radius=22, fill="#F1FAFF",
                                outline=PRIMARY_DEEP, width=3)
        else:
            d.rounded_rectangle([32, y, 748, y + h], radius=22, fill="#FFFFFF",
                                outline="#E2EBF2", width=2)
        d.rounded_rectangle([48, y + 24, 108, y + 84], radius=18, fill=color)
        icon_layers(d, 78, y + 54, "#FFFFFF", 0.62)
        d.text((130, y + 40), name, font=F(28, True), fill=INK, anchor="lm")
        d.text((130, y + 80), cats, font=F(20), fill=INK2, anchor="lm")
        d.text((716, y + 38), meta.split(" · ")[-1], font=F(21, True), fill=INK2, anchor="rm")
        if selected:
            d.ellipse([686, y + 58, 726, y + 98], fill=PRIMARY_DEEP)
            icon_check(d, 706, y + 78, "#FFFFFF", 0.9, width=5)
        y += h + 14
    # 预览
    d.rounded_rectangle([32, top + 560, 748, top + 910], radius=22, fill="#F7FBFE")
    d.text((56, top + 594), "模板内容（12 件）", font=F(24, True), fill=INK, anchor="lm")
    d.text((724, top + 594), "将跳过 5 件已有", font=F(20), fill=INK2, anchor="rm")
    preview = [
        ("短袖", "服装"), ("短裤", "服装"), ("泳衣", "服装"),
        ("防晒霜", "日用品"), ("驱蚊水", "日用品"), ("洗漱包", "日用品"),
        ("充电器", "电子设备"), ("相机", "电子设备"), ("护照", "证件"),
    ]
    for i, (name, cat) in enumerate(preview):
        col = i % 3
        row = i // 3
        px = 56 + col * 230
        py = top + 650 + row * 62
        d.ellipse([px, py - 5, px + 10, py + 5], fill=PRIMARY)
        d.text((px + 22, py), name, font=F(23), fill=INK, anchor="lm")
        d.text((px + 104, py), cat, font=F(18), fill=PLACEHOLDER, anchor="lm")
    d.text((56, top + 850), "导入后仍可逐项勾选、添加或移除。", font=F(20), fill=INK2, anchor="lm")
    # 提示
    d.rounded_rectangle([32, 1246, 748, 1376], radius=22, fill="#F0F9FF")
    d.ellipse([56, 1286, 96, 1326], fill=PRIMARY_LIGHT)
    icon_suitcase(d, 76, 1306, PRIMARY_DEEP, 0.6)
    d.text((116, 1284), "导入后自动按分类分组，逐项勾选即可打包。", font=F(21), fill=INK3, anchor="lm")
    d.text((116, 1322), "只要还有未勾选项，计划卡片就会显示红点。", font=F(21), fill=INK3, anchor="lm")
    # 按钮
    soft_shadow(img, (32, 1492, 748, 1596), 28, blur=16, dy=8, alpha=50)
    d.rounded_rectangle([32, 1492, 748, 1596], radius=28, fill=PRIMARY)
    d.text((390, 1544), "导入 7 件", font=F(30, True), fill="#FFFFFF", anchor="mm")
    gesture_bar(d, 1640)
    img.convert("RGB").save(os.path.join(OUT, "proto-04-template-import.png"))


# ================= 05 模板列表 =================
def screen_templates():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, "模板", "常带的东西，一次收好")
    d.text((34, 218), "全部模板", font=F(26, True), fill=INK, anchor="lm")
    d.text((748, 218), "4 个模板 · 46 件物品", font=F(22), fill=INK2, anchor="rm")
    cards = [
        ("夏季游", "海边 5 天，轻装出发", "12", "服装 5 · 日用品 4 · 电子 3", "#4DB6AC", "上次使用 10-12"),
        ("短途出差", "2 天 1 夜，只带必要", "8", "服装 3 · 电子 3 · 证件 2", "#66CCFF", "上次使用 09-28"),
        ("露营装备", "周末露营，装备优先", "16", "装备 7 · 食品 4 · 日用 5", "#81C784", "上次使用 08-16"),
        ("回老家", "探亲一周，少而全", "10", "服装 4 · 日用品 3 · 礼品 3", "#FFB74D", "上次使用 05-02"),
    ]
    y = 272
    for name, desc, count, cats, color, used in cards:
        h = 250
        soft_shadow(img, (32, y, 748, y + h), 26, blur=16, dy=7, alpha=24)
        d.rounded_rectangle([32, y, 748, y + h], radius=26, fill="#FFFFFF")
        d.rounded_rectangle([48, y + 22, 132, y + 106], radius=24, fill=color)
        icon_layers(d, 90, y + 62, "#FFFFFF", 0.8)
        d.text((154, y + 40), name, font=F(32, True), fill=INK, anchor="lm")
        d.text((154, y + 80), desc, font=F(22), fill=INK2, anchor="lm")
        d.text((716, y + 40), "%s 件" % count, font=F(22, True), fill=INK2, anchor="rm")
        icon_more(d, 716, y + 104, "#B9C9D6")
        d.line([(48, y + 132), (732, y + 132)], fill=LINE, width=2)
        # 分类 chips
        cx = 48
        for part in cats.split(" · "):
            fs = F(19, True)
            tw = d.textlength(part, font=fs)
            cw = int(tw + 24)
            d.rounded_rectangle([cx, y + 152, cx + cw, y + 192], radius=20, fill="#F3F8FC")
            d.text((cx + cw / 2, y + 171), part, font=fs, fill=INK3, anchor="mm")
            cx += cw + 10
        d.text((48, y + 220), used, font=F(20), fill=PLACEHOLDER, anchor="lm")
        y += h + 16
    fab(d, 1452)
    bottom_nav(d, 1)
    img.convert("RGB").save(os.path.join(OUT, "proto-05-templates.png"))


# ================= 06 模板详情 =================
def screen_template_detail():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    top_bar(d, "夏季游", "more")
    # 信息卡
    soft_shadow(img, (32, 176, 748, 350), 24, blur=14, dy=6, alpha=24)
    d.rounded_rectangle([32, 176, 748, 350], radius=24, fill="#FFFFFF")
    d.rounded_rectangle([52, 198, 116, 262], radius=20, fill="#4DB6AC")
    icon_layers(d, 84, 230, "#FFFFFF", 0.62)
    d.text((136, 214), "夏季游", font=F(30, True), fill=INK, anchor="lm")
    d.text((136, 254), "海边 5 天，轻装出发", font=F(21), fill=INK2, anchor="lm")
    d.text((52, 296), "12 件物品 · 4 个分类", font=F(22), fill=INK3, anchor="lm")
    d.text((724, 296), "上次使用 2026-10-12", font=F(20), fill=PLACEHOLDER, anchor="rm")
    section_title(d, 32, 390, "模板物品", "移除只影响本模板")
    groups = [
        ("服装", "4 件", ["短袖", "短裤", "泳衣", "薄外套"]),
        ("日用品", "3 件", ["防晒霜", "驱蚊水", "洗漱包"]),
        ("电子设备", "2 件", ["充电器", "相机"]),
    ]
    y = 448
    for gname, gcount, rows in groups:
        h = 64 + len(rows) * 78 + 6
        soft_shadow(img, (32, y, 748, y + h), 22, blur=12, dy=5, alpha=18)
        d.rounded_rectangle([32, y, 748, y + h], radius=22, fill="#FFFFFF")
        d.text((60, y + 34), gname, font=F(26, True), fill=INK, anchor="lm")
        d.text((716, y + 34), gcount, font=F(21), fill=INK2, anchor="rm")
        for ri, name in enumerate(rows):
            ry = y + 64 + ri * 78
            if ri:
                list_divider(d, 60, 720, ry - 39)
            d.ellipse([62, ry + 33, 74, ry + 45], fill=PRIMARY)
            d.text((92, ry + 39), name, font=F(26), fill=INK, anchor="lm")
            icon_close(d, 704, ry + 39, "#C2D1DD", 0.72)
        y += h + 16
    d.text((60, y + 8), "还有 3 件（证件）· 继续下滑", font=F(20), fill=PLACEHOLDER, anchor="lm")
    icon_chevron(d, 420, y + 14, PLACEHOLDER, 0.56, down=True)
    # 底部按钮
    d.rectangle([0, 1500, W, H], fill=BG)
    d.rounded_rectangle([32, 1524, 366, 1624], radius=28, outline=PRIMARY_DEEP, width=3)
    d.text((199, 1574), "从计划导入", font=F(28, True), fill=PRIMARY_DEEP, anchor="mm")
    soft_shadow(img, (402, 1524, 748, 1624), 28, blur=16, dy=8, alpha=50)
    d.rounded_rectangle([402, 1524, 748, 1624], radius=28, fill=PRIMARY)
    d.text((575, 1574), "添加物品", font=F(28, True), fill="#FFFFFF", anchor="mm")
    gesture_bar(d, 1650)
    img.convert("RGB").save(os.path.join(OUT, "proto-06-template-detail.png"))


# ================= 07 物品库 =================
def screen_items():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, "物品", "所有可能带出门的东西", right="none")
    d.rounded_rectangle([590, 84, 748, 144], radius=30, fill=PRIMARY_LIGHT)
    d.text((669, 114), "分类管理", font=F(21, True), fill=PRIMARY_DEEP, anchor="mm")
    d.rounded_rectangle([32, 246, 748, 322], radius=24, fill="#FFFFFF")
    icon_search(d, 78, 284, INK2, 0.9)
    d.text((120, 284), "搜索物品…", font=F(25), fill=PLACEHOLDER, anchor="lm")
    cx = 32
    for text, active in [("全部", True), ("日用品", False), ("服装", False), ("电子设备", False), ("证件", False)]:
        cx += chip(d, cx, 342, text, active, h=54, fs=22, pad=24) + 12
    groups = [
        ("日用品", "4", "#66CCFF", [("牙刷", "软毛，旅行装"), ("牙膏", "100g 以内"), ("防晒霜", "SPF50+"), ("驱蚊水", "海边必备")]),
        ("服装", "3", "#4DB6AC", [("短袖", "速干 2 件"), ("短裤", "1 条"), ("薄外套", "早晚温差")]),
    ]
    y = 426
    for gname, gcount, gcolor, rows in groups:
        d.text((34, y + 12), gname, font=F(25, True), fill=INK, anchor="lm")
        d.text((748, y + 12), "%s 件" % gcount, font=F(20), fill=INK2, anchor="rm")
        y += 40
        h = len(rows) * 88 + 12
        soft_shadow(img, (32, y, 748, y + h), 22, blur=10, dy=4, alpha=16)
        d.rounded_rectangle([32, y, 748, y + h], radius=22, fill="#FFFFFF")
        for ri, (name, note) in enumerate(rows):
            ry = y + 6 + ri * 88
            if ri:
                list_divider(d, 56, 724, ry)
            d.ellipse([56, ry + 34, 76, ry + 54], fill=gcolor)
            d.text((96, ry + 34), name, font=F(27, True), fill=INK, anchor="lm")
            d.text((96, ry + 64), note, font=F(20), fill=INK2, anchor="lm")
            icon_chevron(d, 716, ry + 44, "#C2D1DD", 0.9)
        y += h + 22
    d.text((34, y + 8), "共 26 件 · 按分类分组", font=F(20), fill=PLACEHOLDER, anchor="lm")
    fab(d, 1452)
    bottom_nav(d, 2)
    img.convert("RGB").save(os.path.join(OUT, "proto-07-items.png"))


# ================= 08 分类管理 =================
def screen_categories():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    top_bar(d, "物品分类", "more")
    # 右上加号
    d.ellipse([658, 84, 730, 156], fill=PRIMARY_LIGHT)
    icon_plus(d, 694, 120, PRIMARY_DEEP, 0.72)
    # 提示
    d.rounded_rectangle([32, 180, 748, 272], radius=20, fill="#FFF7E8")
    d.text((56, 210), "删除分类后，其中的物品会归入「未分类」，不会丢失。",
           font=F(21), fill="#9A6A1F", anchor="lm")
    d.text((56, 242), "首次启动预置 6 个分类，可随时改名或换色。",
           font=F(20), fill="#B98B45", anchor="lm")
    rows = [
        ("日用品", 6, "#66CCFF"),
        ("服装", 5, "#4DB6AC"),
        ("电子设备", 4, "#81C784"),
        ("证件", 3, "#FFD54F"),
        ("药品", 2, "#F06292"),
        ("其他", 6, "#9575CD"),
    ]
    y = 300
    h = len(rows) * 94 + 12
    soft_shadow(img, (32, y, 748, y + h), 24, blur=14, dy=6, alpha=22)
    d.rounded_rectangle([32, y, 748, y + h], radius=24, fill="#FFFFFF")
    for i, (name, count, color) in enumerate(rows):
        ry = y + 6 + i * 94
        if i:
            list_divider(d, 56, 724, ry)
        d.ellipse([56, ry + 32, 82, ry + 58], fill=color)
        d.text((102, ry + 34), name, font=F(27, True), fill=INK, anchor="lm")
        d.text((102, ry + 66), "%d 件物品" % count, font=F(20), fill=INK2, anchor="lm")
        icon_chevron(d, 716, ry + 47, "#C2D1DD", 0.9)
    y += h + 24
    # 未分类
    d.rounded_rectangle([32, y, 748, y + 94], radius=22, fill="#FFFFFF")
    d.ellipse([56, y + 32, 82, y + 58], fill="#E8EEF3")
    d.text((102, y + 34), "未分类", font=F(27, True), fill=INK, anchor="lm")
    d.text((102, y + 66), "0 件物品 · 删除分类后自动归入", font=F(20), fill=INK2, anchor="lm")
    d.text((716, y + 47), "系统保留", font=F(19), fill=PLACEHOLDER, anchor="rm")
    # 底部说明
    d.rounded_rectangle([32, 1030, 748, 1170], radius=22, fill="#F0F7FC")
    d.text((56, 1066), "分类怎么用", font=F(22, True), fill=PRIMARY_DEEP, anchor="lm")
    d.text((56, 1104), "用于物品筛选与清单分组，不计入清单件数。", font=F(20), fill=INK3, anchor="lm")
    d.text((56, 1138), "改名不改写历史计划；删除后物品归入「未分类」。", font=F(20), fill=INK3, anchor="lm")
    # 新建分类
    d.rounded_rectangle([32, 1490, 748, 1594], radius=28, outline=PRIMARY_DEEP, width=3)
    icon_plus(d, 320, 1542, PRIMARY_DEEP, 0.62)
    d.text((430, 1542), "新建分类", font=F(28, True), fill=PRIMARY_DEEP, anchor="mm")
    gesture_bar(d, 1650)
    img.convert("RGB").save(os.path.join(OUT, "proto-08-categories.png"))


# ================= 09 新建计划 =================
def screen_plan_edit():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    icon_close(d, 58, 118, INK2, 1.0)
    d.text((104, 118), "新建计划", font=F(36, True), fill=INK, anchor="lm")
    d.text((712, 118), "保存", font=F(25, True), fill=PRIMARY_DEEP, anchor="rm")

    def label(y, text, hint=None):
        d.text((34, y), text, font=F(23, True), fill=INK3, anchor="lm")
        if hint:
            d.text((746, y), hint, font=F(19), fill=PLACEHOLDER, anchor="rm")

    def field(y, text, h=88, placeholder=False):
        d.rounded_rectangle([32, y, 748, y + h], radius=24, fill=FIELD_BG)
        d.text((64, y + h // 2), text, font=F(27), fill=PLACEHOLDER if placeholder else INK,
               anchor="lm")

    label(186, "目的地", "必填")
    field(214, "东京")
    label(330, "计划名称", "选填")
    field(358, "东京五日游")
    label(474, "出行时间", "共 5 天")
    d.rounded_rectangle([32, 502, 748, 632], radius=24, fill=FIELD_BG)
    icon_calendar(d, 72, 550, PRIMARY_DEEP, 0.82, 4)
    d.text((114, 544), "2026-10-26  →  2026-10-30", font=F(26, True), fill=INK, anchor="lm")
    d.text((114, 590), "周一出发 · 周五返程", font=F(20), fill=INK2, anchor="lm")
    icon_chevron(d, 704, 566, INK2, 0.9)
    label(674, "备注", "选填 · 200 字内")
    d.rounded_rectangle([32, 702, 748, 862], radius=24, fill=FIELD_BG)
    for i, ln in enumerate(["看展、购物，顺便去镰仓看海。",
                            "住宿已订在上野，交通以地铁为主。"]):
        d.text((64, 742 + i * 42), ln, font=F(25), fill=INK, anchor="lm")
    d.text((720, 830), "38/200", font=F(19), fill=PLACEHOLDER, anchor="rm")
    label(908, "主题色")
    r, gap = 30, 26
    for i, c in enumerate(PALETTE):
        cx = 66 + i * (2 * r + gap)
        if i == 0:
            d.ellipse([cx - r - 9, 966 - r - 9, cx + r + 9, 966 + r + 9], outline=c, width=4)
        d.ellipse([cx - r, 966 - r, cx + r, 966 + r], fill=c)
        if i == 0:
            icon_check(d, cx, 966, "#FFFFFF", 0.9, width=6)
    # 提示卡
    d.rounded_rectangle([32, 1044, 748, 1200], radius=22, fill="#F0F9FF")
    d.text((56, 1078), "创建后可以：", font=F(23, True), fill=PRIMARY_DEEP, anchor="lm")
    d.text((56, 1118), "· 直接添加物品，或从模板一键导入", font=F(21), fill=INK3, anchor="lm")
    d.text((56, 1154), "· 逐项勾选打包，未勾选会在列表显示红点", font=F(21), fill=INK3, anchor="lm")
    # 保存按钮
    soft_shadow(img, (32, 1502, 748, 1606), 28, blur=16, dy=8, alpha=50)
    d.rounded_rectangle([32, 1502, 748, 1606], radius=28, fill=PRIMARY)
    d.text((390, 1554), "创建计划", font=F(31, True), fill="#FFFFFF", anchor="mm")
    gesture_bar(d, 1644)
    img.convert("RGB").save(os.path.join(OUT, "proto-09-plan-edit.png"))


# ================= 10 我的 =================
def screen_settings():
    global _IMG
    img = canvas()
    _IMG = img
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, "我的", "数据都在本机，换机用导出带走", right="none")
    # 统计卡
    soft_shadow(img, (32, 216, 748, 440), 26, blur=16, dy=7, alpha=24)
    d.rounded_rectangle([32, 216, 748, 440], radius=26, fill="#FFFFFF")
    stats = [("3", "出行计划"), ("4", "清单模板"), ("26", "物品"), ("6", "分类")]
    for i, (num, name) in enumerate(stats):
        col, row = i % 2, i // 2
        cx = 210 + col * 358
        cy = 288 + row * 104
        d.text((cx, cy), num, font=F(40, True), fill=INK, anchor="mm")
        d.text((cx, cy + 38), name, font=F(20), fill=INK2, anchor="mm")
        if col == 0:
            d.line([(390, cy - 34), (390, cy + 52)], fill="#EDF3F8", width=2)
    # 数据管理
    d.text((34, 478), "数据管理", font=F(25, True), fill=INK, anchor="lm")
    items = [
        ("导出数据（JSON）", "完整导出分类、物品、模板与计划", "download", PRIMARY_DEEP),
        ("导出压缩包（.gz）", "内容不变，体积更小，适合备份", "download", "#4DB6AC"),
        ("导入数据…", "从备份文件合并或覆盖恢复", "upload", "#FFB74D"),
    ]
    y = 510
    for title, desc, icon, color in items:
        d.rounded_rectangle([32, y, 748, y + 112], radius=22, fill="#FFFFFF")
        d.rounded_rectangle([52, y + 24, 116, y + 88], radius=18, fill=mix(color, "#FFFFFF", 0.86))
        if icon == "download":
            icon_download(d, 84, y + 56, color, 0.85)
        else:
            icon_upload(d, 84, y + 56, color, 0.85)
        d.text((136, y + 40), title, font=F(26, True), fill=INK, anchor="lm")
        d.text((136, y + 76), desc, font=F(19), fill=INK2, anchor="lm")
        icon_chevron(d, 716, y + 56, "#C2D1DD", 0.9)
        y += 124
    # 危险操作
    d.rounded_rectangle([32, y, 748, y + 96], radius=22, fill="#FFFFFF")
    icon_trash(d, 84, y + 48, DANGER, 0.9)
    d.text((136, y + 36), "清空所有数据", font=F(24, True), fill=DANGER, anchor="lm")
    d.text((136, y + 70), "不可恢复，建议先导出备份", font=F(19), fill="#D98A8A", anchor="lm")
    y += 120
    # 关于
    d.text((34, y + 10), "关于", font=F(25, True), fill=INK, anchor="lm")
    y += 42
    for title, value in [("关于寸笺", "版本 1.0"), ("隐私说明", "纯本地 · 无网络权限")]:
        d.rounded_rectangle([32, y, 748, y + 92], radius=22, fill="#FFFFFF")
        d.text((56, y + 46), title, font=F(24), fill=INK, anchor="lm")
        d.text((716, y + 46), value, font=F(19), fill=INK2, anchor="rm")
        icon_chevron(d, 738, y + 46, "#C2D1DD", 0.8)
        y += 104
    d.text((390, 1330), "寸笺 Field Notes · 所有数据仅存于本机",
           font=F(20), fill=PLACEHOLDER, anchor="mm")
    bottom_nav(d, 3)
    img.convert("RGB").save(os.path.join(OUT, "proto-10-settings.png"))


# ================= 11 导入确认对话框 =================
def screen_import_dialog():
    img = canvas()
    d = ImageDraw.Draw(img)
    status_bar(d)
    header(d, "我的", "数据都在本机，换机用导出带走", right="none")
    # 简化背景
    for y in (216, 468):
        d.rounded_rectangle([32, y, 748, y + 210], radius=26, fill="#FFFFFF")
    dim(img)
    # 对话框
    x1, y1, x2, y2 = 60, 470, 720, 1240
    soft_shadow(img, (x1, y1, x2, y2), 40, blur=30, dy=16, alpha=80)
    d.rounded_rectangle([x1, y1, x2, y2], radius=40, fill="#FFFFFF")
    d.ellipse([390 - 44, y1 - 44, 390 + 44, y1 + 44], fill=PRIMARY)
    icon_upload(d, 390, y1 - 2, "#FFFFFF", 1.0)
    d.text((390, y1 + 84), "导入数据", font=F(38, True), fill=INK, anchor="mm")
    d.text((390, y1 + 132), "寸笺-20261026-0100.json · 5.2 KB", font=F(21), fill=INK2, anchor="mm")
    # 文件统计
    d.rounded_rectangle([96, y1 + 164, 684, y1 + 360], radius=22, fill="#F7FBFE")
    stats = [("6", "分类"), ("26", "物品"), ("4", "模板"), ("3", "计划")]
    for i, (num, name) in enumerate(stats):
        cx = 190 + i * 133
        d.text((cx, y1 + 214), num, font=F(34, True), fill=INK, anchor="mm")
        d.text((cx, y1 + 252), name, font=F(19), fill=INK2, anchor="mm")
    d.text((390, y1 + 302), "模板条目 46 条 · 计划条目 25 条", font=F(20), fill=INK3, anchor="mm")
    d.text((390, y1 + 336), "文件校验通过，未发现损坏数据", font=F(18), fill=GREEN, anchor="mm")
    # 说明
    d.text((96, y1 + 400), "合并：只补充本机没有的数据，重复项跳过。", font=F(21), fill=INK3, anchor="lm")
    d.text((96, y1 + 438), "覆盖：先清空本机全部数据，再导入此文件。", font=F(21), fill=DANGER, anchor="lm")
    d.rounded_rectangle([96, y1 + 470, 684, y1 + 528], radius=16, fill="#FFF3F3")
    d.text((390, y1 + 499), "覆盖操作不可恢复，建议先导出一份备份。", font=F(19), fill="#C94A4A", anchor="mm")
    # 按钮
    by = y1 + 570
    d.rounded_rectangle([96, by, 292, by + 88], radius=26, outline="#C9D6E0", width=3)
    d.text((194, by + 44), "取消", font=F(26, True), fill=INK2, anchor="mm")
    d.rounded_rectangle([308, by, 504, by + 88], radius=26, outline=PRIMARY_DEEP, width=3)
    d.text((406, by + 44), "合并", font=F(26, True), fill=PRIMARY_DEEP, anchor="mm")
    d.rounded_rectangle([520, by, 684, by + 88], radius=26, fill=PRIMARY)
    d.text((602, by + 44), "覆盖", font=F(26, True), fill="#FFFFFF", anchor="mm")
    img.convert("RGB").save(os.path.join(OUT, "proto-11-import-dialog.png"))


# ================= 应用图标 =================
def app_icon():
    size = 512
    img = Image.new("RGBA", (size, size), "#66CCFF")
    d = ImageDraw.Draw(img)
    # 渐变感：顶部叠一层浅色
    for i in range(size):
        t = i / size
        c = mix("#7ED7FF", "#3FA9F5", t)
        d.line([(0, i), (size, i)], fill=c)
    # 纸张
    px1, py1, px2, py2 = 116, 96, 396, 416
    d.rounded_rectangle([px1 + 8, py1 + 14, px2 + 8, py2 + 14], radius=44, fill=(30, 70, 110, 60))
    d.rounded_rectangle([px1, py1, px2, py2], radius=44, fill="#FFFFFF")
    # 折角
    d.polygon([(px2 - 96, py1), (px2, py1 + 96), (px2 - 96, py1 + 96)], fill="#E3F4FE")
    # 横线
    for i in range(3):
        y = 196 + i * 54
        w = [190, 220, 150][i]
        d.rounded_rectangle([160, y, 160 + w, y + 18], radius=9, fill="#D6E8F5")
    # 勾选
    d.ellipse([116, 300, 236, 420], fill="#66CCFF")
    icon_check(d, 176, 360, "#FFFFFF", 2.0, width=18)
    img.convert("RGB").save(os.path.join(OUT, "app-icon.png"))


if __name__ == "__main__":
    screen_plans()
    screen_plan_detail()
    screen_item_picker()
    screen_template_import()
    screen_templates()
    screen_template_detail()
    screen_items()
    screen_categories()
    screen_plan_edit()
    screen_settings()
    screen_import_dialog()
    app_icon()
    print("done ->", OUT)
