# -*- coding: utf-8 -*-
"""检查 Markdown 文档中的本地链接与图片引用是否存在。"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")


def main():
    targets = [os.path.join(ROOT, "README.md")]
    targets += [os.path.join(ROOT, "docs", f) for f in sorted(os.listdir(os.path.join(ROOT, "docs")))
                if f.endswith(".md")]
    missing = []
    total = 0
    for path in targets:
        with open(path, encoding="utf-8") as f:
            text = f.read()
        for raw in LINK.findall(text):
            link = raw.strip().split(" ")[0].strip("<>")
            if not link or link.startswith(("http://", "https://", "mailto:", "#")):
                continue
            total += 1
            target = os.path.normpath(os.path.join(os.path.dirname(path), link.split("#")[0]))
            if not os.path.exists(target):
                missing.append((os.path.relpath(path, ROOT), raw, os.path.relpath(target, ROOT)))
    print(f"checked {total} local links in {len(targets)} files")
    if missing:
        for src, raw, target in missing:
            print(f"MISSING  {src}: {raw}  ->  {target}")
        sys.exit(1)
    print("all local links OK")


if __name__ == "__main__":
    main()
