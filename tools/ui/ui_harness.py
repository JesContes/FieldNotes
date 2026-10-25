"""ui_harness.py — 无截图 UI 断言与交互层（Python 版）

对应《09 · 测试效率优化方案》的 P1 层：把「点一次 → 截一屏 → 看一眼」的
人工巡场，换成「一次调用跑完一个场景，输出 PASS/FAIL 文本」。

设计要点
--------
1. 断言只读布局树文本（实测 ~1.1 s / 6-8 KB），截图仅在失败时留证。
2. android layout 一律带硬超时 + 两级重试；超时连子进程树一起杀。
   （原会话 03:30 的 2 分钟挂死，就是因为裸调 CLI 没有超时。）
3. CLI / SDK 探测覆盖所有已知安装位置，绝不现场下载安装。
   （原会话 02:28 花了 3.5 分钟重装了一个本来就存在的 CLI。）
4. 点击用语义定位：先从布局树找到节点，再用它自己的 bounds 中心去点。
   （原会话 76 次坐标硬编码，导致 04:00 「撤销」误中相邻行。）
5. 中文全程按 UTF-8 处理，不受控制台代码页影响。

用法
----
    from ui_harness import Harness
    h = Harness()
    h.start_app(wait_for="寸笺")
    h.check_text("出行")
    ok = h.finish()
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass, field
from pathlib import Path

IS_WINDOWS = os.name == "nt"

# --------------------------------------------------------------------------
# 环境解析
# --------------------------------------------------------------------------

_SDK_CANDIDATES = ("ANDROID_HOME", "ANDROID_SDK_ROOT")


def resolve_sdk(explicit: str | None = None) -> Path:
    """找到同时具备 adb 与 android CLI 的 SDK 根目录。

    注意这里刻意包含 cmdline-tools/latest/bin/android.exe 的检查——原会话只查了
    PATH，于是判定「CLI 还没装」，实际它早就在 SDK 里。
    """
    cands: list[str] = []
    if explicit:
        cands.append(explicit)
    for env in _SDK_CANDIDATES:
        if os.environ.get(env):
            cands.append(os.environ[env])
    cands.append(r"D:\Android\Sdk")
    cands.append(os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk"))

    for c in cands:
        if not c:
            continue
        root = Path(c)
        if (root / "platform-tools" / "adb.exe").exists() and (
            root / "cmdline-tools" / "latest" / "bin" / "android.exe"
        ).exists():
            return root
    raise RuntimeError(
        "未找到可用的 Android SDK（需同时含 platform-tools/adb.exe 与 "
        f"cmdline-tools/latest/bin/android.exe）。已尝试: {cands}"
    )


def _decode(raw: bytes) -> str:
    """CLI 的 stdout 在 zh-CN 控制台是 GBK，优先 UTF-8，失败回退 GBK。"""
    for enc in ("utf-8", "gbk"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def _run_bounded(cmd: list[str], timeout: float) -> tuple[int, str, str, bool]:
    """带硬超时的执行；超时则连同子进程树一起杀掉。

    返回 (returncode, stdout, stderr, timed_out)。
    """
    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if IS_WINDOWS else 0,
    )
    try:
        out, err = proc.communicate(timeout=timeout)
        return proc.returncode, _decode(out or b""), _decode(err or b""), False
    except subprocess.TimeoutExpired:
        _kill_tree(proc)
        try:
            out, err = proc.communicate(timeout=5)
        except Exception:
            out, err = b"", b""
        return -1, _decode(out or b""), _decode(err or b""), True


def _kill_tree(proc: subprocess.Popen) -> None:
    if IS_WINDOWS:
        subprocess.run(
            ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
            capture_output=True,
            check=False,
        )
    else:
        proc.kill()


# --------------------------------------------------------------------------
# 布局树节点
# --------------------------------------------------------------------------

_BOUNDS_RE = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")
_CENTER_RE = re.compile(r"\[(-?\d+),(-?\d+)\]")


@dataclass
class Node:
    cls: str = ""
    text: str = ""
    desc: str = ""
    rid: str = ""
    bounds: str = ""
    center: tuple[int, int] | None = None
    clickable: bool = False

    @property
    def haystack(self) -> str:
        return f"{self.text} {self.desc} {self.rid}"

    def __str__(self) -> str:
        return (
            f"{self.cls} | text={self.text!r} | desc={self.desc!r} | "
            f"id={self.rid} | bounds={self.bounds} | clickable={'yes' if self.clickable else 'no'}"
        )


@dataclass
class Result:
    name: str
    passed: bool
    detail: str = ""
    elapsed_ms: int = 0


# --------------------------------------------------------------------------
# Harness
# --------------------------------------------------------------------------


class Harness:
    """设备 + 断言记录器。一个实例跑一个场景。"""

    def __init__(
        self,
        sdk: str | None = None,
        serial: str | None = None,
        layout_timeout: float = 15.0,
        work_dir: str | None = None,
        verbose: bool = True,
    ) -> None:
        self.sdk = resolve_sdk(sdk)
        self.adb = self.sdk / "platform-tools" / "adb.exe"
        self.cli = self.sdk / "cmdline-tools" / "latest" / "bin" / "android.exe"
        self.serial = serial or self._first_device()
        self.layout_timeout = layout_timeout
        self.verbose = verbose

        base = Path(work_dir) if work_dir else Path(tempfile.gettempdir()) / "ui-harness"
        self.work_dir = base
        self.layout_path = base / f"layout-{self.serial}.json"
        self.shot_dir = base / "shots"
        self.shot_dir.mkdir(parents=True, exist_ok=True)

        self.nodes: list[Node] = []
        self.results: list[Result] = []
        self._layout_ms: int = 0
        # 屏幕缓存是否失效：只有交互（点击/输入/启停应用）才会置脏。
        # 连续断言共享同一份布局树，避免「每条断言都 dump 一次」——实测这能把
        # 一个 19 条断言的场景从 ~23 s 压到 ~6 s。
        self._dirty: bool = True

    # ---------------------------------------------------------------- 基础

    def _first_device(self) -> str:
        rc, out, _, _ = _run_bounded([str(self.adb), "devices"], 15)
        for line in out.splitlines():
            m = re.match(r"^(\S+)\s+device$", line.strip())
            if m:
                return m.group(1)
        raise RuntimeError("没有在线设备：`adb devices` 为空")

    def adb_shell(self, *args: str, timeout: float = 20.0) -> str:
        rc, out, err, to = _run_bounded(
            [str(self.adb), "-s", self.serial, "shell", *args], timeout
        )
        if to:
            raise TimeoutError(f"adb shell {' '.join(args)} 超时 {timeout}s")
        return out + err

    # ---------------------------------------------------------------- 布局树

    def layout(self) -> list[Node]:
        """抓一次布局树。~1.1s；带硬超时，超时自动换 --flat 重试一次。"""
        attempts: list[list[str]] = [[], ["--flat"]]
        errors: list[str] = []

        for extra in attempts:
            self.layout_path.parent.mkdir(parents=True, exist_ok=True)
            if self.layout_path.exists():
                self.layout_path.unlink(missing_ok=True)

            cmd = [str(self.cli), f"--sdk={self.sdk}", "layout", *extra, "-o", str(self.layout_path)]
            t0 = time.perf_counter()
            rc, out, err, timed_out = _run_bounded(cmd, self.layout_timeout)
            self._layout_ms = int((time.perf_counter() - t0) * 1000)

            if timed_out:
                errors.append(f"超时 {self.layout_timeout}s（{' '.join(extra) or '默认'}）")
                continue
            if not self.layout_path.exists():
                errors.append(f"rc={rc} 未产出文件：{(err or out).strip()[:200]}")
                continue

            raw = self.layout_path.read_text(encoding="utf-8", errors="replace")
            if not raw.lstrip().startswith("["):
                errors.append(f"输出非 JSON：{raw[:120]!r}")
                continue

            try:
                self.nodes = self._flatten(json.loads(raw))
            except json.JSONDecodeError as exc:
                errors.append(f"JSON 解析失败：{exc}")
                continue
            self._dirty = False
            return self.nodes

        raise RuntimeError("android layout 抓取失败：" + " | ".join(errors))

    @staticmethod
    def _flatten(windows: list[dict]) -> list[Node]:
        out: list[Node] = []

        def walk(node: dict | None) -> None:
            if not isinstance(node, dict):
                return
            text = node.get("text") or ""
            desc = node.get("content-desc") or ""
            rid = node.get("resource-id") or ""
            if text or desc or rid:
                center = None
                m = _CENTER_RE.search(node.get("center") or "")
                if m:
                    center = (int(m.group(1)), int(m.group(2)))
                out.append(
                    Node(
                        cls=node.get("class") or "",
                        text=text,
                        desc=desc,
                        rid=rid,
                        bounds=node.get("bounds") or "",
                        center=center,
                        clickable="CLICKABLE" in (node.get("interactions") or []),
                    )
                )
            for key in ("content", "children"):
                kids = node.get(key)
                if isinstance(kids, list):
                    for kid in kids:
                        walk(kid)

        for win in windows or []:
            walk(win)
        return out

    def find(
        self,
        pattern: str,
        *,
        regex: bool = False,
        exact: bool = False,
        clickable_only: bool = False,
        refresh: bool | None = None,
    ) -> list[Node]:
        """在布局树里找节点。

        refresh=None（默认）表示「只在屏幕脏了才重新抓」——交互后自动刷新，
        纯断言不重复抓。需要强制重抓时传 refresh=True。
        """
        if refresh is True or (refresh is None and self._dirty) or not self.nodes:
            self.layout()
        matcher = (
            (lambda s: re.search(pattern, s) is not None)
            if regex
            else (
                (lambda s: s == pattern)
                if exact
                else (lambda s: pattern.lower() in s.lower())
            )
        )
        hits = []
        for n in self.nodes:
            if clickable_only and not n.clickable:
                continue
            if matcher(n.text) or matcher(n.desc) or matcher(n.rid):
                hits.append(n)
        return hits

    # ---------------------------------------------------------------- 断言

    def _record(self, name: str, passed: bool, detail: str = "") -> bool:
        self.results.append(Result(name=name, passed=passed, detail=detail, elapsed_ms=0))
        if self.verbose:
            tag = "PASS" if passed else "FAIL"
            line = f"    [{tag}] {name}"
            if detail:
                line += f" — {detail}"
            print(line, flush=True)
        return passed

    def check_text(self, pattern: str, *, name: str | None = None, min_hits: int = 1,
                   exact: bool = False) -> bool:
        hits = self.find(pattern, exact=exact)
        ok = len(hits) >= min_hits
        return self._record(
            name or f"出现「{pattern}」",
            ok,
            f"命中 {len(hits)} 处" if ok else "未找到",
        )

    def check_no_text(self, pattern: str, *, name: str | None = None) -> bool:
        hits = self.find(pattern)
        ok = not hits
        return self._record(
            name or f"不出现「{pattern}」",
            ok,
            "确认不存在" if ok else f"误命中 {len(hits)} 处：{hits[0].text}",
        )

    def check_clickable(self, pattern: str, *, name: str | None = None) -> bool:
        hits = self.find(pattern, clickable_only=True)
        ok = bool(hits)
        return self._record(
            name or f"「{pattern}」可点击",
            ok,
            f"可点击节点 {len(hits)} 个" if ok else "未找到可点击节点",
        )

    def check_progress(self, checked: int, total: int, *, name: str | None = None) -> bool:
        """断言进度文案，兼容 `1/7`、`已收 1 / 7`、`已勾选 1/7` 等写法。"""
        hits = self.find(rf"{checked}\s*/\s*{total}", regex=True)
        ok = bool(hits)
        return self._record(
            name or f"进度 {checked}/{total}",
            ok,
            f"匹配：{hits[0].text}" if ok else f"未找到 {checked}/{total}",
        )

    def check_inside_screen(self, pattern: str, *, screen_width: int = 1080,
                            name: str | None = None) -> bool:
        """文字/元素是否越界（文本侧的截断判据之一）。"""
        hits = self.find(pattern)
        if not hits:
            return self._record(name or f"「{pattern}」在屏内", False, "元素不存在")
        bad = []
        for h in hits:
            m = _BOUNDS_RE.search(h.bounds)
            if not m:
                continue
            x1, _, x2, _ = (int(g) for g in m.groups())
            if x2 > screen_width or x1 < 0:
                bad.append(h.bounds)
        ok = not bad
        return self._record(
            name or f"「{pattern}」在屏内",
            ok,
            "在屏内" if ok else f"越界：{bad[0]}",
        )

    def note(self, name: str, detail: str = "") -> None:
        """记录一条不含判定的信息（例如「已切到物品页」）。"""
        if self.verbose:
            print(f"    [INFO] {name}" + (f" — {detail}" if detail else ""), flush=True)

    # ---------------------------------------------------------------- 交互

    def tap(self, pattern: str, *, exact: bool = False, settle: float = 0.5) -> Node:
        """语义定位 + 精确点击：命中节点的 center 就是点击点。"""
        hits = self.find(pattern, exact=exact)
        if not hits:
            raise LookupError(f"点击目标不存在：{pattern!r}")
        clickable = [h for h in hits if h.clickable]
        target = clickable[0] if clickable else hits[0]
        if target.center is None:
            raise RuntimeError(f"节点缺少 center：{target}")
        x, y = target.center
        self.adb_shell("input", "tap", str(x), str(y))
        if settle > 0:
            time.sleep(settle)
        self._dirty = True
        return target

    def input_text(self, value: str, *, selector: str | None = None,
                   submit: bool = False, settle: float = 0.4) -> None:
        if selector:
            self.tap(selector, settle=0.3)
        cmd = [str(self.cli), f"--sdk={self.sdk}", "input", "text", value]
        if submit:
            cmd.append("--submit")
        _run_bounded(cmd, 30)
        if settle > 0:
            time.sleep(settle)
        self._dirty = True

    def wait_for(self, pattern: str, *, timeout: float = 8.0, exact: bool = False) -> bool:
        deadline = time.perf_counter() + timeout
        while True:
            if self.find(pattern, exact=exact, refresh=True):
                return True
            if time.perf_counter() >= deadline:
                return False
            time.sleep(0.4)

    # ---------------------------------------------------------------- 应用

    def start_app(self, package: str = "com.fieldnotes", activity: str = ".MainActivity",
                  *, wait_for: str | None = None, timeout: float = 15.0) -> None:
        """冷启动并轮询锚点。

        固定 sleep 不可靠：实测 800 ms 时只抓到 systemui 窗口（4 个节点）。
        """
        self.adb_shell("am", "force-stop", package)
        self.adb_shell("am", "start", "-n", f"{package}/{activity}")
        self._dirty = True
        self.nodes = []

        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            time.sleep(0.4)
            try:
                self.layout()
            except RuntimeError:
                continue
            if wait_for:
                if self.find(wait_for, refresh=False):
                    return
            elif len(self.nodes) > 5:
                return
        raise TimeoutError(f"应用在 {timeout}s 内未就绪（wait_for={wait_for!r}）")

    def clear_app_data(self, package: str = "com.fieldnotes") -> None:
        self.adb_shell("pm", "clear", package)
        self._dirty = True

    def set_animations(self, scale: float = 0.0) -> None:
        for key in ("window_animation_scale", "transition_animation_scale",
                    "animator_duration_scale"):
            self.adb_shell("settings", "put", "global", key, str(scale))

    def screenshot(self, name: str) -> Path:
        """仅在失败或需要归档时调用。"""
        remote = "/sdcard/_ui.png"
        local = self.shot_dir / f"{name}.png"
        self.adb_shell("screencap", "-p", remote)
        _run_bounded([str(self.adb), "-s", self.serial, "pull", remote, str(local)], 30)
        return local

    # ---------------------------------------------------------------- 报告

    @property
    def passed(self) -> int:
        return sum(1 for r in self.results if r.passed)

    @property
    def failed(self) -> list[Result]:
        return [r for r in self.results if not r.passed]

    def finish(self, *, title: str = "", evidence: bool = True) -> bool:
        total = len(self.results)
        fails = self.failed
        print("", flush=True)
        status = "PASS" if not fails else "FAIL"
        print(f"  {status}: {self.passed}/{total} 通过", flush=True)
        for f in fails:
            print(f"    失败项：{f.name} — {f.detail}", flush=True)
        if fails and evidence:
            try:
                shot = self.screenshot(f"fail-{(title or 'scenario').replace('/', '-')}")
                print(f"    失败截图：{shot}", flush=True)
            except Exception as exc:  # 取证失败不应掩盖断言失败
                print(f"    截图失败：{exc}", flush=True)
        return not fails


def main() -> int:
    """自检入口：python tools/ui/ui_harness.py [--find 文本]"""
    import argparse

    ap = argparse.ArgumentParser(description="UI harness 自检")
    ap.add_argument("--sdk")
    ap.add_argument("--serial")
    ap.add_argument("--find", help="在当前屏幕查找文本并打印节点")
    ap.add_argument("--timeout", type=float, default=15.0)
    args = ap.parse_args()

    h = Harness(sdk=args.sdk, serial=args.serial, layout_timeout=args.timeout)
    t0 = time.perf_counter()
    nodes = h.layout()
    ms = int((time.perf_counter() - t0) * 1000)
    print(f"device={h.serial}  sdk={h.sdk}")
    print(f"layout={h.layout_path}  {len(nodes)} nodes  {ms}ms  "
          f"{h.layout_path.stat().st_size / 1024:.1f} KB")

    if args.find:
        hits = h.find(args.find, refresh=False)
        print(f"\nMATCH {args.find!r}: {len(hits)}")
        for n in hits:
            print(f"  {n}")
        return 0 if hits else 2

    for n in nodes:
        print(f"  {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
