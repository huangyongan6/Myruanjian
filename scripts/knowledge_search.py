# -*- coding: utf-8 -*-
"""
知识库检索核心
==============

替代 Java 后端 ``KnowledgeBaseServiceImpl.loadMarkdownByName`` 的"按名字 LIKE +
截断 1500 字"模式：对查询做关键词检索，召回 Top-K 切片。

设计目标
--------

- **零外部依赖**：仅 Python 3.8+ 标准库。后端环境（即使最小化 Docker）也能跑。
- **不调 LLM**：只负责"找对的内容"，拼 Prompt 仍由 Java 后端做（保持职责单一）。
- **可替换**：检索接口 ``KnowledgeBase.search()`` 是抽象入口，后续想接 Embedding
  / 向量库时只换实现，不动调用方。

检索算法
--------

**BM25 + 中文 bigram 分词**。

- 为什么不上 Embedding：(1) 训练数据是 32 篇机器学习入门 Markdown，体量小，BM25
  已经足够精准；(2) Embedding 需要调外部 API 或下载模型（数百 MB），会拖慢镜像
  构建并增加密钥管理负担；(3) 后续若召回不满意，把 ``search()`` 换成向量版即可。
- 为什么不用 jieba：jieba 装包 ~5MB，纯 Python bigram 在 32 篇文档规模下质量相当，
  启动 0 成本。
- BM25 参数：k1=1.5, b=0.75（Lucene 默认值，已被广泛验证）。

切片策略
--------

按 Markdown 二级标题 ``##`` 切片：

- 每个 chunk 形如 ``{"module": 2, "file": "decision-tree.md", "title": "决策树",
  "section": "信息增益", "text": "...", "chunk_id": "decision-tree.md#信息增益"}``
- 一个文档对应 N 个 chunk（N = ## 段落数 + 1，文档头部算一个 chunk）
- 召回时返回 Top-K chunk + 来源信息，便于 Java 端拼上下文。

CLI 使用
--------

::

    python scripts/knowledge_search.py "什么是过拟合"
    python scripts/knowledge_search.py "梯度下降" --top-k 3 --module 2
    python scripts/knowledge_search.py "CNN 卷积" --json
    python scripts/knowledge_search.py "RNN" --files backend/src/main/resources/knowledge/

作为库使用
----------

::

    from scripts.knowledge_search import KnowledgeBase
    kb = KnowledgeBase.from_directory("backend/src/main/resources/knowledge/")
    hits = kb.search("过拟合怎么办", top_k=5)
    for h in hits:
        print(h.score, h.chunk.chunk_id, h.chunk.title)
"""

from __future__ import annotations

import argparse
import dataclasses
import json
import math
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Iterable


# ------------------------------------------------------------------
# 常量与工具函数
# ------------------------------------------------------------------

# 中文 bigram 提取的正则：连续的 2 个汉字。
_CJK_BIGRAM_RE = re.compile(r"[一-鿿]{2}")
# 英文/数字 token：以字母数字开头，至少 2 字符
_ASCII_TOKEN_RE = re.compile(r"[A-Za-z][A-Za-z0-9_-]+")

# BM25 参数（Lucene 默认）
_BM25_K1 = 1.5
_BM25_B = 0.75


def tokenize(text: str) -> list[str]:
    """分词：英文/数字整词 + 中文 bigram + 中文单字保留为 1-gram 兜底。

    三种粒度混用是为了兼顾：(1) 英文术语召回（"SVM"、"K-Means"）；
    (2) 中文短语（"过拟合"切成 "过拟"+"拟合"，任一字串命中即召回）；
    (3) 中文单字兜底（避免 bigram 完全错位）。
    """
    text = text.lower()
    tokens: list[str] = []
    # 英文 token
    for m in _ASCII_TOKEN_RE.finditer(text):
        tokens.append(m.group(0))
    # 中文 bigram
    for m in _CJK_BIGRAM_RE.finditer(text):
        tokens.append(m.group(0))
    # 中文单字（仅出现在非 bigram 内的孤立字，避免和 bigram 重复）
    # 简化：直接把每个汉字也加进去；重复 token 在 BM25 里 tf 累加有正向作用。
    for ch in re.findall(r"[一-鿿]", text):
        tokens.append(ch)
    return tokens


# ------------------------------------------------------------------
# 数据结构
# ------------------------------------------------------------------

@dataclasses.dataclass
class Chunk:
    """一个 Markdown 切片。"""

    module: int
    file: str                # 相对路径，如 "module2/decision-tree.md"
    title: str               # 文件一级标题（# xxx）
    section: str             # 切片所属二级标题（## yyy），文件头部切片为 ""
    text: str                # 切片正文
    chunk_id: str            # 唯一 id："{file}#{section}"
    weight: float = 1.0      # 召回权重（代码块降权，标题加权）

    def to_dict(self) -> dict:
        return dataclasses.asdict(self)


@dataclasses.dataclass
class Hit:
    chunk: Chunk
    score: float

    def to_dict(self) -> dict:
        return {"score": round(self.score, 4), **self.chunk.to_dict()}


# ------------------------------------------------------------------
# Markdown 切片
# ------------------------------------------------------------------

_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*$", re.MULTILINE)


def split_markdown(md_text: str, file_relpath: str, module: int) -> list[Chunk]:
    """按 ## 切片。"""
    # 提取所有标题的位置
    headings = list(_HEADING_RE.finditer(md_text))
    if not headings:
        # 无标题，整篇作为一个 chunk
        return [Chunk(
            module=module, file=file_relpath, title=_first_line(md_text),
            section="", text=md_text.strip(), chunk_id=f"{file_relpath}#"
        )]

    # 一级标题作为文件名（文件级 title）
    h1 = next((h.group(2).strip() for h in headings if h.group(1) == "#"), "")

    chunks: list[Chunk] = []
    # 先按所有 heading 切段，再按 ## 边界合并
    segments: list[tuple[str, str, int, int]] = []  # (level, title, start, end)
    for i, h in enumerate(headings):
        start = h.end()
        end = headings[i + 1].start() if i + 1 < len(headings) else len(md_text)
        segments.append((h.group(1), h.group(2).strip(), start, end))

    # 把 H1 之外的所有段落分配到最近的 ## section；若没有 ##，则全部归到文件头 chunk
    h2_segments = [(t, s, e) for (lvl, t, s, e) in segments if lvl == "##"]
    if not h2_segments:
        # 没有 ##，整篇一个 chunk
        full_text = md_text.strip()
        if full_text:
            chunks.append(Chunk(
                module=module, file=file_relpath, title=h1 or _first_line(md_text),
                section="", text=full_text,
                chunk_id=f"{file_relpath}#",
            ))
        return chunks

    # 有 ##：H1 到第一个 ## 之间的内容单独作为一个 chunk（"概览"）
    first_h2_start = h2_segments[0][1]
    prelude = md_text[:first_h2_start].strip()
    # 概览 chunk 必须有实质内容（不只是 H1）；否则跳过
    prelude_body_lines = [
        ln for ln in prelude.splitlines()
        if ln.strip() and not _HEADING_RE.match(ln)
    ]
    if "\n".join(prelude_body_lines).strip():
        chunks.append(Chunk(
            module=module, file=file_relpath, title=h1 or _first_line(md_text),
            section="(概览)", text=prelude,
            chunk_id=f"{file_relpath}#(概览)",
            weight=1.2,  # 概览通常是关键介绍，略加权
        ))
    # 每个 ## section 一个 chunk
    for title, s, e in h2_segments:
        section_text = md_text[s:e].strip()
        if not section_text:
            continue
        # 代码块章节降权（避免 "PCA(" 这种 token 在代码里泛滥污染召回）
        is_code = section_text.lstrip().startswith("```") or "代码实现" in title or "代码" in title
        chunks.append(Chunk(
            module=module, file=file_relpath, title=h1 or title,
            section=title, text=section_text,
            chunk_id=f"{file_relpath}#{title}",
            weight=0.5 if is_code else 1.0,
        ))
    return chunks


def _first_line(text: str) -> str:
    for line in text.splitlines():
        line = line.strip()
        if line:
            return line.lstrip("#").strip()
    return ""


# ------------------------------------------------------------------
# BM25 索引
# ------------------------------------------------------------------

class _BM25Index:
    """BM25 倒排索引（纯 Python 标准库实现）。"""

    def __init__(self, chunks: list[Chunk], k1: float = _BM25_K1, b: float = _BM25_B):
        self.chunks = chunks
        self.k1 = k1
        self.b = b
        self.N = len(chunks)
        self.avgdl = 0.0
        # df[token] = 出现该 token 的文档数
        self.df: Counter = Counter()
        # tf[doc_idx][token] = 词频
        self.tf: list[Counter] = []
        # dl[doc_idx] = 文档 token 总数
        self.dl: list[int] = []
        # 倒排表：token -> [doc_idx, ...]
        self.postings: dict[str, list[int]] = {}

        self._build()

    def _build(self) -> None:
        total_len = 0
        for idx, chunk in enumerate(self.chunks):
            # 标题/章节 token 出现频率按 3 倍权重重复（让标题里的关键词更显著）
            tokens = tokenize(chunk.text)
            tokens += tokenize(chunk.title) * 3
            tokens += tokenize(chunk.section) * 2
            tf = Counter(tokens)
            self.tf.append(tf)
            self.dl.append(sum(tf.values()))
            total_len += self.dl[-1]
            for tok in tf.keys():
                self.df[tok] += 1
                self.postings.setdefault(tok, []).append(idx)
        self.avgdl = (total_len / self.N) if self.N else 0.0

    def _idf(self, df_t: int) -> float:
        # Robertson-Sparck Jones IDF
        return math.log(((self.N - df_t + 0.5) / (df_t + 0.5)) + 1.0)

    def score(self, query_tokens: list[str], top_k: int = 5,
              module_filter: int | None = None) -> list[Hit]:
        if self.N == 0 or not query_tokens:
            return []
        scores: list[float] = [0.0] * self.N
        for tok in query_tokens:
            if tok not in self.postings:
                continue
            df_t = self.df[tok]
            idf = self._idf(df_t)
            for doc_idx in self.postings[tok]:
                if module_filter is not None and self.chunks[doc_idx].module != module_filter:
                    continue
                tf_td = self.tf[doc_idx][tok]
                dl = self.dl[doc_idx]
                num = tf_td * (self.k1 + 1)
                den = tf_td + self.k1 * (1 - self.b + self.b * dl / max(self.avgdl, 1e-6))
                scores[doc_idx] += idf * (num / den)
        # 应用 chunk 级权重（代码块章节降权 0.5，概览加权 1.2）
        ranked = sorted(
            ((s * self.chunks[i].weight, i) for i, s in enumerate(scores) if s > 0),
            key=lambda x: x[0], reverse=True,
        )
        return [Hit(chunk=self.chunks[i], score=s) for s, i in ranked[:top_k]]


# ------------------------------------------------------------------
# 知识库封装
# ------------------------------------------------------------------

class KnowledgeBase:
    """知识库：加载目录 → 切片 → 索引 → 检索。"""

    def __init__(self, chunks: list[Chunk], index: _BM25Index):
        self.chunks = chunks
        self.index = index

    @classmethod
    def from_directory(cls, knowledge_dir: str | Path) -> "KnowledgeBase":
        knowledge_dir = Path(knowledge_dir)
        if not knowledge_dir.exists():
            raise FileNotFoundError(f"知识库目录不存在: {knowledge_dir}")
        chunks: list[Chunk] = []
        for sub in sorted(knowledge_dir.iterdir()):
            if not sub.is_dir() or not sub.name.startswith("module"):
                continue
            try:
                module = int(sub.name.replace("module", ""))
            except ValueError:
                continue
            for md in sorted(sub.glob("*.md")):
                relpath = f"{sub.name}/{md.name}"
                try:
                    text = md.read_text(encoding="utf-8")
                except UnicodeDecodeError:
                    continue
                chunks.extend(split_markdown(text, relpath, module))
        index = _BM25Index(chunks)
        return cls(chunks, index)

    def search(self, query: str, top_k: int = 5,
               module_filter: int | None = None) -> list[Hit]:
        tokens = tokenize(query)
        return self.index.score(tokens, top_k=top_k, module_filter=module_filter)

    def stats(self) -> dict:
        return {
            "chunk_count": len(self.chunks),
            "module_distribution": dict(Counter(c.module for c in self.chunks)),
        }


# ------------------------------------------------------------------
# CLI
# ------------------------------------------------------------------

_REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_KB_DIR = _REPO_ROOT / "backend" / "src" / "main" / "resources" / "knowledge"


def _print_hits_text(hits: list[Hit], query: str) -> None:
    if not hits:
        print(f"(无召回) query={query!r}")
        return
    print(f"查询: {query}    召回 {len(hits)} 条")
    print("=" * 78)
    for i, h in enumerate(hits, 1):
        print(f"\n[{i}] score={h.score:.4f}  module={h.chunk.module}  "
              f"{h.chunk.chunk_id}")
        print(f"    标题: {h.chunk.title}  /  章节: {h.chunk.section or '(无)'}")
        snippet = h.chunk.text.strip().replace("\n", " ")
        if len(snippet) > 240:
            snippet = snippet[:240] + "…"
        print(f"    正文: {snippet}")


def main(argv: list[str] | None = None) -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass

    parser = argparse.ArgumentParser(description="知识库检索（BM25 + 中文 bigram）")
    parser.add_argument("query", nargs="?", default="",
                        help="查询字符串（--stats 时可省略）")
    parser.add_argument("--files", default=str(DEFAULT_KB_DIR),
                        help=f"知识库目录路径（默认: {DEFAULT_KB_DIR}）")
    parser.add_argument("--top-k", type=int, default=5, help="返回条数（默认 5）")
    parser.add_argument("--module", type=int, default=None,
                        help="限定模块编号 1-6（默认全部）")
    parser.add_argument("--json", action="store_true", help="以 JSON 输出")
    parser.add_argument("--stats", action="store_true", help="只打印知识库统计信息")
    args = parser.parse_args(argv)

    kb = KnowledgeBase.from_directory(args.files)
    if args.stats:
        print(json.dumps(kb.stats(), ensure_ascii=False, indent=2))
        return 0
    if not args.query:
        parser.error("query 不能为空（除非使用 --stats）")

    hits = kb.search(args.query, top_k=args.top_k, module_filter=args.module)
    if args.json:
        print(json.dumps([h.to_dict() for h in hits], ensure_ascii=False, indent=2))
    else:
        _print_hits_text(hits, args.query)
    return 0


if __name__ == "__main__":
    sys.exit(main())