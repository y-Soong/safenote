# -*- coding: utf-8 -*-
"""
30_embed_load — processed 청크 → TEI 임베딩(1024) → pgvector 적재(UPSERT, 멱등).

흐름:
  1) registry(license_checked=TRUE) 의 source 를 tb_ai_corpus_source 에 UPSERT (FK 선행)
  2) processed/{sid}_processed.json 로드 → CONTENT 배치 임베딩(TEI) → tb_ai_corpus_chunk UPSERT
     - EMBEDDING = 1024차원 vector
     - META_JSON = 청크 meta + track/data_reliability/source_name (DDL에 없는 소스속성을 jsonb로 보존)
     - ON CONFLICT(chunk_id) DO UPDATE → 재실행 멱등(결정②)

사용:
  python pipeline/30_embed_load.py --source 15053339           # 특정 자료
  python pipeline/30_embed_load.py --source 15053339 --limit 50 # 소량 테스트
  python pipeline/30_embed_load.py                             # 전체(대용량·시간 소요)

전제: pgvector(5432)·TEI(8090 Ready) 기동. 의존성: requests, psycopg2, openpyxl.
"""
import argparse
import glob
import importlib.util
import json
import os
import sys
import time

import requests
import psycopg2
from psycopg2.extras import execute_values

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PROCESSED_DIR = os.path.join(ROOT, "corpus", "processed")

_spec = importlib.util.spec_from_file_location("collect_api", os.path.join(HERE, "collect_api.py"))
_ca = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_ca)

TEI_URL = os.environ.get("TEI_URL", "http://localhost:8090/embed")
PG = dict(
    host=os.environ.get("PG_HOST", "localhost"),
    port=int(os.environ.get("PG_PORT", "5432")),
    dbname=os.environ.get("PG_DB", "prafta_ai"),
    user=os.environ.get("PG_USER", "prafta"),
    password=os.environ.get("PG_PW", "prafta1234"),
)
EMBED_BATCH = 8        # TEI 1회 요청당 문장 수 — ★2026-08-27: TEI 백엔드 로그에 "Backend does not
#   support a batch size > 8"(max_batch_requests=8 강제)가 찍혀 있는 걸 뒤늦게 발견. 16으로 보내면
#   서버가 내부적으로 쪼개 처리하며 큐가 계속 밀려 요청마다 점점 느려지다(43s→118s) 결국
#   클라이언트 타임아웃으로 매번 커밋 직전에 실패 — 15144147 적재 중 밤새 진행 0인 원인이었음.
DB_BATCH = 200         # DB 1회 UPSERT 행 수


def embed(texts, retries=3):
    body = json.dumps({"inputs": texts}).encode("utf-8")
    for attempt in range(1, retries + 1):
        try:
            r = requests.post(TEI_URL, data=body, headers={"Content-Type": "application/json"}, timeout=180)
            r.raise_for_status()
            return r.json()
        except Exception as e:                       # noqa: BLE001
            if attempt == retries:
                raise RuntimeError(f"TEI 임베딩 실패({retries}회): {e}")
            time.sleep(1.5 * attempt)


def vec_literal(v):
    return "[" + ",".join(f"{x:.8f}" for x in v) + "]"


def clip(s, n):
    """VARCHAR 컬럼 한도 방어 절단(초과 시 잘림). None 유지."""
    if s is None:
        return None
    s = str(s)
    return s if len(s) <= n else s[:n]


def upsert_source(cur, src):
    def d8(s):  # 'YYYY-MM-DD'/'YYYYMMDD' → 'YYYYMMDD'
        s = str(s or "").replace("-", "").strip()
        return s[:8] if len(s) >= 8 else "20260702"
    cur.execute(
        """
        INSERT INTO tb_ai_corpus_source
          (source_id, source_org, source_name, source_url, license_type,
           adopted_date, license_checked_date, source_update_cycle, feed_type,
           evidence_tier, use_yn)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'Y')
        ON CONFLICT (source_id) DO UPDATE SET
          source_org=EXCLUDED.source_org, source_name=EXCLUDED.source_name,
          source_url=EXCLUDED.source_url, license_type=EXCLUDED.license_type,
          adopted_date=EXCLUDED.adopted_date, license_checked_date=EXCLUDED.license_checked_date,
          source_update_cycle=EXCLUDED.source_update_cycle, feed_type=EXCLUDED.feed_type,
          evidence_tier=EXCLUDED.evidence_tier,
          use_yn='Y', update_date=CURRENT_TIMESTAMP
        """,
        (
            str(src["source_id"]).strip(),
            str(src.get("source_org") or "")[:100],
            str(src.get("source_name") or "")[:200],
            str(src.get("source_url") or "")[:500] or None,
            str(src.get("license_type") or "N/A")[:20],
            d8(src.get("license_checked_date")),
            d8(src.get("license_checked_date")),
            str(src.get("source_update_cycle") or "")[:20] or None,
            str(src.get("feed_type") or "FILE")[:10],
            # 근거 층위(prafta-062): registry evidence_tier 컬럼(사용자 수기 입력).
            #   미기입이면 NULL(배지 미표시). ★재적재 시 NULL 로 덮어쓰므로 xlsx 입력이 원본이다.
            str(src.get("evidence_tier") or "").strip()[:20] or None,
        ),
    )


CHUNK_SQL = """
INSERT INTO tb_ai_corpus_chunk
  (chunk_id, source_id, content, embedding, hazard_text, measure_text,
   domain_tag, cause_agent, meta_json, source_locator, use_yn)
VALUES %s
ON CONFLICT (chunk_id) DO UPDATE SET
  content=EXCLUDED.content, embedding=EXCLUDED.embedding,
  hazard_text=EXCLUDED.hazard_text, measure_text=EXCLUDED.measure_text,
  domain_tag=EXCLUDED.domain_tag, cause_agent=EXCLUDED.cause_agent,
  meta_json=EXCLUDED.meta_json, source_locator=EXCLUDED.source_locator,
  use_yn=EXCLUDED.use_yn, update_date=CURRENT_TIMESTAMP
"""
CHUNK_TMPL = "(%s,%s,%s,%s::vector,%s,%s,%s,%s,%s::jsonb,%s,%s)"


def load_source(conn, src, limit=None, force=False):
    sid = str(src["source_id"]).strip()
    path = os.path.join(PROCESSED_DIR, f"{sid}_processed.json")
    if not os.path.exists(path):
        print(f"  [건너뜀] {sid}: processed 없음(10_preprocess 먼저)")
        return 0
    chunks = json.load(open(path, encoding="utf-8"))
    if limit:
        chunks = chunks[:limit]
    cur = conn.cursor()
    upsert_source(cur, src)
    conn.commit()

    # resume: 이미 적재된 chunk_id 는 건너뛴다(재실행/중단복구 시 임베딩 낭비 방지)
    if not force:
        cur.execute("SELECT chunk_id FROM tb_ai_corpus_chunk WHERE source_id=%s", (sid,))
        existing = {r[0] for r in cur.fetchall()}
        before = len(chunks)
        chunks = [c for c in chunks if c["chunk_id"] not in existing]
        if before != len(chunks):
            print(f"  (resume) 기적재 {before - len(chunks)} 건너뜀, 잔여 {len(chunks)}")
    total = len(chunks)
    if total == 0:
        print(f"  [완료] {sid}: 신규 적재 0 (이미 최신)")
        cur.close()
        return 0

    done = 0
    rows = []
    for i in range(0, total, EMBED_BATCH):
        batch = chunks[i:i + EMBED_BATCH]
        vecs = embed([c["content"] for c in batch])
        for c, v in zip(batch, vecs):
            if len(v) != 1024:
                raise RuntimeError(f"{c['chunk_id']} 임베딩 차원 {len(v)}")
            meta = dict(c.get("meta_json") or {})
            meta["track"] = c.get("track")
            meta["data_reliability"] = c.get("data_reliability")
            meta["source_name"] = c.get("source_name")
            rows.append((
                clip(c["chunk_id"], 40), sid, c["content"], vec_literal(v),
                c.get("hazard_text"), c.get("measure_text"),
                clip(c.get("domain_tag"), 50), clip(c.get("cause_agent"), 100),
                json.dumps(meta, ensure_ascii=False), clip(c.get("source_locator"), 200), "Y",
            ))
        if len(rows) >= DB_BATCH:
            execute_values(cur, CHUNK_SQL, rows, template=CHUNK_TMPL)
            conn.commit()
            done += len(rows)
            rows = []
            print(f"    {sid}: {done}/{total} 적재", flush=True)
    if rows:
        execute_values(cur, CHUNK_SQL, rows, template=CHUNK_TMPL)
        conn.commit()
        done += len(rows)
    cur.close()
    print(f"  [완료] {sid}: {done}/{total} 청크 적재")
    return done


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", help="특정 source_id만")
    ap.add_argument("--limit", type=int, help="소스당 상위 N청크만(테스트)")
    ap.add_argument("--force", action="store_true", help="이미 적재된 청크도 재임베딩")
    args = ap.parse_args()

    reg = _ca.load_registry(_ca.REGISTRY_PATH)
    targets = [s for s in reg if _ca.is_checked(s.get("license_checked"))
               and (not args.source or str(s.get("source_id")) == str(args.source))]
    if not targets:
        sys.exit("[안내] 적재 대상(license_checked=TRUE) 없음.")

    # TEI 준비 확인
    try:
        h = requests.get(TEI_URL.replace("/embed", "/health"), timeout=10)
        if h.status_code != 200:
            sys.exit("[실패] TEI 미준비(/health != 200). 모델 로딩 완료 후 재시도.")
    except Exception as e:                           # noqa: BLE001
        sys.exit(f"[실패] TEI 연결 불가: {e}")

    conn = psycopg2.connect(**PG)
    print(f"== 적재 시작 · 대상 {len(targets)}건{' · limit '+str(args.limit) if args.limit else ''} ==")
    grand = 0
    for src in targets:
        print(f"\n[{src['source_id']}] {src.get('source_name')}")
        grand += load_source(conn, src, args.limit, args.force)
    conn.close()
    print(f"\n== 총 적재 청크: {grand} ==")


if __name__ == "__main__":
    main()
