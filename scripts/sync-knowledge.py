# -*- coding: utf-8 -*-
"""
同步知识库脚本
==============

将顶层中文知识库 ``knowledge/`` 同步到后端 classpath
``backend/src/main/resources/knowledge/``。

为什么需要这个脚本
------------------

代码里真正读取的是 ``backend/src/main/resources/knowledge/``（英文文件名，
``KnowledgeBaseServiceImpl.loadMarkdown`` 通过 ``ClassPathResource`` 加载），
但项目顶层还有一个 ``knowledge/`` 中文目录。两份内容**不完全一致**，
且顶层版本通常**更详细**（更全的小节、更多练习题）。

如果不加同步，开发期和评委会看到的版本会错位——本脚本保证两份内容对齐。

同步方向与策略
--------------

- **方向**：顶层 → 内部（顶层是原稿，内部是精简翻译版）。
- **默认行为（``--apply`` 缺省）**：**只做干跑（dry-run）**，打印差异清单，不动文件。
- **映射**（中文 → 英文）写在 ``MAPPING`` 里，**显式声明**每一对的对应关系。
- **拆篇**：顶层 1 篇对应内部 2 篇（``偏差方差与过拟合欠拟合.md`` →
  ``bias-variance.md`` + ``overfitting.md``），用 ``SPLITS`` 声明，并在两份之间
  复制整篇原稿（后续可由人工拆分），自动在两份中插入引用提示。
- **追加策略**（``--apply`` 真正生效时）：把顶层原稿以 ``<!-- top-level source -->``
  分隔块**追加**到内部对应文件末尾，避免覆盖内部已有的精简内容；新增映射的内部
  文件则直接以顶层内容为骨架创建。
- **孤立内部文件**（顶层无对应，例如 ``supervised-learning.md`` / ``overfitting.md``
  / ``_template.md``）：保持原样不动，在报告中标记为"内部独有"。
- **孤立顶层文件**（内部无对应，例如 ``强化学习`` 类内容尚未在内部分篇）：列出但不
  复制，等待人工决策。

使用方法
--------

::

    python scripts/sync-knowledge.py                       # 干跑，打印差异
    python scripts/sync-knowledge.py --apply               # 真正修改文件
    python scripts/sync-knowledge.py --apply --backup     # 修改前备份到 .bak
    python scripts/sync-knowledge.py --report report.md   # 把报告写到文件

依赖：仅 Python 3.8+ 标准库。
"""

from __future__ import annotations

import argparse
import dataclasses
import difflib
import io
import sys
import shutil
from pathlib import Path

# 仓库根目录：本脚本位于 <repo>/scripts/sync-knowledge.py
REPO_ROOT = Path(__file__).resolve().parent.parent

TOP_LEVEL_DIR = REPO_ROOT / "knowledge"
INTERNAL_DIR = REPO_ROOT / "backend" / "src" / "main" / "resources" / "knowledge"

# ------------------------------------------------------------------
# 文件名映射表：顶层中文 → 内部英文（与 seed-knowledge.sql 对齐）
# ------------------------------------------------------------------
# 格式：{(模块编号, 顶层文件名) : (内部模块目录, 内部文件名)}
# 模块编号对应 seed-knowledge.sql 中 knowledge_point.module。
MAPPING: dict[tuple[int, str], tuple[str, str]] = {
    # module 1：基础概念
    (1, "机器学习定义.md"):        ("module1", "ml-definition.md"),
    (1, "数据集划分.md"):          ("module1", "data-split.md"),
    (1, "模型评估指标.md"):        ("module1", "evaluation-metrics.md"),
    # 顶层这一篇在内部被拆成两篇，由 SPLITS 处理
    (1, "偏差方差与过拟合欠拟合.md"): ("module1", "bias-variance.md"),

    # module 2：经典算法
    (2, "线性回归.md"):            ("module2", "linear-regression.md"),
    (2, "逻辑回归.md"):            ("module2", "logistic-regression.md"),
    (2, "决策树.md"):              ("module2", "decision-tree.md"),
    (2, "随机森林.md"):            ("module2", "random-forest.md"),
    (2, "SVM.md"):                 ("module2", "svm.md"),
    (2, "KNN.md"):                 ("module2", "knn.md"),
    (2, "朴素贝叶斯.md"):          ("module2", "naive-bayes.md"),
    (2, "集成学习与XGBoost.md"):    ("module2", "ensemble-learning.md"),

    # module 3：无监督学习
    (3, "K-Means聚类.md"):         ("module3", "k-means.md"),
    (3, "层次聚类.md"):            ("module3", "hierarchical-clustering.md"),
    (3, "PCA主成分分析.md"):       ("module3", "pca.md"),
    (3, "降维与可视化.md"):        ("module3", "dim-reduction.md"),

    # module 4：深度学习
    (4, "神经网络基础.md"):        ("module4", "neural-network.md"),
    (4, "CNN卷积神经网络.md"):     ("module4", "cnn.md"),
    (4, "RNN与LSTM.md"):           ("module4", "rnn-lstm.md"),
    (4, "Transformer基础.md"):     ("module4", "transformer.md"),

    # module 5：实践工具
    (5, "NumPy数据处理.md"):       ("module5", "numpy.md"),
    (5, "Pandas数据处理.md"):      ("module5", "pandas.md"),
    (5, "Scikit-learn模型训练.md"): ("module5", "sklearn.md"),
    (5, "PyTorch入门.md"):         ("module5", "pytorch.md"),

    # module 6：项目实战
    (6, "房价预测.md"):            ("module6", "house-price.md"),
    (6, "手写数字识别.md"):        ("module6", "mnist.md"),
    (6, "客户分群.md"):            ("module6", "customer-segmentation.md"),
    (6, "文本情感分析.md"):        ("module6", "sentiment-analysis.md"),
}

# 拆篇：1 篇顶层原稿 → 2 篇内部文件
# 格式：{顶层文件名 : [内部文件名列表]}
SPLITS: dict[str, list[str]] = {
    "偏差方差与过拟合欠拟合.md": [
        "module1/bias-variance.md",
        "module1/overfitting.md",
    ],
}

# 追加分隔块（用于 --apply 模式时把顶层内容追加到内部文件末尾）
APPEND_SEPARATOR = "\n\n<!-- ============================================ -->\n" \
                   "<!-- 以下内容由 scripts/sync-knowledge.py 同步自顶层原稿 knowledge/ -->\n" \
                   "<!-- 仅供阅读参考；正文以本文件原有章节为准，重复段落由维护者清理。 -->\n" \
                   "<!-- ============================================ -->\n\n"


@dataclasses.dataclass
class DiffRow:
    """一对文件的差异行。"""

    module: int
    top_relpath: str       # 顶层文件相对路径
    internal_relpath: str  # 内部文件相对路径
    top_lines: int
    internal_lines: int
    top_bytes: int
    internal_bytes: int
    status: str            # mapped / split / missing-internal / extra-internal / extra-top-level
    note: str = ""

    @property
    def line_diff(self) -> int:
        return self.top_lines - self.internal_lines


def collect_top_level() -> dict[int, list[Path]]:
    """扫描顶层 knowledge/ 目录，按模块编号（01-基础概念 → 1）分组。"""
    groups: dict[int, list[Path]] = {}
    for sub in sorted(TOP_LEVEL_DIR.iterdir()):
        if not sub.is_dir():
            continue
        # 目录名形如 "01-基础概念"
        prefix = sub.name.split("-", 1)[0]
        try:
            module = int(prefix)
        except ValueError:
            continue
        groups[module] = sorted(sub.glob("*.md"))
    return groups


def collect_internal() -> dict[int, dict[str, Path]]:
    """扫描内部 resources/knowledge/，按 module 子目录分组。"""
    groups: dict[int, dict[str, Path]] = {}
    for sub in sorted(INTERNAL_DIR.iterdir()):
        if not sub.is_dir() or not sub.name.startswith("module"):
            continue
        try:
            module = int(sub.name.replace("module", ""))
        except ValueError:
            continue
        groups[module] = {p.name: p for p in sub.glob("*.md")}
    return groups


def build_diff_rows() -> list[DiffRow]:
    """构造差异清单。"""
    top_groups = collect_top_level()
    internal_groups = collect_internal()

    rows: list[DiffRow] = []
    handled_internal: set[tuple[int, str]] = set()

    # 遍历顶层文件，按映射表匹配
    for module, files in sorted(top_groups.items()):
        for top_path in files:
            top_name = top_path.name
            if (module, top_name) in MAPPING:
                int_subdir, int_name = MAPPING[(module, top_name)]
                int_path = INTERNAL_DIR / int_subdir / int_name
                handled_internal.add((module, int_name))
                if top_name in SPLITS:
                    # 拆篇：顶层 1 篇对应内部多篇，本行只展示原稿，作为索引
                    note = "拆篇：同时同步到 " + ", ".join(SPLITS[top_name])
                    rows.append(DiffRow(
                        module=module,
                        top_relpath=f"knowledge/{module:02d}-{module_dir_name(module)}/{top_name}",
                        internal_relpath=f"backend/.../knowledge/{int_subdir}/{int_name} (split source)",
                        top_lines=count_lines(top_path),
                        internal_lines=count_lines(int_path) if int_path.exists() else 0,
                        top_bytes=top_path.stat().st_size,
                        internal_bytes=int_path.stat().st_size if int_path.exists() else 0,
                        status="split",
                        note=note,
                    ))
                else:
                    rows.append(DiffRow(
                        module=module,
                        top_relpath=f"knowledge/{module:02d}-{module_dir_name(module)}/{top_name}",
                        internal_relpath=f"backend/.../knowledge/{int_subdir}/{int_name}",
                        top_lines=count_lines(top_path),
                        internal_lines=count_lines(int_path),
                        top_bytes=top_path.stat().st_size,
                        internal_bytes=int_path.stat().st_size,
                        status="mapped",
                    ))
            else:
                # 顶层有但内部没有
                rows.append(DiffRow(
                    module=module,
                    top_relpath=f"knowledge/{module:02d}-{module_dir_name(module)}/{top_name}",
                    internal_relpath="(无对应内部文件)",
                    top_lines=count_lines(top_path),
                    internal_lines=0,
                    top_bytes=top_path.stat().st_size,
                    internal_bytes=0,
                    status="extra-top-level",
                    note="顶层有但映射表未声明，需人工确认是否新建内部文件并补 seed-knowledge.sql",
                ))

    # 遍历内部文件，找顶层没有对应的
    for module, files in sorted(internal_groups.items()):
        for int_name, int_path in sorted(files.items()):
            if (module, int_name) in handled_internal:
                continue
            if int_name in SPLITS.get(_reverse_split_lookup(module, int_name), []):
                # 是拆篇的产物之一，跳过（已由 split 行覆盖）
                continue
            rows.append(DiffRow(
                module=module,
                top_relpath="(无对应顶层文件)",
                internal_relpath=f"backend/.../knowledge/module{module}/{int_name}",
                top_lines=0,
                internal_lines=count_lines(int_path),
                top_bytes=0,
                internal_bytes=int_path.stat().st_size,
                status="extra-internal",
                note="内部有但顶层无原稿（独立补写/模板/拆篇产物），保留不动",
            ))

    return rows


def _reverse_split_lookup(module: int, int_name: str) -> str:
    """通过内部文件名反查拆篇源顶层文件名（用于跳过判断）。"""
    for top_name, targets in SPLITS.items():
        for target in targets:
            if target.endswith(f"/{int_name}") or target == int_name:
                return top_name
    return ""


def module_dir_name(module: int) -> str:
    """模块编号 → 顶层目录中文名（用于报告展示）。"""
    return {
        1: "基础概念", 2: "经典算法", 3: "无监督学习",
        4: "深度学习", 5: "实践工具", 6: "项目实战",
    }.get(module, "?")


def count_lines(path: Path) -> int:
    try:
        with path.open("rb") as f:
            return sum(1 for _ in f)
    except FileNotFoundError:
        return 0


def print_report(rows: list[DiffRow]) -> str:
    """生成可读报告，返回字符串。"""
    out = io.StringIO()
    out.write("=" * 78 + "\n")
    out.write("知识库同步差异报告（顶层 knowledge/ → backend .../resources/knowledge/）\n")
    out.write("=" * 78 + "\n\n")

    by_status: dict[str, list[DiffRow]] = {}
    for r in rows:
        by_status.setdefault(r.status, []).append(r)

    out.write(f"总计 {len(rows)} 行；按状态分组：\n")
    for status, items in by_status.items():
        out.write(f"  - {status}: {len(items)}\n")
    out.write("\n")

    # 表格
    headers = ("模块", "状态", "顶层行数", "内部行数", "行差", "顶层路径")
    out.write(f"{headers[0]:<4} {headers[1]:<18} {headers[2]:<8} {headers[3]:<8} {headers[4]:<6} {headers[5]}\n")
    out.write("-" * 78 + "\n")
    for r in rows:
        out.write(
            f"{r.module:<4} {r.status:<18} {r.top_lines:<8} {r.internal_lines:<8} "
            f"{r.line_diff:<+6} {r.top_relpath}\n"
        )
        if r.internal_relpath != "(无对应内部文件)":
            out.write(f"{'':>4} {'':<18} {'':<8} {'':<8} {'':<6}   ↳ {r.internal_relpath}\n")
        if r.note:
            out.write(f"{'':>4} {'':<18} {'':<8} {'':<8} {'':<6}   ℹ {r.note}\n")

    out.write("\n")
    # 总结
    mapped = by_status.get("mapped", [])
    if mapped:
        additions = sum(1 for r in mapped if r.line_diff > 0)
        subtractions = sum(1 for r in mapped if r.line_diff < 0)
        same = len(mapped) - additions - subtractions
        out.write(f"映射对：{len(mapped)}；顶层更详 {additions} / 内部更详 {subtractions} / 一致 {same}\n")
    out.write("\n")
    out.write("建议：\n")
    out.write("  1. 仔细 review 'mapped' 行中内部行数偏少但顶层有大量内容的情况——\n")
    out.write("     这些是评估时可能露馅的'内容缩水'。\n")
    out.write("  2. 'extra-top-level' 行需要在 MAPPING/SPLITS 里补映射，并同步\n")
    out.write("     backend/src/main/resources/db/seed-knowledge.sql 增加 content_path 行。\n")
    out.write("  3. 'extra-internal' 行通常无需处理。\n")
    return out.getvalue()


def apply_sync(rows: list[DiffRow], *, backup: bool) -> list[str]:
    """执行同步：把顶层原稿追加到内部文件末尾（拆篇则复制整篇）。"""
    actions: list[str] = []
    top_groups = collect_top_level()

    for r in rows:
        if r.status == "mapped":
            top_path = resolve_top_path(r.top_relpath)
            int_path = INTERNAL_DIR / r.internal_relpath.split("knowledge/")[-1]
            if not int_path.exists():
                actions.append(f"SKIP (内部文件不存在): {int_path}")
                continue
            backup_path = backup_target(int_path, backup)
            top_text = top_path.read_text(encoding="utf-8")
            int_text = int_path.read_text(encoding="utf-8")
            # 已在末尾追加过则跳过
            if "scripts/sync-knowledge.py 同步自顶层原稿" in int_text:
                actions.append(f"SKIP (已追加): {int_path}")
                continue
            new_text = int_text.rstrip() + APPEND_SEPARATOR + top_text
            int_path.write_text(new_text, encoding="utf-8")
            actions.append(f"APPEND  {top_path.name}  →  {int_path.relative_to(REPO_ROOT)}"
                           + (f"  (backup: {backup_path.name})" if backup else ""))
        elif r.status == "split":
            top_path = resolve_top_path(r.top_relpath)
            top_name = top_path.name
            for target in SPLITS.get(top_name, []):
                int_path = INTERNAL_DIR / target
                if not int_path.exists():
                    actions.append(f"SKIP (内部文件不存在): {int_path}")
                    continue
                backup_path = backup_target(int_path, backup)
                top_text = top_path.read_text(encoding="utf-8")
                int_text = int_path.read_text(encoding="utf-8")
                marker = f"<!-- 拆篇源文件：{top_name}（位于顶层 knowledge/） -->\n\n"
                if marker.strip() in int_text:
                    actions.append(f"SKIP (已注入拆篇源): {int_path}")
                    continue
                # 拆篇：把整篇原稿注入两份内部文件头部，便于人工拆分
                new_text = marker + top_text + "\n\n" + SEPARATOR_BLOCK + int_text
                int_path.write_text(new_text, encoding="utf-8")
                actions.append(f"INJECT  {top_name}  →  {int_path.relative_to(REPO_ROOT)}"
                               + (f"  (backup: {backup_path.name})" if backup else ""))
        # extra-top-level / extra-internal 不动
    return actions


SEPARATOR_BLOCK = "<!-- ============================================ -->\n" \
                  "<!-- 以下为内部原稿（精简翻译版） -->\n" \
                  "<!-- ============================================ -->\n\n"


def resolve_top_path(relpath: str) -> Path:
    """relpath 形如 'knowledge/01-基础概念/xxx.md'。"""
    return REPO_ROOT / relpath


def backup_target(path: Path, backup: bool) -> Path | None:
    if not backup:
        return None
    bak = path.with_suffix(path.suffix + ".bak")
    shutil.copy2(path, bak)
    return bak


def main(argv: list[str] | None = None) -> int:
    # Windows GBK 控制台兼容性：把 stdout 重包为 UTF-8
    try:
        sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass

    parser = argparse.ArgumentParser(description="同步顶层 knowledge/ 到后端 resources/knowledge/")
    parser.add_argument("--apply", action="store_true", help="真正修改文件（默认干跑）")
    parser.add_argument("--backup", action="store_true", help="修改前备份为 .bak")
    parser.add_argument("--report", type=Path, help="把报告写入指定文件")
    parser.add_argument("--diff", action="store_true", help="对每个映射对输出文本差异（unified diff 片段）")
    args = parser.parse_args(argv)

    if not TOP_LEVEL_DIR.exists():
        print(f"ERROR: 顶层知识库不存在: {TOP_LEVEL_DIR}", file=sys.stderr)
        return 1
    if not INTERNAL_DIR.exists():
        print(f"ERROR: 内部知识库不存在: {INTERNAL_DIR}", file=sys.stderr)
        return 1

    rows = build_diff_rows()
    report = print_report(rows)

    if args.diff:
        report += "\n\n" + "=" * 78 + "\n详细差异（unified diff 片段，仅 'mapped' 状态）\n" + "=" * 78 + "\n"
        for r in rows:
            if r.status != "mapped":
                continue
            top_path = resolve_top_path(r.top_relpath)
            int_rel = r.internal_relpath.split("knowledge/")[-1]
            int_path = INTERNAL_DIR / int_rel
            if not int_path.exists():
                continue
            top_lines = top_path.read_text(encoding="utf-8").splitlines(keepends=True)
            int_lines = int_path.read_text(encoding="utf-8").splitlines(keepends=True)
            diff = difflib.unified_diff(
                int_lines, top_lines,
                fromfile=f"internal/{int_path.name}",
                tofile=f"top-level/{top_path.name}",
                n=2,
            )
            report += "".join(diff) + "\n"

    print(report)

    if args.report:
        args.report.write_text(report, encoding="utf-8")
        print(f"[报告已写入] {args.report}")

    if args.apply:
        print("=" * 78)
        print("应用同步 (--apply)")
        print("=" * 78)
        actions = apply_sync(rows, backup=args.backup)
        for a in actions:
            print(a)
        print(f"\n完成，共 {len(actions)} 个动作。")
    else:
        print("=" * 78)
        print("这是干跑（dry-run）。如需真正修改文件，请加 --apply。")
        print("=" * 78)
    return 0


if __name__ == "__main__":
    sys.exit(main())