# scripts/ — 知识库工具集

> 配套 [`backend/`](../backend/) 的 Python 工具，让"知识库真的能用"。

本目录三个脚本协同工作，解决 [`backend/` KnowledgeBaseServiceImpl](../backend/) 的两个遗留问题：

1. **顶层 `knowledge/` 与内部 `resources/knowledge/` 内容不一致**（两份独立维护，漂移）。
2. **检索方式是"按名称 LIKE + 截断 1500 字"**——既无真正召回，也无法处理超长文档。

---

## 工具一览

| 脚本 | 作用 | 调用方式 |
|---|---|---|
| `sync-knowledge.py` | 把顶层 `knowledge/` 中文原稿同步到内部 `resources/knowledge/`，保留文件名（不破坏 seed-knowledge.sql） | CLI：干跑或 `--apply` |
| `knowledge_search.py` | 加载知识库 → 按 H2 切片 → BM25 索引 → 中文 bigram 分词检索 | CLI / Python 模块 |
| `knowledge_api.py` | 把 `knowledge_search` 包成 HTTP 服务，Java 后端通过 HTTP 调它 | CLI：起一个零依赖 HTTP 服务 |

**零外部依赖**：三个脚本都只使用 Python 3.8+ 标准库。后端环境即使精简 Docker 镜像也能跑。

---

## 1. `sync-knowledge.py` — 同步顶层原稿到后端 classpath

### 为什么需要

代码读的是 [`backend/src/main/resources/knowledge/`](../backend/src/main/resources/knowledge/)（被 `KnowledgeBaseServiceImpl.loadMarkdown` 用 `ClassPathResource` 加载）；顶层还有个 [`knowledge/`](../knowledge/) 是中文版原稿。两份内容会漂移。

### 同步策略

- **方向**：顶层 → 内部（顶层是原稿，内部是精简翻译版）。
- **默认行为**：**干跑**，打印差异清单，不动文件。
- **`--apply` 才真改**：把顶层原稿以 `<!-- top-level source -->` 分隔块**追加**到内部文件末尾（保留内部精简版），拆篇则把整篇原稿注入头部供人工拆。
- **`--backup`**：修改前生成 `.bak`。
- **保留 `seed-knowledge.sql` 里的 `content_path` 字段**：英文文件名不变，数据库 INSERT 不需要改。

### 用法

```bash
# 干跑，看差异清单
python scripts/sync-knowledge.py

# 写入报告文件
python scripts/sync-knowledge.py --report scripts/sync-report.txt

# 真正同步 + 备份
python scripts/sync-knowledge.py --apply --backup

# 加 diff 细节
python scripts/sync-knowledge.py --diff
```

### 干跑结果（节选）

```
总计 31 行；按状态分组：
  - split: 1        # 拆篇：偏差方差与过拟合欠拟合.md → bias-variance.md + overfitting.md
  - mapped: 27      # 一一对应
  - extra-internal: 3  # supervised/unsupervised-learning.md、overfitting.md（顶层原稿混在别的章节里）

映射对：27；顶层更详 15 / 内部更详 0 / 一致 12
```

完整报告：[`scripts/sync-report.txt`](sync-report.txt)

---

## 2. `knowledge_search.py` — BM25 + 中文 bigram 检索

### 为什么不用 Embedding / 向量库

- 32 篇入门 Markdown，体量小，BM25 已足够精准。
- Embedding 需要调外部 API 或下载数百 MB 模型，拖慢镜像构建。
- 检索入口 `KnowledgeBase.search()` 抽象化，后续要换向量版只换实现，不动调用方。

### 切片策略

按 Markdown 二级标题 `##` 切片：

```python
Chunk(
    module=2,
    file="module2/decision-tree.md",
    title="决策树",
    section="信息增益",
    text="...",
    chunk_id="module2/decision-tree.md#信息增益",
    weight=1.0,    # 代码块章节降权 0.5，概览加权 1.2
)
```

一个文档对应 N 个 chunk；召回时返回 Top-K。

### 用法

```bash
# 1. 查看统计
python scripts/knowledge_search.py --stats
# {"chunk_count": 352, "module_distribution": {"1": 74, "2": 96, ...}}

# 2. 命令行查询
python scripts/knowledge_search.py "什么是过拟合"
python scripts/knowledge_search.py "梯度下降" --top-k 3
python scripts/knowledge_search.py "PCA 主成分" --module 3
python scripts/knowledge_search.py "PyTorch 训练流程" --json

# 3. 作为库
from scripts.knowledge_search import KnowledgeBase
kb = KnowledgeBase.from_directory("backend/src/main/resources/knowledge/")
hits = kb.search("过拟合", top_k=5)
for h in hits:
    print(h.score, h.chunk.chunk_id)
```

### 检索效果（实测）

| 查询 | Top-1 命中 | 备注 |
|---|---|---|
| "什么是过拟合" | `bias-variance.md#适用场景` + `overfitting.md#常见易错点` | 拆篇产物都对得上 |
| "梯度下降" | `linear-regression.md#常见易错点` + `logistic-regression.md#常见易错点` | 提到梯度下降的易错点章节 |
| "CNN 卷积层" | `cnn.md#常见易错点` + `cnn.md#练习题` | 直击模块 4 |
| "PCA 主成分" | `pca.md#适用场景`（加权后排第 1） | 代码块降权生效，原理章节排前 |
| "房价预测怎么做" | `house-price.md#数据集` + `house-price.md#学完应该掌握` | 项目实战 |

---

## 3. `knowledge_api.py` — HTTP 服务（Java 后端调用入口）

### 启动

```bash
python scripts/knowledge_api.py --port 8765
# [knowledge_api] 加载知识库: backend\src\main\resources\knowledge
# [knowledge_api] 索引就绪，准备监听 http://127.0.0.1:8765
```

### 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/health` | `{"status":"ok","chunks":352}` |
| `GET` | `/search?q=...&top_k=5&module=2` | GET 查询（top_k 默认 5，module 可选） |
| `POST` | `/search` body `{"q":"...","top_k":5,"module":2}` | POST 查询 |
| `GET` | `/chunk/{chunk_id}` | 取单个 chunk 全文（chunk_id 需 URL-encode `#`） |

### Java 端如何用

替换原 [`ResourceServiceImpl.buildKnowledgeContext`](../backend/) 里"按名称 LIKE + 截断 1500 字"那段，改成：

```java
String url = "http://localhost:8765/search?q=" + URLEncoder.encode(query, "UTF-8") + "&top_k=5";
SearchResponse resp = new RestTemplate().getForObject(url, SearchResponse.class);
StringBuilder ctx = new StringBuilder();
for (Hit h : resp.hits) {
    ctx.append("[").append(h.title).append(" - ").append(h.section).append("]\n");
    ctx.append(h.text).append("\n\n");
}
// 把 ctx 注入 Prompt
```

### 测试

```bash
curl "http://127.0.0.1:8765/health"
curl "http://127.0.0.1:8765/search?q=CNN%E5%8D%B7%E7%A7%AF%E5%B1%82&top_k=2"
curl -X POST -H "Content-Type: application/json" \
     -d '{"q":"RNN 遗忘门","top_k":3,"module":4}' \
     "http://127.0.0.1:8765/search"
```

---

## 与 Java 后端的协同建议

当前 [`backend/`](../backend/) 的 `KnowledgeBaseServiceImpl.loadMarkdownByName` 是"按名称 LIKE 单文档 → 截断 1500 字"，本质是**名字匹配 + 截断**，不是检索。

把它升级成真正的"知识库检索"有两个层次：

### 最低成本：直接用 HTTP API（不改后端结构）

1. `docker-compose.yml` 加一个 `knowledge-search` 服务跑 `knowledge_api.py`。
2. Java 端在 `ResourceServiceImpl.buildKnowledgeContext` 里多调一次 `http://knowledge-search:8765/search?q=...`，把返回的 Top-K 拼接进 `knowledge_preview`。

### 进阶：把 BM25 内嵌进 JVM

如果不想引入额外服务，可把 `knowledge_search.py` 移植成 Java（Lucene Core / Lucene Analysis 都能直接做 BM25 + ChineseAnalyzer），逻辑一一对应。**优先级比"上向量库"高**——32 篇入门文档上向量反而过度设计。

---

## 文件清单

```
scripts/
├── README.md                # 本文件
├── sync-knowledge.py        # 顶层 → 内部同步
├── sync-report.txt          # 同步报告（干跑生成）
├── knowledge_search.py      # BM25 检索核心（库 + CLI）
└── knowledge_api.py         # HTTP 封装（标准库，零依赖）
```