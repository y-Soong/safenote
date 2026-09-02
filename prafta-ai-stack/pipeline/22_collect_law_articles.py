# -*- coding: utf-8 -*-
"""
22_collect_law_articles — 법령 조문 XML 수집기 (prafta-062 [배포 B] S2).

법령 6종(산업안전보건법·시행령·시행규칙, 산업안전보건기준에 관한 규칙,
중대재해처벌법·시행령)의 조문 본문 XML 을 law.go.kr DRF 로 수집해
corpus/raw/ 에 "무변경 보관"한다(가공 금지 — 청킹은 10_preprocess 단계).

2단 구조(요청서 §법령 조달 경로 — 하나로는 안 된다):
  - 본문      : https://www.law.go.kr/DRF/lawService.do?OC=<OC>&target=law&MST=<MST>&type=XML
                ★open.law.go.kr/LSO/DRF/ 는 404. www.law.go.kr/DRF/ 가 정답.
  - 목록·개정감지: https://apis.data.go.kr/1170000/law/lawSearchList.do (data.go.kr 15000115,
                기존 DATA_GO_KR_SERVICE_KEY 재사용. 조문 본문은 안 나온다 — 목록·메타 전용)

인증(★시크릿 규율 — security 리뷰 축):
  - OC 는 corpus/.env 의 LAW_GO_KR_OC 로만 읽는다(.env 는 .gitignore 로 커밋 차단).
  - ★OC 원문을 어떤 print/로그/에러 메시지/스냅샷 JSON 에도 남기지 않는다.
    requests 예외 문자열에는 쿼리스트링(OC 포함)이 통째로 실려 오므로,
    모든 출력 직전에 _scrub() 으로 OC 값을 마스킹한다.

★실패 진단 (실제 소모 사례 — 요청서 §함정):
  DRF 의 "사용자 정보 검증에 실패 — 서버장비의 IP주소 및 도메인주소를 등록해 주세요" 에러는
  ①잘못된 OC 와 ②미등록 IP 를 **구분하지 않는다**(가짜 OC 로도 같은 문구가 나온다).
  이 에러를 만나면 OC 를 의심하기 전에 **호출 PC 의 공인 IP 가 open.law.go.kr 마이페이지
  (API인증키관리)의 등록 IP 와 일치하는지부터 대조**할 것. 등록 IP = 121.161.241.60 (개발 PC).

MST 는 registry(sources.xlsx)의 api_fixed_params(예: "target=law&MST=283449")에 보관한다
— MST 는 법령 개정마다 바뀌는 값이라 source_id 에 넣지 않는다(plan §1 062-04).
개정 감지로 새 MST 를 확인하면 registry 의 api_fixed_params 만 갱신하면 된다(코드 무수정).

사용:
  python pipeline/22_collect_law_articles.py                    # LAW_* 전 소스 본문 수집
  python pipeline/22_collect_law_articles.py --source LAW_OSHA  # 특정 법령만
  python pipeline/22_collect_law_articles.py --check-revision   # 목록 API 개정 감지만(본문 수집 안 함)

의존성: requests, openpyxl (collect_api.py 와 동일)
"""
import argparse
import glob
import importlib.util
import json
import os
import re
import sys
import time
import urllib.parse
import xml.etree.ElementTree as ET
from datetime import datetime, date

import requests

# Windows 콘솔 cp949 크래시 방어(파이프라인 공통 관례)
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW_DIR = os.path.join(ROOT, "corpus", "raw")

# collect_api 의 .env/registry 로더 재사용(중복 방지 — 00_validate_registry 와 동일 패턴)
_spec = importlib.util.spec_from_file_location("collect_api", os.path.join(HERE, "collect_api.py"))
_ca = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_ca)

LAW_SERVICE_URL = "https://www.law.go.kr/DRF/lawService.do"
LAW_SEARCH_URL = "https://apis.data.go.kr/1170000/law/lawSearchList.do"
LAW_SOURCE_PREFIX = "LAW_"

# 모듈 전역: _scrub() 이 마스킹할 시크릿 값 목록(OC·serviceKey). main() 에서 채운다.
_SECRETS = []


def _scrub(text):
    """출력 직전 시크릿 마스킹 — 예외 문자열/응답 미리보기 등 어디에든 OC 가 섞일 수 있어
    모든 print 경로가 이 함수를 통과한다(★OC 원문 노출 금지)."""
    s = str(text)
    for sec in _SECRETS:
        if sec:
            s = s.replace(sec, "<masked>")
    return s


def _fixed_params(src):
    """registry api_fixed_params("k=v&k2=v2") → dict."""
    out = {}
    fixed = str(src.get("api_fixed_params", "") or "").strip()
    for kv in fixed.split("&"):
        if "=" in kv:
            k, v = kv.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def _http_get(url, params, retries=2, timeout=60):
    """GET(재시도 1회). ★예외 메시지에 URL(쿼리스트링=OC 포함)이 실리므로 scrub 후 재던진다."""
    last = None
    for attempt in range(1, retries + 1):
        try:
            r = requests.get(url, params=params, timeout=timeout)
            r.raise_for_status()
            return r
        except Exception as e:                       # noqa: BLE001 (수집 견고성 우선)
            last = _scrub(e)
            if attempt < retries:
                time.sleep(2.0 * attempt)
    raise RuntimeError(f"GET 실패({retries}회): {last}")


# ── 본문 수집 (DRF lawService.do) ──
def collect_law_body(src, oc, today):
    sid = str(src.get("source_id", "")).strip()
    name = str(src.get("source_name", "")).strip()
    fixed = _fixed_params(src)
    mst = fixed.get("MST", "")
    if not mst:
        print(f"  [건너뜀] {sid}: registry api_fixed_params 에 MST 없음")
        return None

    xml_path = os.path.join(RAW_DIR, f"{sid}_law_{mst}_{today}.xml")
    meta_path = os.path.join(RAW_DIR, f"{sid}_law_{mst}_{today}.meta.json")

    # 재실행 시 당일 완료분 skip(수집기 공통 관례 — 21_collect_koshaguide_pdf 미러)
    if os.path.exists(xml_path) and os.path.getsize(xml_path) > 0 and os.path.exists(meta_path):
        print(f"  [skip] {sid}: 당일 스냅샷 이미 존재 → {os.path.basename(xml_path)}")
        return json.load(open(meta_path, encoding="utf-8"))

    params = {"OC": oc, "target": fixed.get("target", "law"), "MST": mst, "type": "XML"}
    print(f"  GET {LAW_SERVICE_URL}?OC=<masked>&target={params['target']}&MST={mst}&type=XML")
    r = _http_get(LAW_SERVICE_URL, params)

    # 응답 검증: 루트가 <법령>이어야 정상. 아니면 인증/파라미터 오류 응답(HTML 등).
    try:
        root = ET.fromstring(r.content)
    except ET.ParseError:
        preview = _scrub(r.text[:300].replace("\n", " "))
        raise RuntimeError(
            f"{sid}: XML 파싱 실패 — 응답이 법령 XML 이 아님(인증 오류 가능). "
            f"★'사용자 정보 검증 실패'면 OC 보다 등록 IP(121.161.241.60)부터 대조. 미리보기: {preview}")
    if root.tag != "법령":
        preview = _scrub(r.text[:300].replace("\n", " "))
        raise RuntimeError(
            f"{sid}: 루트 태그 '{root.tag}' (기대: 법령) — "
            f"★'사용자 정보 검증 실패'면 OC 보다 등록 IP(121.161.241.60)부터 대조. 미리보기: {preview}")

    # 조문단위 집계(수집 메타용 — 가공은 하지 않는다)
    units = root.findall("조문/조문단위")
    n_articles = sum(1 for u in units
                     if (u.findtext("조문여부") or "").strip() == "조문")
    basic = root.find("기본정보")
    law_name_xml = (basic.findtext("법령명_한글") or "").strip() if basic is not None else ""
    eff_date = (basic.findtext("시행일자") or "").strip() if basic is not None else ""

    # 원문 XML 무변경 보관(bytes 그대로 — 인코딩 선언 포함 원본 보존)
    os.makedirs(RAW_DIR, exist_ok=True)
    with open(xml_path, "wb") as f:
        f.write(r.content)

    meta = {
        "source_id": sid,
        "source_name": name,
        "law_name_xml": law_name_xml,
        "endpoint": LAW_SERVICE_URL,          # ★파라미터(OC) 미포함 — 시크릿 규율
        "mst": mst,
        "law_effective_date": eff_date,
        "collected_at": datetime.now().isoformat(timespec="seconds"),
        "unit_count": len(units),             # 조문단위 전체(장·절 등 '전문' 포함)
        "article_count": n_articles,          # 조문여부='조문' 만(청킹 모수)
        "xml_path": xml_path,
        "bytes": len(r.content),
    }
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    print(f"  [저장] {os.path.basename(xml_path)}  "
          f"(조문단위 {len(units):,} · 조문 {n_articles:,} · {len(r.content):,} bytes)")
    if law_name_xml and law_name_xml != name:
        print(f"  [주의] {sid}: registry source_name('{name}') ≠ XML 법령명('{law_name_xml}') — 확인 필요")
    return meta


# ── 개정 감지 (data.go.kr 1170000 lawSearchList.do) ──
def _iter_search_entries(content):
    """목록 API 응답에서 법령 엔트리(dict)들을 뽑는다.
    응답 형식이 게이트웨이 경유에 따라 다를 수 있어 XML(DRF형 LawSearch)·JSON 양쪽을 방어적으로
    처리한다(메인 세션 실호출은 resultCode=00 확인 — 표준 래퍼 가능성도 흡수)."""
    text = content.decode("utf-8", errors="replace") if isinstance(content, bytes) else str(content)
    entries = []
    # 1) XML 시도
    try:
        root = ET.fromstring(content)
        for law in root.iter("law"):
            d = {c.tag: (c.text or "").strip() for c in law}
            entries.append(d)
        if not entries:
            # DRF형: <LawSearch><law>...</law></LawSearch> 외에 한글 태그 변형도 방어
            for law in root.iter("법령"):
                d = {c.tag: (c.text or "").strip() for c in law}
                entries.append(d)
        return entries, None
    except ET.ParseError:
        pass
    # 2) JSON 시도
    try:
        js = json.loads(text)
        node = js
        for key in ("LawSearch", "response", "body", "items"):
            if isinstance(node, dict) and key in node:
                node = node[key]
        cand = None
        if isinstance(node, dict):
            cand = node.get("law") or node.get("item")
        elif isinstance(node, list):
            cand = node
        if isinstance(cand, dict):
            cand = [cand]
        for it in cand or []:
            if isinstance(it, dict):
                entries.append({k: str(v).strip() for k, v in it.items()})
        return entries, None
    except Exception:                                # noqa: BLE001
        return [], text[:300]


def _entry_val(d, *names):
    """엔트리에서 이름 후보(한글/영문 표기 편차) 순회 조회."""
    for n in names:
        if n in d and str(d[n]).strip():
            return str(d[n]).strip()
    return ""


def check_revision(targets, service_key, today):
    """법령 6종의 목록 메타(MST/공포일자/시행일자)를 조회해 스냅샷 JSON 으로 남기고,
    직전 스냅샷과 diff 를 콘솔 표로 출력한다. ★자동 재수집은 하지 않는다(비범위 —
    변경 감지 시 registry api_fixed_params 의 MST 를 사람이 갱신 후 본 수집기 재실행)."""
    rows = []
    for src in targets:
        sid = str(src.get("source_id", "")).strip()
        name = str(src.get("source_name", "")).strip()
        params = {
            "serviceKey": service_key,
            "target": "law",
            "query": name,
            "numOfRows": 50,
            "pageNo": 1,
        }
        try:
            r = _http_get(LAW_SEARCH_URL, params)
            entries, err_preview = _iter_search_entries(r.content)
        except Exception as e:                       # noqa: BLE001
            print(f"  [오류] {sid}: {_scrub(e)}")
            rows.append({"source_id": sid, "law_name": name, "error": _scrub(e)})
            continue
        if err_preview:
            print(f"  [오류] {sid}: 응답 해석 불가 — 미리보기: {_scrub(err_preview)}")
            rows.append({"source_id": sid, "law_name": name, "error": "응답 해석 불가"})
            continue

        # 법령명 정확 일치 + (있다면) 현행 상태 우선
        hit = None
        for d in entries:
            nm = _entry_val(d, "법령명한글", "lawNmKor", "법령명", "lawNm")
            if nm == name:
                st = _entry_val(d, "현행연혁코드", "curHistCd")
                if st in ("", "현행"):
                    hit = d
                    break
                hit = hit or d
        if hit is None:
            print(f"  [주의] {sid}: 목록에서 '{name}' 정확 일치 없음 (후보 {len(entries)}건)")
            rows.append({"source_id": sid, "law_name": name, "error": "목록 일치 없음"})
            continue
        rows.append({
            "source_id": sid,
            "law_name": name,
            "mst": _entry_val(hit, "법령일련번호", "lawSn", "MST"),
            "law_id": _entry_val(hit, "법령ID", "lawId"),
            "promulgation_date": _entry_val(hit, "공포일자", "promulgationDt", "promDt"),
            "effective_date": _entry_val(hit, "시행일자", "enfcDt"),
            "revision_kind": _entry_val(hit, "제개정구분명", "rvsnKindNm"),
        })
        time.sleep(0.3)   # 게이트웨이 배려

    # 직전 스냅샷 diff (오늘자 산출물 제외한 가장 최근 파일)
    out_path = os.path.join(RAW_DIR, f"law_revision_check_{today}.json")
    prev_files = sorted(p for p in glob.glob(os.path.join(RAW_DIR, "law_revision_check_*.json"))
                        if os.path.abspath(p) != os.path.abspath(out_path))
    prev = {}
    if prev_files:
        try:
            for pr in json.load(open(prev_files[-1], encoding="utf-8")).get("laws", []):
                prev[pr.get("source_id")] = pr
        except Exception:                            # noqa: BLE001
            prev = {}

    print("\n== 개정 감지 결과 ==")
    print(f"{'source_id':<14} {'MST':>8} {'공포일자':>10} {'시행일자':>10}  판정")
    changed = 0
    for row in rows:
        sid = row["source_id"]
        if row.get("error"):
            print(f"{sid:<14} {'-':>8} {'-':>10} {'-':>10}  조회 실패: {row['error']}")
            continue
        # registry 의 현재 MST 와도 대조(스냅샷이 없어도 즉시 판정 가능)
        reg_mst = ""
        for src in targets:
            if str(src.get("source_id", "")).strip() == sid:
                reg_mst = _fixed_params(src).get("MST", "")
        p = prev.get(sid)
        marks = []
        if reg_mst and row.get("mst") and reg_mst != row["mst"]:
            marks.append(f"★MST 변경(registry {reg_mst} → 목록 {row['mst']})")
        if p:
            for k, label in (("mst", "MST"), ("promulgation_date", "공포일자"),
                             ("effective_date", "시행일자")):
                if p.get(k) and row.get(k) and p[k] != row[k]:
                    marks.append(f"{label} {p[k]}→{row[k]}")
        verdict = " · ".join(marks) if marks else ("변동 없음" if (p or reg_mst) else "기준선(직전 스냅샷 없음)")
        if marks:
            changed += 1
        print(f"{sid:<14} {row.get('mst',''):>8} {row.get('promulgation_date',''):>10} "
              f"{row.get('effective_date',''):>10}  {verdict}")

    snapshot = {
        "checked_at": datetime.now().isoformat(timespec="seconds"),
        "endpoint": LAW_SEARCH_URL,                  # ★serviceKey 미포함
        "previous_snapshot": os.path.basename(prev_files[-1]) if prev_files else None,
        "changed_count": changed,
        "laws": rows,
    }
    os.makedirs(RAW_DIR, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(snapshot, f, ensure_ascii=False, indent=2)
    print(f"\n[저장] {out_path}  (변경 감지 {changed}건)")
    if changed:
        print("→ 변경된 법령은 registry(sources.xlsx) api_fixed_params 의 MST 를 갱신한 뒤 본 수집기를"
              " 재실행하고, 재적재 후 sql/prafta-062-law-revision-2.sql 절차로 구버전 조문을 내린다.")
    return snapshot


def main():
    ap = argparse.ArgumentParser(description="법령 조문 XML 수집기(law.go.kr DRF)")
    ap.add_argument("--source", help="특정 source_id만 (예: LAW_OSHA)")
    ap.add_argument("--check-revision", action="store_true",
                    help="목록 API 로 개정 감지만 수행(본문 수집 안 함)")
    args = ap.parse_args()

    env = _ca.load_env(_ca.ENV_PATH)
    oc = env.get("LAW_GO_KR_OC", "")
    service_key = env.get("DATA_GO_KR_SERVICE_KEY", "")
    # ★시크릿 마스킹 목록: 원문뿐 아니라 URL 인코딩 변형까지 등록한다 —
    #   requests 의 HTTPError 메시지엔 URL 이 percent-encoding 된 형태로 실려
    #   원문 치환만으론 빠져나갈 수 있다(보안 리뷰 Low, 2026-09-03).
    for v in (oc, service_key):
        if v:
            _SECRETS.extend({v, urllib.parse.quote(v, safe=""), urllib.parse.quote_plus(v)})

    registry = _ca.load_registry(_ca.REGISTRY_PATH)
    targets = []
    for src in registry:
        sid = str(src.get("source_id", "")).strip()
        if not sid.startswith(LAW_SOURCE_PREFIX):
            continue
        if args.source and sid != str(args.source):
            continue
        if not _ca.is_checked(src.get("license_checked")):
            print(f"[게이트] {sid}: license_checked 아님 → 건너뜀(하드가드#1)")
            continue
        targets.append(src)
    if not targets:
        sys.exit("[안내] 수집 대상(LAW_* + license_checked=TRUE) 없음. registry 확인.")

    if args.check_revision:
        if not service_key or service_key.startswith("여기에"):
            sys.exit("[실패] DATA_GO_KR_SERVICE_KEY 미설정. corpus/.env 확인.")
        print(f"== 법령 개정 감지 · 대상 {len(targets)}건 ==")
        check_revision(targets, service_key, date.today().strftime("%Y%m%d"))
        return

    if not oc or oc.startswith("여기에"):
        sys.exit("[실패] LAW_GO_KR_OC 미설정. corpus/.env 확인(.env.example 참조).")

    today = date.today().strftime("%Y%m%d")
    print(f"== 법령 본문 수집 시작 · 대상 {len(targets)}건 ==")
    results = []
    for src in targets:
        print(f"\n[{src.get('source_id')}] {src.get('source_name')}")
        try:
            m = collect_law_body(src, oc, today)
            if m:
                results.append(m)
        except Exception as e:                       # noqa: BLE001
            print(f"  [오류] {src.get('source_id')}: {_scrub(e)}")
        time.sleep(0.5)   # 서버 배려

    print("\n== 요약 ==")
    total_articles = 0
    for m in results:
        total_articles += m.get("article_count", 0)
        print(f"  {m['source_id']:<14} 조문 {m.get('article_count', 0):>5,} · "
              f"조문단위 {m.get('unit_count', 0):>5,} · 시행 {m.get('law_effective_date', '')}")
    print(f"  합계 조문 {total_articles:,}건 (기대치 1,515 — 개정으로 달라질 수 있음)")


if __name__ == "__main__":
    main()
