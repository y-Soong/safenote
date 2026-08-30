# -*- coding: utf-8 -*-
"""
21_collect_koshaguide_pdf — 15144147(KOSHA GUIDE) 첨부 PDF 다운로더.

collect_api.py --source 15144147 이 이미 받아둔 메타데이터 스냅샷(corpus/raw/15144147_koshaguide_*.json,
techGdlnNo/techGdlnNm/techGdlnOfancYmd/fileDownloadUrl)을 읽어, 각 건의 실제 PDF를 내려받는다.
다운로드 호스트(portal.kosha.or.kr)는 API 게이트웨이(apis.data.go.kr)와 달라 일일 호출한도
개념이 다를 수 있다 — 그래도 20_collect_disaster_attach.py 와 동일하게 안전하게 설계한다:
매 건 직후 JSONL(corpus/raw/15144147_koshaguide_pdfs.jsonl)에 append+flush, 재실행 시 이미
받은 techGdlnNo 는 건너뛴다(중복 다운로드 방지).

사용:
  python pipeline/21_collect_koshaguide_pdf.py --limit 20   # 소량 테스트
  python pipeline/21_collect_koshaguide_pdf.py              # 전량(1,039건)
"""
import argparse
import glob
import json
import os
import sys
import time
from datetime import datetime

import requests

for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW_DIR = os.path.join(ROOT, "corpus", "raw")
PDF_DIR = os.path.join(RAW_DIR, "15144147_pdfs")
SOURCE_ID = "15144147"
MANIFEST_PATH = os.path.join(RAW_DIR, f"{SOURCE_ID}_koshaguide_pdfs.jsonl")


def load_metadata():
    hits = sorted(glob.glob(os.path.join(RAW_DIR, f"{SOURCE_ID}_koshaguide_*.json")))
    if not hits:
        sys.exit(f"[실패] {SOURCE_ID} 메타데이터 스냅샷 없음. "
                  f"collect_api.py --source {SOURCE_ID} 먼저 실행.")
    snap = json.load(open(hits[-1], encoding="utf-8"))
    return snap.get("items", [])


def load_done():
    done = set()
    if os.path.exists(MANIFEST_PATH):
        with open(MANIFEST_PATH, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    done.add(json.loads(line)["techGdlnNo"])
                except Exception:                       # noqa: BLE001
                    continue
    return done


def download_pdf(url, dest_path, retries=3):
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
        return True
    last = None
    for attempt in range(1, retries + 1):
        try:
            r = requests.get(url, timeout=30)
            r.raise_for_status()
            os.makedirs(os.path.dirname(dest_path), exist_ok=True)
            with open(dest_path, "wb") as f:
                f.write(r.content)
            return True
        except Exception as e:                        # noqa: BLE001
            last = e
            if attempt < retries:
                time.sleep(1.5 * attempt)
    print(f"  [경고] 다운로드 실패 {dest_path}: {last}")
    return False


def main():
    ap = argparse.ArgumentParser(description="15144147 KOSHA GUIDE PDF 다운로더")
    ap.add_argument("--limit", type=int, default=0, help="이번 실행 신규 처리 건수 제한(0=전량)")
    ap.add_argument("--sleep", type=float, default=0.2, help="요청 간 대기(초, 서버 배려)")
    args = ap.parse_args()

    items = load_metadata()
    done = load_done()
    pending = [it for it in items if it.get("techGdlnNo") and it["techGdlnNo"] not in done]
    print(f"== {SOURCE_ID} PDF 수집 · 전체 {len(items):,}건 · 이미 완료 {len(done):,}건 · "
          f"남음 {len(pending):,}건 ==")
    if args.limit:
        pending = pending[: args.limit]

    os.makedirs(RAW_DIR, exist_ok=True)
    ok, fail = 0, 0
    manifest_f = open(MANIFEST_PATH, "a", encoding="utf-8")
    try:
        for i, it in enumerate(pending, 1):
            no = it.get("techGdlnNo", "")
            url = it.get("fileDownloadUrl", "")
            if not url:
                fail += 1
                continue
            safe_no = no.replace("/", "_")
            local_path = os.path.join(PDF_DIR, f"{safe_no}.pdf")
            if download_pdf(url, local_path):
                ok += 1
                manifest_f.write(json.dumps({
                    "techGdlnNo": no,
                    "techGdlnNm": it.get("techGdlnNm"),
                    "techGdlnOfancYmd": it.get("techGdlnOfancYmd"),
                    "fileDownloadUrl": url,
                    "local_path": local_path,
                }, ensure_ascii=False) + "\n")
                manifest_f.flush()
            else:
                fail += 1
            if i % 100 == 0:
                print(f"  진행 {i:,}/{len(pending):,} (성공 {ok:,} - 실패 {fail:,})", flush=True)
            time.sleep(args.sleep)
    finally:
        manifest_f.close()

    remaining = len(items) - len(load_done())
    print(f"[요약] 이번 실행 성공 {ok:,} - 실패 {fail:,} - 전체 잔여 {remaining:,}건 -> {MANIFEST_PATH}")


if __name__ == "__main__":
    main()
