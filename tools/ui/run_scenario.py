"""run_scenario.py — 批量跑 UI 验收场景，一次工具调用得出全部结论。

用法
----
    python tools/ui/run_scenario.py                        # 跑全部场景
    python tools/ui/run_scenario.py smoke_home             # 只跑一个
    python tools/ui/run_scenario.py --filter smoke         # 按文件名过滤
    python tools/ui/run_scenario.py --list                 # 列出场景

产物
----
    android/build/acceptance/junit.xml      # 可被 CI / IDE 直接消费
    android/build/acceptance/report.md      # 人读的汇总
    %TEMP%/ui-harness/shots/                # 仅失败场景留一张截图

退出码：0 全绿 · 1 有用例失败 · 2 环境错误。
"""

from __future__ import annotations

import argparse
import importlib.util
import sys
import time
import traceback
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from ui_harness import Harness, Result  # noqa: E402

SCEN_DIR = Path(__file__).resolve().parent / "scenarios"
DEFAULT_OUT = Path(__file__).resolve().parents[2] / "android" / "build" / "acceptance"


def load_scenario(path: Path):
    spec = importlib.util.spec_from_file_location(f"scen_{path.stem}", path)
    if spec is None or spec.loader is None:
        raise ImportError(f"无法加载场景：{path}")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    if not hasattr(mod, "run"):
        raise AttributeError(f"场景 {path.name} 缺少 run(h) 函数")
    return mod


def render_junit(suites: list[dict], out: Path, total_s: float) -> None:
    root = ET.Element("testsuites", name="fieldnotes-acceptance")
    for s in suites:
        case = ET.SubElement(
            root,
            "testsuite",
            name=f"{s['id']} · {s['title']}",
            tests=str(len(s["results"])),
            failures=str(len([r for r in s["results"] if not r.passed])),
            errors="1" if s.get("error") else "0",
            time=f"{s['elapsed']:.3f}",
        )
        if s.get("error"):
            err = ET.SubElement(case, "error", message=s["error"].splitlines()[0][:300])
            err.text = s["error"]
        for r in s["results"]:
            tc = ET.SubElement(case, "testcase", classname=s["id"], name=r.name,
                               time=f"{r.elapsed_ms / 1000:.3f}")
            if not r.passed:
                ET.SubElement(tc, "failure", message=r.detail or "断言失败").text = r.detail
    out.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(out, encoding="utf-8", xml_declaration=True)


def render_markdown(suites: list[dict], out: Path, total_s: float) -> None:
    lines = ["# UI 验收报告", ""]
    total = sum(len(s["results"]) for s in suites)
    bad = sum(len([r for r in s["results"] if not r.passed]) for s in suites)
    lines += [
        f"- 场景：{len(suites)}",
        f"- 断言：{total}（失败 {bad}）",
        f"- 用时：{total_s:.1f} s",
        "",
        "| 场景 | 用例 | 断言 | 失败 | 用时 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for s in suites:
        fails = len([r for r in s["results"] if not r.passed])
        lines.append(
            f"| {s['id']} | {s['title']} | {len(s['results'])} | {fails} | {s['elapsed']:.1f}s |"
        )
    details = [r for s in suites for r in s["results"] if not r.passed]
    if details:
        lines += ["", "## 失败明细", ""]
        for r in details:
            lines.append(f"- **{r.name}** — {r.detail}")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description="FieldNotes UI 验收场景运行器")
    ap.add_argument("names", nargs="*", help="场景文件名（不含 .py），缺省为全部")
    ap.add_argument("--filter", default="", help="按文件名子串过滤")
    ap.add_argument("--sdk")
    ap.add_argument("--serial")
    ap.add_argument("--timeout", type=float, default=15.0, help="单次 layout 超时（秒）")
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--list", action="store_true", help="只列出场景")
    ap.add_argument("--no-evidence", action="store_true", help="失败时不截图")
    args = ap.parse_args()

    if not SCEN_DIR.exists():
        print(f"场景目录不存在：{SCEN_DIR}")
        return 2

    files = sorted(SCEN_DIR.glob("*.py"))
    if args.names:
        want = {n if n.endswith(".py") else f"{n}.py" for n in args.names}
        files = [f for f in files if f.name in want]
    if args.filter:
        files = [f for f in files if args.filter in f.stem]
    if not files:
        print("没有匹配的场景")
        return 2

    if args.list:
        for f in files:
            mod = load_scenario(f)
            print(f"{getattr(mod, 'ID', f.stem):12s} {getattr(mod, 'TITLE', '')}  ({f.name})")
        return 0

    try:
        # 用一个轻量实例先确认环境（设备在线 / CLI 可用）
        probe = Harness(sdk=args.sdk, serial=args.serial, layout_timeout=args.timeout,
                        verbose=False)
        probe.set_animations(0.0)
        probe.adb_shell("input", "keyevent", "224")  # WAKEUP，避免锁屏干扰
        probe.adb_shell("wm", "dismiss-keyguard")
    except Exception as exc:
        print(f"环境检查失败：{exc}")
        return 2

    suites: list[dict] = []
    t_all = time.perf_counter()

    for f in files:
        mod = load_scenario(f)
        sid = getattr(mod, "ID", f.stem)
        title = getattr(mod, "TITLE", "")
        print(f"\n▶ {sid} · {title}", flush=True)

        t0 = time.perf_counter()
        h = Harness(sdk=args.sdk, serial=args.serial, layout_timeout=args.timeout)
        entry = {"id": sid, "title": title, "results": [], "error": "", "elapsed": 0.0}
        try:
            mod.run(h)
        except Exception:
            entry["error"] = traceback.format_exc()
            print("    场景异常：", flush=True)
            print("    " + entry["error"].replace("\n", "\n    "), flush=True)
            h._record(f"{sid} 场景执行", False, "场景抛出异常")
        entry["elapsed"] = time.perf_counter() - t0
        entry["results"] = list(h.results)
        suites.append(entry)

        h.finish(title=sid, evidence=not args.no_evidence)

    total_s = time.perf_counter() - t_all
    try:
        render_junit(suites, args.out / "junit.xml", total_s)
        render_markdown(suites, args.out / "report.md", total_s)
        print(f"\n报告：{args.out / 'report.md'}")
    except Exception as exc:
        print(f"\n写报告失败：{exc}")

    total = sum(len(s["results"]) for s in suites)
    bad = sum(len([r for r in s["results"] if not r.passed]) for s in suites)
    bad += sum(1 for s in suites if s["error"])
    print(f"总计：{total} 条断言，{bad} 条失败，{total_s:.1f} s", flush=True)
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
