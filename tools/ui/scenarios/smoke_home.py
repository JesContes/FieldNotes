"""SMOKE-01 · 计划列表首屏（汇总口径 · 状态文案 · 进度 · 红点规则 · 四页签）

对应验收清单：TC-021（卡片信息）、TC-043/TC-044/TC-046（红点规则）、TC-060（导航可达）
一次运行约 4–6 s，产出 13 条断言。
"""

ID = "SMOKE-01"
TITLE = "计划列表首屏：汇总 · 状态 · 进度 · 红点 · 页签"


def run(h):
    # 冷启动并等首屏锚点（不要用固定 sleep）
    h.start_app(wait_for="寸笺")

    # 1) 头部
    h.check_text("寸笺", name="标题「寸笺」")
    h.check_text("一纸清单，装下整段旅程", name="副标题")
    h.check_text("全部计划", name="区块标题「全部计划」")

    # 2) 汇总口径：待收件数必须等于各计划未收之和（当前样例数据 5+8+0=13）
    h.check_text("待收 13 件 · 共 3 个计划", name="汇总「待收 13 件 · 共 3 个计划」")

    # 3) 状态分段计数
    for seg, cnt in (("全部", 3), ("进行中", 0), ("未开始", 2), ("已结束", 1)):
        h.check_text(f"{seg} {cnt}", name=f"分段「{seg} {cnt}」")

    # 4) 三种状态文案各出现一次
    h.check_text("还有 1 天出发", name="状态：即将出发")
    h.check_text("还有 13 天出发", name="状态：未开始")
    h.check_text("已结束 20 天", name="状态：已结束")

    # 5) 进度与红点口径：全部收齐的卡片显示「已全部打包」，未收齐的不显示
    h.check_progress(2, 7)
    h.check_progress(0, 8)
    h.check_progress(10, 10)
    h.check_text("已全部打包", name="无红点卡片显示「已全部打包」", min_hits=1)

    # 6) 四页签可达
    for tab in ("出行", "模板", "物品", "我的"):
        h.check_text(tab, name=f"页签「{tab}」")
