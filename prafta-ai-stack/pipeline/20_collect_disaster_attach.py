# -*- coding: utf-8 -*-
"""
20_collect_disaster_attach — 15121008(국내재해사례 첨부파일) 수집기.

15121001(국내재해사례 게시판, 이미 수집·적재됨)의 boardno 를 순회하며
disaster_attach_api02 로 첨부 PDF 경로(filepath)를 조회하고, PDF 실물을 내려받는다.
독립 소스가 아니라 15121001 raw 스냅샷에 boardno 로 연계된 자료라 collect_api.py(레지스트리
구동 단일소스 페이지네이션 수집)의 범용 로직으로는 처리할 수 없어 전용 스크립트로 분리한다.

엔드포인트 확정(2026-08-26 실측): registry api_endpoint 표기(`.../disaster_attach_api02`)는
500 오류가 난다 — 실제로는 대문자 서브패스 `.../disaster_attach_api02/Disaster_attach_api02`
+ 고정 파라미터 callApiId=1070 이 필요하다(15121001 의 callApiId=1060 패턴과 동일 계열).

★일일 호출한도 1,000건(2026-08-26 실측, 응답헤더 X-RateLimit-Limit=1000) — data.go.kr 이
자정(KST) 기준 리셋하는 것으로 보인다. 대상 3,543건 전체를 받으려면 여러 날에 걸쳐 나눠 돌려야
한다. 그래서 이 스크립트는 매 건 처리 직후 JSONL(corpus/raw/15121008_kosha_disaster_attach.jsonl)
에 한 줄씩 append+flush 한다 — 중간에 한도 소진으로 멈추거나 죽어도 그때까지 받은 건 보존되고,
재실행 시 이미 JSONL 에 있는 boardno 는 API 호출 없이 건너뛴다(한도 낭비 방지).
한도 소진(HTTP 429 / returnReasonCode=22)을 감지하면 그 즉시 재시도 없이 정상 종료한다
(재시도해도 안 풀리는 걸 알면서 계속 두드리는 건 남은 한도만 태움 — 실제로 겪은 실패 패턴).

전제:
  - corpus/raw/15121001_*.json 이미 존재(collect_api.py --source 15121001 로 먼저 수집됨)
  - corpus/.env 에 DATA_GO_KR_SERVICE_KEY

사용:
  python pipeline/20_collect_disaster_attach.py --limit 5   # 소량 테스트(엔드투엔드 검증용)
  python pipeline/20_collect_disaster_attach.py             # 한도가 허용하는 만큼 수집(자동 이어받기)
"""
import argparse
import glob
import json
import os
import sys
import time
from datetime import datetime

import requests

# Windows 콘솔 cp949 크래시 방어(★반복된 함정): 기본 stdout/stderr 인코딩이 cp949라
# em dash(—) 등 일부 유니코드 문자를 print 하면 UnicodeEncodeError 로 스크립트 전체가 죽는다
# (실제로 700여 건 처리 후 이 문제로 한 번 중단됨). 로그가 깨져 보여도 죽지는 않게 UTF-8+replace 로 강제.
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ENV_PATH = os.path.join(ROOT, "corpus", ".env")
RAW_DIR = os.path.join(ROOT, "corpus", "raw")
PDF_DIR = os.path.join(RAW_DIR, "15121008_pdfs")

ENDPOINT = "https://apis.data.go.kr/B552468/disaster_attach_api02/Disaster_attach_api02"
CALL_API_ID = "1070"
LINKED_SOURCE_ID = "15121001"
TARGET_SOURCE_ID = "15121008"
OK_RESULT_CODES = {"0", "00"}
MANIFEST_PATH = os.path.join(RAW_DIR, f"{TARGET_SOURCE_ID}_kosha_disaster_attach.jsonl")

QUOTA_EXCEEDED = "QUOTA_EXCEEDED"   # fetch_attach_list 가 이 문자열을 code 로 반환하면 즉시 중단


class QuotaExceeded(Exception):
    pass


def load_env(path):
    """corpus/.env 파싱(KEY=VALUE). python-dotenv 의존 없이(collect_api.py 와 동일 방식)."""
    if not os.path.exists(path):
        sys.exit(f"[실패] .env 없음: {path}")
    env = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    return env


def load_linked_records():
    """15121001 최신 raw 스냅샷에서 첨부보유(atcflcnt>=1) 레코드만 추출."""
    hits = sorted(glob.glob(os.path.join(RAW_DIR, f"{LINKED_SOURCE_ID}_*.json")))
    if not hits:
        sys.exit(f"[실패] {LINKED_SOURCE_ID} 원본 스냅샷 없음. "
                  f"collect_api.py --source {LINKED_SOURCE_ID} 먼저 실행.")
    snap = json.load(open(hits[-1], encoding="utf-8"))
    items = snap.get("items", [])
    return [it for it in items if int(it.get("atcflcnt") or 0) > 0]


def load_done_boardnos():
    """이미 JSONL 에 기록된 boardno 집합(재실행 시 API 재호출 방지 — 한도 절약)."""
    done = set()
    if os.path.exists(MANIFEST_PATH):
        with open(MANIFEST_PATH, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    done.add(json.loads(line)["boardno"])
                except Exception:                       # noqa: BLE001 (깨진 줄은 무시하고 계속)
                    continue
    return done


def fetch_attach_list(boardno, service_key, retries=3):
    """boardno 1건의 첨부 목록(filenm/filepath) 조회. 한도 소진이면 QuotaExceeded 를 던진다."""
    params = {
        "ServiceKey": service_key, "type": "json", "callApiId": CALL_API_ID,
        "boardno": boardno, "pageNo": 1, "numOfRows": 20,
    }
    last = None
    for attempt in range(1, retries + 1):
        try:
            r = requests.get(ENDPOINT, params=params, timeout=20)
            if r.status_code == 429:
                raise QuotaExceeded(r.headers.get("X-RateLimit-Limit", "?"))
            r.raise_for_status()
            js = r.json()
            header = js.get("header", {}) or {}
            body = js.get("body", {}) or {}
            code = str(header.get("resultCode", "")).strip()
            if code == "22" or "LIMITED_NUMBER_OF_SERVICE_REQUESTS" in str(
                    (js.get("OpenAPI_ServiceResponse", {}) or {})
                    .get("cmmMsgHeader", {}).get("errMsg", "")):
                raise QuotaExceeded("resultCode=22")
            if code not in OK_RESULT_CODES:
                return None, code
            items_wrap = body.get("items", "")
            item = items_wrap.get("item", "") if isinstance(items_wrap, dict) else ""
            if isinstance(item, dict):
                item = [item]
            elif not isinstance(item, list):
                item = []
            return item, code
        except QuotaExceeded:
            raise
        except Exception as e:                        # noqa: BLE001 (수집 견고성 우선)
            last = e
            if attempt < retries:
                time.sleep(1.5 * attempt)
    print(f"  [경고] {boardno}: 조회 실패 - {last}")
    return None, "ERR"


def download_pdf(url, dest_path, retries=3):
    """이미 받은 파일(존재+비어있지 않음)이면 스킵(멱등). 그 외 다운로드."""
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
    ap = argparse.ArgumentParser(description="15121008 첨부 PDF 수집기(15121001 boardno 연계)")
    ap.add_argument("--limit", type=int, default=0, help="이번 실행에서 새로 처리할 건수 제한(0=한도까지)")
    ap.add_argument("--sleep", type=float, default=0.15, help="요청 간 대기(초, 서버 배려)")
    args = ap.parse_args()

    env = load_env(ENV_PATH)
    service_key = env.get("DATA_GO_KR_SERVICE_KEY", "")
    if not service_key:
        sys.exit("[실패] DATA_GO_KR_SERVICE_KEY 미설정. corpus/.env 확인.")

    records = load_linked_records()
    done = load_done_boardnos()
    pending = [r for r in records if r.get("boardno") and r["boardno"] not in done]
    print(f"== {TARGET_SOURCE_ID} 첨부 수집 · 전체 {len(records):,}건 · "
          f"이미 완료 {len(done):,}건 · 남음 {len(pending):,}건 ==")
    if args.limit:
        pending = pending[: args.limit]

    os.makedirs(RAW_DIR, exist_ok=True)
    ok, fail, quota_stopped = 0, 0, False
    manifest_f = open(MANIFEST_PATH, "a", encoding="utf-8")
    try:
        for i, rec in enumerate(pending, 1):
            boardno = rec["boardno"]
            try:
                attaches, code = fetch_attach_list(boardno, service_key)
            except QuotaExceeded as qe:
                print(f"[중단] 일일 호출한도 소진(limit={qe}) — 여기까지 {ok:,}건 저장됨. "
                      f"한도 리셋 후(자정 KST 추정) 재실행하면 이어서 진행됩니다.")
                quota_stopped = True
                break
            if attaches is None:
                fail += 1
                time.sleep(args.sleep)
                continue
            got_any = False
            for j, a in enumerate(attaches):
                filenm = str(a.get("filenm") or "")
                filepath = str(a.get("filepath") or "")
                if not filepath:
                    continue
                ext = os.path.splitext(filenm)[1] or ".pdf"
                local_path = os.path.join(PDF_DIR, f"{boardno}_{j}{ext}")
                if download_pdf(filepath, local_path):
                    ok += 1
                    got_any = True
                    manifest_f.write(json.dumps({
                        "boardno": boardno,
                        "filenm": filenm,
                        "filepath": filepath,
                        "local_path": local_path,
                        "business": rec.get("business"),
                        "keyword": rec.get("keyword"),
                    }, ensure_ascii=False) + "\n")
                else:
                    fail += 1
            if not got_any and attaches:
                fail += 1
            elif not attaches:
                # 첨부 0건 응답(atcflcnt 는 1이었지만 실제 첨부가 비어있는 예외 케이스) — 다시 안 묻도록
                # boardno 만 기록(파일 없이) 해 재실행 시 재호출을 막는다.
                manifest_f.write(json.dumps({"boardno": boardno, "filenm": None,
                                              "filepath": None, "local_path": None,
                                              "business": rec.get("business"),
                                              "keyword": rec.get("keyword")}, ensure_ascii=False) + "\n")
            manifest_f.flush()
            if i % 100 == 0:
                print(f"  진행 {i:,}/{len(pending):,} (성공 {ok:,} - 실패 {fail:,})", flush=True)
            time.sleep(args.sleep)
    finally:
        manifest_f.close()

    remaining = len(records) - len(load_done_boardnos())
    print(f"[요약] 이번 실행 성공 {ok:,} - 실패 {fail:,} - 전체 잔여 {remaining:,}건 "
          f"({'한도 소진으로 중단' if quota_stopped else '완료'}) -> {MANIFEST_PATH}")


if __name__ == "__main__":
    main()
