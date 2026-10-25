# -*- coding: utf-8 -*-
"""寸笺 / Field Notes · 功能流程图 + 时序图生成脚本（半平面风）"""
import math
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "images")
os.makedirs(OUT, exist_ok=True)

FONT = "C:/Windows/Fonts/msyh.ttc"
FONT_B = "C:/Windows/Fonts/msyhbd.ttc"

PRIMARY = "#66CCFF"
PRIMARY_DEEP = "#3FA9F5"
PRIMARY_LIGHT = "#E3F4FE"
INK = "#22313F"
INK2 = "#5A6B78"
INK3 = "#7C93A3"
ARROW = "#8FA6B8"
DECIDE_BG = "#FFF6E8"
DECIDE_LINE = "#FFB74D"
LINE = "#DDE9F2"


def F(size, bold=False):
    return ImageFont.truetype(FONT_B if bold else FONT, size)


# ---------------- 基础绘图 ----------------
def seg(d, p1, p2, color, width=5, dashed=False):
    if not dashed:
        d.line([p1, p2], fill=color, width=width)
        return
    x1, y1 = p1
    x2, y2 = p2
    length = math.hypot(x2 - x1, y2 - y1)
    if length == 0:
        return
    dx, dy = (x2 - x1) / length, (y2 - y1) / length
    t = 0.0
    while t < length:
        t2 = min(t + 14, length)
        d.line([(x1 + dx * t, y1 + dy * t), (x1 + dx * t2, y1 + dy * t2)],
               fill=color, width=width)
        t = t2 + 10


def arrow(d, pts, color=ARROW, width=5, dashed=False, head=16):
    for i in range(len(pts) - 1):
        seg(d, pts[i], pts[i + 1], color, width, dashed)
    (x1, y1), (x2, y2) = pts[-2], pts[-1]
    ang = math.atan2(y2 - y1, x2 - x1)
    p1 = (x2 + head * math.cos(ang + math.pi - 0.42),
          y2 + head * math.sin(ang + math.pi - 0.42))
    p2 = (x2 + head * math.cos(ang + math.pi + 0.42),
          y2 + head * math.sin(ang + math.pi + 0.42))
    d.polygon([(x2, y2), p1, p2], fill=color)


def lines_center(d, cx, cy, lines, font, fill, lh=None):
    lh = lh or font.size + 10
    y0 = cy - lh * (len(lines) - 1) / 2
    for i, ln in enumerate(lines):
        d.text((cx, y0 + i * lh), ln, font=font, fill=fill, anchor="mm")


# ---------------- 流程图元素 ----------------
def fc_start(d, box, text, size=30):
    x1, y1, x2, y2 = box
    d.rounded_rectangle(box, radius=(y2 - y1) // 2, fill=PRIMARY)
    lines_center(d, (x1 + x2) // 2, (y1 + y2) // 2, [text], F(size, True), "#FFFFFF")


def fc_proc(d, box, lines, fill="#FFFFFF", outline=PRIMARY_DEEP, size=28, title=None):
    x1, y1, x2, y2 = box
    d.rounded_rectangle(box, radius=18, fill=fill, outline=outline, width=4)
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    if title:
        title_h = 34
        d.text((cx, y1 + 30), title, font=F(26, True), fill=PRIMARY_DEEP, anchor="mm")
        lines_center(d, cx, cy + title_h // 2, lines, F(size), INK, F(size).size + 8)
    else:
        lines_center(d, cx, cy, lines, F(size), INK, F(size).size + 10)


def fc_decide(d, box, text, size=30):
    x1, y1, x2, y2 = box
    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
    pts = [(cx, y1), (x2, cy), (cx, y2), (x1, cy)]
    d.polygon(pts, fill=DECIDE_BG)
    for a, b in zip(pts, pts[1:] + pts[:1]):
        d.line([a, b], fill=DECIDE_LINE, width=4)
    lines_center(d, cx, cy, text.split("\n"), F(size, True), INK, F(size).size + 8)


def label(d, x, y, text, size=24, color=INK2, anchor="mm"):
    d.text((x, y), text, font=F(size), fill=color, anchor=anchor)


def legend(d, x, y):
    d.rounded_rectangle([x, y, x + 520, y + 170], radius=16, fill="#FAFDFF",
                        outline="#D8E3EC", width=2)
    d.rounded_rectangle([x + 26, y + 22, x + 120, y + 58], radius=18, fill=PRIMARY)
    d.text((x + 140, y + 40), "开始 / 结束", font=F(23), fill=INK, anchor="lm")
    d.rounded_rectangle([x + 26, y + 72, x + 120, y + 108], radius=8, fill="#FFFFFF",
                        outline=PRIMARY_DEEP, width=3)
    d.text((x + 140, y + 90), "处理步骤", font=F(23), fill=INK, anchor="lm")
    d.polygon([(x + 73, y + 120), (x + 115, y + 136), (x + 73, y + 152), (x + 31, y + 136)],
              fill=DECIDE_BG, outline=DECIDE_LINE)
    d.text((x + 140, y + 136), "条件判断", font=F(23), fill=INK, anchor="lm")


# ================= 主流程图 =================
def flowchart_main():
    W, H = 2600, 2180
    img = Image.new("RGBA", (W, H), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((80, 58), "寸笺 · 功能流程图", font=F(48, True), fill=INK)
    d.text((80, 128), "启动加载 → 计划列表（状态 / 进度 / 红点）→ 六类用户操作 → Room Flow 自动刷新",
           font=F(26), fill=INK2)
    legend(d, 2000, 50)

    # 业务不变量面板（填充主轴右侧留白）
    d.rounded_rectangle([760, 230, 2020, 650], radius=20, fill="#F7FBFE",
                        outline="#DCEBF6", width=2)
    d.text((800, 272), "五条业务不变量", font=F(30, True), fill=INK, anchor="lm")
    invariants = [
        "红点 = 清单存在未勾选条目（总数 > 0 且 checked < total）",
        "勾选视觉 = 名称中划线 + 文字变灰（#9AB0C0）",
        "条目快照 = 物品名 + 分类名，后续改名不回溯历史",
        "全量迁移 = 分类 / 物品 / 模板 / 计划 四类数据",
        "刷新机制 = Room Flow → ViewModel → Compose 自动重组",
    ]
    for i, text in enumerate(invariants):
        cy = 330 + i * 62
        d.ellipse([800, cy - 15, 830, cy + 15], fill=PRIMARY_LIGHT)
        d.text((815, cy), str(i + 1), font=F(18, True), fill=PRIMARY_DEEP, anchor="mm")
        d.text((850, cy), text, font=F(23), fill=INK2, anchor="lm")

    # 主脊柱
    fc_start(d, (80, 220, 440, 310), "开始 · 启动应用")
    arrow(d, [(260, 310), (260, 360)])
    fc_proc(d, (60, 360, 460, 480), ["初始化 Room 本地数据库", "（首次启动写入预置分类）"])
    arrow(d, [(260, 480), (260, 530)])
    fc_proc(d, (60, 530, 460, 650), ["订阅全部数据（Flow）", "计划 / 模板 / 物品 / 分类"])
    arrow(d, [(260, 650), (260, 700)])
    fc_proc(d, (60, 700, 460, 840), ["组装计划卡片", "状态 · 进度 · 未勾选红点"])
    arrow(d, [(260, 840), (260, 890)])
    fc_proc(d, (60, 890, 460, 1010), ["渲染出行计划列表", "+ 底部四个页签"])
    arrow(d, [(260, 1010), (260, 1060)])
    fc_decide(d, (60, 1060, 460, 1270), "用户操作？")

    # 分支总线
    arrow(d, [(260, 1270), (260, 1335)])
    seg(d, (260, 1335), (2240, 1335), ARROW, 5)
    centers = [440, 800, 1160, 1520, 1880, 2240]
    for cx in centers:
        arrow(d, [(cx, 1335), (cx, 1365)])

    branches = [
        ("① 新建 / 编辑计划",
         ["填写目的地、出行时间", "校验必填项与日期", "INSERT / UPDATE plan"],
         ["跳转计划详情页", "列表由 Flow 自动刷新"]),
        ("② 勾选 / 取消勾选",
         ["点击清单行", "UPDATE checked", "重算进度与红点"],
         ["中划线 + 置灰即时生效", "红点出现 / 消失"]),
        ("③ 添加物品 / 导入模板",
         ["打开物品选择器 / 模板", "按 nameKey 去重", "事务内批量 INSERT"],
         ["清单按分类分组刷新", "Toast 汇总导入 / 跳过数"]),
        ("④ 模板管理",
         ["新建 / 改名 / 删除模板", "从模板或历史计划导入", "移除模板中的条目"],
         ["模板列表即时刷新", "已建计划不受影响"]),
        ("⑤ 物品 / 分类管理",
         ["新增 / 编辑 / 删除物品", "新增 / 改色 / 删除分类", "删除分类 → 未分类"],
         ["物品库即时刷新", "历史清单保留快照"]),
        ("⑥ 数据导出 / 导入",
         ["SAF 选择文件", "JSON 编码 / 解码校验", "合并或覆盖（事务）"],
         ["全量数据恢复", "Flow 驱动全界面刷新"]),
    ]
    for cx, (title, body, result) in zip(centers, branches):
        box = (cx - 155, 1365, cx + 155, 1555)
        fc_proc(d, box, body, fill="#F7FCFF", title=title, size=22)
        arrow(d, [(cx, 1555), (cx, 1600)])
        fc_proc(d, (cx - 155, 1600, cx + 155, 1710), result, fill="#FFFFFF",
                outline="#B9D9EE", size=21)
        arrow(d, [(cx, 1710), (cx, 1820)], color="#7FC4EA", dashed=True)

    # 回流
    seg(d, (2240, 1820), (40, 1820), "#7FC4EA", 4, dashed=True)
    seg(d, (40, 1820), (40, 950), "#7FC4EA", 4, dashed=True)
    arrow(d, [(40, 950), (60, 950)], color="#7FC4EA", dashed=True)
    d.text((1330, 1862), "Room Flow 发射最新数据 → UI 自动重组（无需手动刷新）",
           font=F(25, True), fill="#2E86B8", anchor="mm")

    # 说明
    d.rounded_rectangle([60, 1910, 2540, 2110], radius=18, fill="#F7FBFE", outline="#DCEBF6", width=2)
    d.text((92, 1946), "红点规则：计划清单条目总数 > 0 且存在未勾选项时，计划卡片显示红点；全部勾选后红点立即消失。",
           font=F(23), fill=INK2, anchor="lm")
    d.text((92, 1988), "计划状态：今天 < 出发日 → 未开始；出发日 ≤ 今天 ≤ 返程日 → 进行中；今天 > 返程日 → 已结束。",
           font=F(23), fill=INK2, anchor="lm")
    d.text((92, 2030), "数据边界：所有数据仅存本机，不申请网络权限；导入导出通过系统文件选择器（SAF）完成。",
           font=F(23), fill=INK2, anchor="lm")
    d.text((92, 2072), "快照规则：计划 / 模板条目保存物品名与分类名快照，后续改名或删除物品不会改写历史数据。",
           font=F(23), fill=INK2, anchor="lm")
    d.text((80, 2140), "注：应用退出后流程结束；数据已持久化在本机 Room 数据库。",
           font=F(22), fill=INK3)
    img.convert("RGB").save(os.path.join(OUT, "flowchart.png"))
    print("flowchart ok")


# ================= 计划详情子流程图 =================
def flowchart_detail():
    W, H = 2280, 1640
    img = Image.new("RGBA", (W, H), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((70, 56), "计划详情 · 子流程图", font=F(44, True), fill=INK)
    d.text((70, 120), "进入计划详情后可执行的六类操作，以及每次操作后的界面反馈",
           font=F(24), fill=INK2)

    fc_start(d, (80, 180, 440, 270), "进入计划详情页")
    arrow(d, [(260, 270), (260, 320)])
    fc_proc(d, (50, 320, 470, 490),
            ["展示 Hero 卡", "（目的地 / 时间 / 状态）", "进度条（已收 / 总数）", "按分类分组的勾选清单"],
            size=22)
    arrow(d, [(260, 490), (260, 535)])
    fc_decide(d, (50, 535, 470, 735), "用户操作？")

    arrow(d, [(260, 735), (260, 780)])
    seg(d, (260, 780), (1980, 780), ARROW, 5)
    centers = [300, 636, 972, 1308, 1644, 1980]
    for cx in centers:
        arrow(d, [(cx, 780), (cx, 815)])

    ops = [
        ("① 勾选 / 取消", ["点击清单行", "UPDATE checked", "重算进度 / 红点"], "中划线 + 灰色 / 恢复"),
        ("② 添加物品", ["多选物品", "按 nameKey 去重", "事务批量 INSERT"], "清单即时刷新"),
        ("③ 导入模板", ["选择一份模板", "复制模板条目", "去重后 INSERT"], "Toast「导入 N 件」"),
        ("④ 存为模板", ["输入模板名称", "复制全部条目快照", "INSERT 模板 + 条目"], "跳转模板详情"),
        ("⑤ 编辑计划", ["表单回填", "校验目的地 / 日期", "UPDATE plan"], "返回详情并刷新"),
        ("⑥ 删除计划", ["二次确认", "DELETE plan", "级联删除 plan_item"], "返回计划列表"),
    ]
    for cx, (title, body, result) in zip(centers, ops):
        fc_proc(d, (cx - 145, 815, cx + 145, 1010), body, fill="#F7FCFF", title=title, size=22)
        arrow(d, [(cx, 1010), (cx, 1055)])
        fc_proc(d, (cx - 145, 1055, cx + 145, 1145), [result], fill="#FFFFFF",
                outline="#B9D9EE", size=20)
        arrow(d, [(cx, 1145), (cx, 1240)], color="#7FC4EA", dashed=True)

    seg(d, (1980, 1240), (40, 1240), "#7FC4EA", 4, dashed=True)
    seg(d, (40, 1240), (40, 560), "#7FC4EA", 4, dashed=True)
    arrow(d, [(40, 560), (50, 560)], color="#7FC4EA", dashed=True)
    d.text((1140, 1282), "操作完成后由 Flow 发射新数据，详情页自动重组", font=F(24, True),
           fill="#2E86B8", anchor="mm")

    d.rounded_rectangle([60, 1340, 2220, 1530], radius=18, fill="#F7FBFE",
                        outline="#DCEBF6", width=2)
    d.text((92, 1376), "移除条目：在清单行上左滑，弹出「已移除 · 撤销」提示；撤销可恢复该条目。",
           font=F(22), fill=INK2, anchor="lm")
    d.text((92, 1416), "全部勾选 / 全部取消：详情页右上角更多菜单，批量更新后同样重算红点。",
           font=F(22), fill=INK2, anchor="lm")
    d.text((92, 1456), "计划清单为空：展示「还没有物品」空状态，提供【添加物品】【导入模板】两个入口。",
           font=F(22), fill=INK2, anchor="lm")
    d.text((70, 1580), "注：删除计划会级联删除其清单条目；模板与物品库不受影响。",
           font=F(21), fill=INK3)
    img.convert("RGB").save(os.path.join(OUT, "flowchart-02-plan-detail.png"))
    print("flowchart-02 ok")


# ---------------- 时序图 ----------------
def seq_base(title, sub, lifes, xs, w):
    msgs = []

    def add(kind, a=0, b=0, text=""):
        msgs.append((kind, a, b, text))

    return w, msgs, add


def draw_sequence(filename, title, sub, lifes, xs, w, msgs):
    section_count = sum(1 for m in msgs if m[0] == "sec")
    msg_count = sum(1 for m in msgs if m[0] != "sec")
    H = 360 + section_count * 64 + msg_count * 80 + 120
    img = Image.new("RGBA", (w, H), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((60, 46), title, font=F(42, True), fill=INK)
    d.text((62, 108), sub, font=F(22), fill=INK2)

    box_w = 245 if len(xs) == 7 else 265
    for (name, kind), x in zip(lifes, xs):
        if kind == "ui":
            fill, outline, tc = PRIMARY, None, "#FFFFFF"
        elif kind == "actor":
            fill, outline, tc = PRIMARY_DEEP, None, "#FFFFFF"
        elif kind == "db":
            fill, outline, tc = "#E3F4FE", PRIMARY_DEEP, INK
        else:
            fill, outline, tc = "#FFFFFF", PRIMARY_DEEP, INK
        d.rounded_rectangle([x - box_w // 2, 150, x + box_w // 2, 245], radius=14,
                            fill=fill, outline=outline, width=3 if outline else 1)
        lines = name.split("\n")
        lines_center(d, x, 197, lines, F(24, True), tc, 32)

    for x in xs:
        seg(d, (x, 255), (x, H - 75), "#B9C9D6", 3, dashed=True)

    y = 300
    n = 0
    for kind, a, b, text in msgs:
        if kind == "sec":
            d.text((60, y), text, font=F(26, True), fill=PRIMARY_DEEP)
            d.line([(52, y + 34), (w - 52, y + 34)], fill=LINE, width=3)
            y += 64
            continue
        n += 1
        if kind == "self":
            x = xs[a]
            arrow(d, [(x, y), (x + 62, y), (x + 62, y + 32), (x + 8, y + 32)],
                  color=PRIMARY_DEEP, width=4, head=13)
            label = "%d: %s" % (n, text)
            tw = d.textlength(label, font=F(19))
            d.rounded_rectangle([x + 70, y + 2, x + 70 + tw + 12, y + 30], radius=6, fill="#FFFFFF")
            d.text((x + 76, y + 16), label, font=F(19), fill=INK, anchor="lm")
        else:
            x1, x2 = xs[a], xs[b]
            ret = (kind == "ret")
            arrow(d, [(x1, y), (x2, y)], color="#9AB0C0" if ret else PRIMARY_DEEP,
                  width=4, dashed=ret, head=13)
            token = "%d: %s" % (n, text)
            lines = token.split("\n")
            lh = 23
            ty = y - 20 - lh * (len(lines) - 1)
            widths = [d.textlength(ln, font=F(18)) for ln in lines]
            maxw = max(widths)
            cx = (x1 + x2) / 2
            top = ty - 11
            bottom = ty + lh * (len(lines) - 1) + 11
            d.rounded_rectangle([cx - maxw / 2 - 6, top, cx + maxw / 2 + 6, bottom],
                                radius=6, fill="#FFFFFF")
            for i, ln in enumerate(lines):
                d.text((cx, ty + i * lh), ln, font=F(18),
                       fill=INK if not ret else INK2, anchor="mm")
        y += 80

    d.text((60, H - 54), "注：实线箭头为调用 / 数据流，虚线箭头为返回；前缀数字为消息顺序。",
           font=F(20), fill=INK3)
    img.convert("RGB").save(os.path.join(OUT, filename))
    print(filename, "ok, H =", H)


def seq_load_toggle():
    lifes = [("用户", "actor"), ("Compose UI\n出行列表", "ui"), ("PlanList\nViewModel", "box"),
             ("Repository", "box"), ("Room DAO", "box"), ("SQLite", "db")]
    xs = [160, 480, 800, 1120, 1440, 1760]
    w = 1920
    msgs = [
        ("sec", 0, 0, "阶段一：启动加载与红点计算"),
        ("msg", 0, 1, "打开应用"),
        ("msg", 1, 2, "observePlans()"),
        ("msg", 2, 3, "observePlanSummaries()"),
        ("msg", 3, 4, "observeWithItems()"),
        ("msg", 4, 5, "SELECT plan JOIN plan_item"),
        ("ret", 5, 4, "结果集"),
        ("ret", 4, 3, "Flow<List<PlanWithItems>>"),
        ("ret", 3, 2, "持续发射最新数据"),
        ("self", 2, 0, "组装状态 / 进度 / 红点"),
        ("ret", 2, 1, "StateFlow<PlansUiState>"),
        ("msg", 1, 0, "渲染计划卡片与红点"),
        ("sec", 0, 0, "阶段二：勾选 / 取消勾选与红点即时更新"),
        ("msg", 0, 1, "点击清单行"),
        ("msg", 1, 2, "toggleItem(planId, itemId)"),
        ("msg", 2, 3, "setChecked(itemId, true)"),
        ("msg", 3, 4, "updateChecked(id, 1)"),
        ("msg", 4, 5, "UPDATE plan_item SET ..."),
        ("ret", 5, 4, "影响行数 = 1"),
        ("ret", 4, 3, "Flow 重新发射"),
        ("ret", 3, 2, "新数据"),
        ("self", 2, 0, "重算 checked/total 与 hasUnchecked"),
        ("ret", 2, 1, "新 UiState"),
        ("msg", 1, 0, "中划线 + 灰色；红点消失"),
    ]
    draw_sequence("seq-01-load-toggle.png", "时序图 ① 启动加载 · 勾选与红点",
                  "参与者：用户 / Compose 出行列表 / ViewModel / Repository / Room DAO / SQLite",
                  lifes, xs, w, msgs)


def seq_create_import():
    lifes = [("用户", "actor"), ("Compose UI\n计划页", "ui"), ("PlanDetail\nViewModel", "box"),
             ("Repository", "box"), ("Room DAO", "box"), ("SQLite", "db")]
    xs = [160, 480, 800, 1120, 1440, 1760]
    w = 1920
    msgs = [
        ("sec", 0, 0, "阶段一：新建出行计划"),
        ("msg", 0, 1, "点击「＋」填写表单"),
        ("msg", 1, 2, "savePlan(input)"),
        ("self", 2, 0, "校验目的地 / 日期区间"),
        ("msg", 2, 3, "createPlan(plan)"),
        ("msg", 3, 4, "insertPlan(entity)"),
        ("msg", 4, 5, "INSERT INTO plan"),
        ("ret", 5, 4, "rowId"),
        ("ret", 4, 3, "planId"),
        ("ret", 3, 2, "planId"),
        ("ret", 2, 1, "创建成功事件"),
        ("msg", 1, 0, "跳转空清单详情页"),
        ("sec", 0, 0, "阶段二：导入模板（批量、去重、事务）"),
        ("msg", 0, 1, "点击「导入模板」并选中模板"),
        ("msg", 1, 2, "importTemplate(planId, templateId)"),
        ("msg", 2, 3, "importTemplate(...)"),
        ("msg", 3, 4, "getTemplateItems(templateId)"),
        ("msg", 4, 5, "SELECT template_item"),
        ("ret", 5, 4, "模板条目列表"),
        ("self", 3, 0, "按 nameKey 过滤计划内已有条目"),
        ("msg", 3, 4, "insertPlanItems(去重后条目)"),
        ("msg", 4, 5, "BEGIN / INSERT / COMMIT"),
        ("ret", 5, 4, "提交成功"),
        ("ret", 4, 3, "Flow 发射 plan_item"),
        ("ret", 3, 2, "最新清单"),
        ("self", 2, 0, "重算进度 / 红点"),
        ("ret", 2, 1, "UiState"),
        ("msg", 1, 0, "分组清单 + 红点 + Toast"),
    ]
    draw_sequence("seq-02-create-import-template.png", "时序图 ② 新建计划 · 导入模板",
                  "参与者：用户 / Compose 计划页 / ViewModel / Repository / Room DAO / SQLite",
                  lifes, xs, w, msgs)


def seq_transfer():
    lifes = [("用户", "actor"), ("设置页\n（SAF）", "ui"), ("Transfer\nViewModel", "box"),
             ("Repository", "box"), ("ExportCodec", "box"), ("Room DAO", "box"), ("SQLite", "db")]
    xs = [150, 460, 770, 1080, 1390, 1700, 2010]
    w = 2160
    msgs = [
        ("sec", 0, 0, "阶段一：全量导出"),
        ("msg", 0, 1, "点击「导出数据（JSON）」"),
        ("msg", 1, 2, "export(gzip = false)"),
        ("msg", 2, 3, "loadSnapshot()"),
        ("msg", 3, 5, "查询六张表"),
        ("msg", 5, 6, "SELECT category / item / ..."),
        ("ret", 6, 5, "全部记录"),
        ("ret", 5, 3, "Snapshot"),
        ("ret", 3, 2, "Snapshot"),
        ("msg", 2, 4, "encode(snapshot)"),
        ("ret", 4, 2, "JSON bytes（可选 gzip）"),
        ("msg", 2, 1, "createDocument(文件名)"),
        ("msg", 1, 0, "系统保存面板"),
        ("msg", 0, 1, "选择目录并确认"),
        ("msg", 1, 2, "写入 bytes"),
        ("ret", 2, 1, "导出成功 + 字节数"),
        ("sec", 0, 0, "阶段二：全量导入"),
        ("msg", 0, 1, "点击「导入数据…」选择文件"),
        ("msg", 1, 2, "import(uri)"),
        ("msg", 2, 1, "读取 bytes"),
        ("ret", 1, 2, "bytes"),
        ("msg", 2, 4, "decodeAndValidate(bytes)"),
        ("self", 4, 0, "识别 gzip / 校验 format 与版本"),
        ("ret", 4, 2, "ImportPlan + 统计"),
        ("ret", 2, 1, "预览计数"),
        ("msg", 1, 0, "显示「合并 / 覆盖」对话框"),
        ("msg", 0, 1, "选择「合并」"),
        ("msg", 1, 2, "commit(MERGE)"),
        ("msg", 2, 3, "importAll(plan, MERGE)"),
        ("msg", 3, 5, "事务：按业务键去重插入"),
        ("msg", 5, 6, "BEGIN / INSERT / COMMIT"),
        ("ret", 6, 5, "成功"),
        ("ret", 5, 3, "Flow 发射"),
        ("ret", 3, 2, "导入结果"),
        ("ret", 2, 1, "关闭对话框 + Toast"),
        ("msg", 1, 0, "全界面数据刷新"),
    ]
    draw_sequence("seq-03-transfer.png", "时序图 ③ 全量导出 · 导入恢复",
                  "参与者：用户 / 设置页（SAF） / TransferViewModel / Repository / ExportCodec / Room DAO / SQLite",
                  lifes, xs, w, msgs)


if __name__ == "__main__":
    flowchart_main()
    flowchart_detail()
    seq_load_toggle()
    seq_create_import()
    seq_transfer()
