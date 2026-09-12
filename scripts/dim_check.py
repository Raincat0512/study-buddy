# -*- coding: utf-8 -*-
# 临时诊断脚本：查 embedding-3 实际返回的维度数（用完可删）
# 用法：先确保环境变量 EMBEDDING_API_KEY 已设置，然后 python dim_check.py
import os, sys, json, urllib.request, urllib.error

key = os.environ.get("EMBEDDING_API_KEY")
if not key and len(sys.argv) > 1:
    key = sys.argv[1]
if not key:
    print("未找到 key：请先设置环境变量 EMBEDDING_API_KEY，或把 key 作为第一个参数传入")
    sys.exit(1)

body = json.dumps({"model": "embedding-3", "input": "测试"}).encode("utf-8")
req = urllib.request.Request(
    "https://open.bigmodel.cn/api/paas/v4/embeddings",
    data=body,
    headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
    method="POST",
)
try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    vec = data["data"][0]["embedding"]
    print("返回维度数 =", len(vec))
except urllib.error.HTTPError as e:
    print("HTTP 错误", e.code, e.read().decode("utf-8", errors="replace"))
except Exception as e:
    print("请求失败:", repr(e))
