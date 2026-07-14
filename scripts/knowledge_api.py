# -*- coding: utf-8 -*-
"""
知识库检索 HTTP 服务
====================

把 ``knowledge_search.py`` 的检索能力以 REST 方式暴露，Java 后端通过 HTTP
调用即可把"按名称 LIKE 命中单文档"升级为"BM25 Top-K 召回"。

零外部依赖：仅 Python 3.8+ 标准库（http.server + urllib.parse）。

启动方式
--------

::

    python scripts/knowledge_api.py --port 8765
    # 然后 Java 端 GET http://localhost:8765/search?q=过拟合&top_k=5
    # 或 POST http://localhost:8765/search   body: {"q":"过拟合","top_k":5,"module":2}

端点
----

- ``GET  /health``           → ``{"status":"ok","chunks":N}``
- ``GET  /search?q=&top_k=&module=``  → 200 JSON / 400 缺 q
- ``POST /search`` body ``{"q":"...","top_k":5,"module":2}`` → 同 GET
- ``GET  /chunk/{chunk_id}`` → 返回单 chunk 全文（用于 Java 端按需取正文）
- 其他路径 → 404

Java 端调用示例（伪代码）
------------------------

::

    RestTemplate rt = new RestTemplate();
    String url = "http://localhost:8765/search?q={q}&top_k={k}";
    SearchResponse resp = rt.getForObject(url, SearchResponse.class, "过拟合", 5);
    StringBuilder ctx = new StringBuilder();
    for (Hit h : resp.hits) {
        ctx.append("[").append(h.title).append(" - ").append(h.section).append("]\n");
        ctx.append(h.text).append("\n\n");
    }
    // 把 ctx 注入 Prompt，替换原 KnowledgeBaseServiceImpl.loadMarkdownByName 的 1500 字截断
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# 让 ``from knowledge_search import ...`` 在 ``scripts/`` 目录下也能工作
sys.path.insert(0, str(Path(__file__).resolve().parent))
from knowledge_search import KnowledgeBase, DEFAULT_KB_DIR  # noqa: E402


# ------------------------------------------------------------------
# 共享的 KnowledgeBase 实例（懒加载 + 全局缓存）
# ------------------------------------------------------------------

_KB: KnowledgeBase | None = None


def get_kb(knowledge_dir: str) -> KnowledgeBase:
    global _KB
    if _KB is None:
        _KB = KnowledgeBase.from_directory(knowledge_dir)
    return _KB


# ------------------------------------------------------------------
# HTTP Handler
# ------------------------------------------------------------------

class _Handler(BaseHTTPRequestHandler):
    knowledge_dir: str = str(DEFAULT_KB_DIR)  # 由 main() 注入

    # 静音默认 access log（按需可改回 self.log_message）
    def log_message(self, fmt: str, *args) -> None:  # noqa: N802
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))

    # ---- 工具方法 ----
    def _send_json(self, status: int, payload: dict | list) -> None:
        try:
            sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
        except Exception:
            pass
        body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _send_text(self, status: int, text: str, content_type: str = "text/plain; charset=utf-8") -> None:
        body = text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    # ---- 路由分发 ----
    def do_GET(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        qs = urllib.parse.parse_qs(parsed.query)

        if path == "/health":
            kb = get_kb(self.knowledge_dir)
            self._send_json(200, {"status": "ok", "chunks": len(kb.chunks)})
            return

        if path == "/search":
            self._handle_search(qs.get("q", [None])[0],
                                qs.get("top_k", ["5"])[0],
                                qs.get("module", [None])[0])
            return

        if path.startswith("/chunk/"):
            chunk_id = urllib.parse.unquote(path[len("/chunk/"):])
            self._handle_get_chunk(chunk_id)
            return

        self._send_json(404, {"error": "not_found", "path": path})

    def do_POST(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path != "/search":
            self._send_json(404, {"error": "not_found", "path": parsed.path})
            return
        length = int(self.headers.get("Content-Length", "0"))
        try:
            body = json.loads(self.rfile.read(length).decode("utf-8")) if length else {}
        except json.JSONDecodeError as e:
            self._send_json(400, {"error": "invalid_json", "detail": str(e)})
            return
        self._handle_search(body.get("q"),
                            str(body.get("top_k", 5)),
                            body.get("module"))

    def do_OPTIONS(self) -> None:  # noqa: N802
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    # ---- 业务逻辑 ----
    def _handle_search(self, q, top_k_raw, module_raw) -> None:
        if not q or not isinstance(q, str) or not q.strip():
            self._send_json(400, {"error": "missing_query",
                                  "hint": "请提供参数 q（非空字符串）"})
            return
        try:
            top_k = int(top_k_raw) if top_k_raw is not None else 5
        except (TypeError, ValueError):
            self._send_json(400, {"error": "invalid_top_k", "value": top_k_raw})
            return
        top_k = max(1, min(top_k, 50))
        module_filter = None
        if module_raw is not None and str(module_raw).strip():
            try:
                module_filter = int(module_raw)
            except (TypeError, ValueError):
                self._send_json(400, {"error": "invalid_module", "value": module_raw})
                return

        kb = get_kb(self.knowledge_dir)
        hits = kb.search(q, top_k=top_k, module_filter=module_filter)
        self._send_json(200, {
            "query": q,
            "top_k": top_k,
            "module": module_filter,
            "hit_count": len(hits),
            "hits": [h.to_dict() for h in hits],
        })

    def _handle_get_chunk(self, chunk_id: str) -> None:
        kb = get_kb(self.knowledge_dir)
        for c in kb.chunks:
            if c.chunk_id == chunk_id:
                self._send_json(200, c.to_dict())
                return
        self._send_json(404, {"error": "chunk_not_found", "chunk_id": chunk_id})


# ------------------------------------------------------------------
# 入口
# ------------------------------------------------------------------

def main(argv: list[str] | None = None) -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
        sys.stderr.reconfigure(encoding="utf-8")  # type: ignore[attr-defined]
    except Exception:
        pass

    parser = argparse.ArgumentParser(description="知识库检索 HTTP 服务（标准库，零依赖）")
    parser.add_argument("--port", type=int, default=8765, help="监听端口（默认 8765）")
    parser.add_argument("--host", default="127.0.0.1", help="监听地址（默认 127.0.0.1）")
    parser.add_argument("--files", default=str(DEFAULT_KB_DIR),
                        help=f"知识库目录（默认: {DEFAULT_KB_DIR}）")
    args = parser.parse_args(argv)

    _Handler.knowledge_dir = args.files
    # 启动时立即建索引，让 /health 第一次响应就快
    print(f"[knowledge_api] 加载知识库: {args.files}", file=sys.stderr)
    get_kb(args.files)
    print(f"[knowledge_api] 索引就绪，准备监听 http://{args.host}:{args.port}", file=sys.stderr)

    server = ThreadingHTTPServer((args.host, args.port), _Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[knowledge_api] 收到 Ctrl+C，退出。", file=sys.stderr)
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())