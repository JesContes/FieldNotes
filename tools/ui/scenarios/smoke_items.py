"""SMOKE-02 · 物品页：分类分组 · 计数 · 搜索入口 · 返回计划页

对应验收清单：TC-001/TC-002（物品与分类分组）、TC-003（分类色点所在行）
一次运行约 5–7 s，产出 8 条断言。
"""

ID = "SMOKE-02"
TITLE = "物品页：分类分组 · 计数 · 搜索 · 跨页签返回"


def run(h):
    h.start_app(wait_for="寸笺")

    # 切到「物品」页签
    h.tap("物品", settle=1.0)
    h.check_text("所有可能带出门的东西", name="物品页副标题（确认已切页）")
    h.check_text("分类管理", name="分组管理入口")

    # 六个预置分类（08 清单前置条件：首次启动写入 6 个预置分类）
    for cat in ("日用品", "服装", "电子设备", "证件", "药品"):
        h.check_text(cat, name=f"分类「{cat}」")

    # 分组计数：必须与分组标题同行出现
    h.check_text("6 件", name="分组计数「6 件」", min_hits=2)

    # 已知条目与其备注
    h.check_text("牙刷", name="条目「牙刷」")
    h.check_text("100g 以内", name="条目备注「100g 以内」")

    # 返回计划页，确认状态没有串页
    h.tap("出行", settle=1.0)
    h.check_text("全部计划", name="回到计划页")
