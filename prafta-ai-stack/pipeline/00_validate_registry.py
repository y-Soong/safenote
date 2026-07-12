# -*- coding: utf-8 -*-
"""
00_validate_registry — 레지스트리 게이트 검사.

작업지시서 §1(하드가드#1)·§6·§9 이행:
  - license_checked=TRUE 인 자료만 '처리대상'으로 통과시킨다.
  - FALSE/누락은 '제외'로 리포트한다(추측·자동승격 금지).
  - 필수 필드/원본 파일 존재/track↔라이선스 정합/신뢰등급 누락을 점검한다.

이 스크립트는 검사·리포트만 한다(데이터 변경 없음). 후속 단계(10_preprocess 등)는
여기서 '처리대상'으로 통과한 자료만 대상으로 한다.

사용: python pipeline/00_validate_registry.py
"""
import glob
import importlib.util
import os
import sys

# collect_api 의 registry 로더 재사용(중복 방지)
HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("collect_api", os.path.join(HERE, "collect_api.py"))
_ca = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_ca)

REGISTRY_PATH = _ca.REGISTRY_PATH
RAW_DIR = _ca.RAW_DIR
load_registry = _ca.load_registry
is_checked = _ca.is_checked

REQUIRED = ["source_id", "source_name", "license_type", "feed_type"]
VERBATIM_MARKERS = ["제3유형", "변경금지"]
VALID_RELIABILITY = {"규범형", "집계형", "자율신고형"}


def raw_file_exists(src):
    """FILE: file 컬럼 경로의 원본이 raw/ 에 있나. API: {source_id}_*.json 스냅샷 있나."""
    sid = str(src.get("source_id", "")).strip()
    feed = str(src.get("feed_type", "")).strip().upper()
    if feed == "API":
        hits = glob.glob(os.path.join(RAW_DIR, f"{sid}_*.json"))
        return bool(hits), (os.path.basename(hits[0]) if hits else "(스냅샷 없음: collect_api 필요)")
    fname = os.path.basename(str(src.get("file", "")).strip())
    if not fname:
        return False, "(file 미지정)"
    path = os.path.join(RAW_DIR, fname)
    return os.path.exists(path), fname


def validate(src):
    """단일 자료 검증 → (status, reasons[]). status ∈ {PASS, EXCLUDED, ERROR}."""
    reasons = []
    sid = str(src.get("source_id", "")).strip()

    # 1) 필수 필드
    missing = [f for f in REQUIRED if not str(src.get(f, "")).strip()]
    if missing:
        reasons.append(f"필수 필드 누락: {', '.join(missing)}")

    # 2) content_fields (처리 본문 재료)
    if not src.get("content_fields"):
        reasons.append("content_fields 비어있음(청크 CONTENT 재료 없음)")

    # 3) 원본 존재
    exists, shown = raw_file_exists(src)
    if not exists:
        reasons.append(f"원본 미존재: {shown}")

    # 4) track ↔ 라이선스 정합
    ltype = str(src.get("license_type", ""))
    track = str(src.get("track", "")).strip()
    if any(mk in ltype for mk in VERBATIM_MARKERS) and track != "verbatim":
        reasons.append(f"라이선스 '{ltype}'는 변경금지 계열 → track=verbatim 이어야 함(현재 '{track}')")

    # 5) 신뢰등급
    rel = str(src.get("data_reliability", "")).strip()
    if rel and rel not in VALID_RELIABILITY:
        reasons.append(f"data_reliability 값 이상: '{rel}'")
    rel_warn = "" if rel in VALID_RELIABILITY else "  ⚠ 신뢰등급 미지정"

    # 6) 라이선스 게이트(하드가드#1) — 가장 마지막에 판정
    if not is_checked(src.get("license_checked")):
        return "EXCLUDED", ["license_checked≠TRUE → 처리 제외(사람 확정 필요)"], rel_warn

    # 구조 오류가 있으면 ERROR(게이트는 통과했지만 처리 불가)
    if reasons:
        return "ERROR", reasons, rel_warn
    return "PASS", [], rel_warn


def main():
    if not os.path.exists(REGISTRY_PATH):
        sys.exit(f"[실패] 레지스트리 없음: {REGISTRY_PATH}")
    reg = load_registry(REGISTRY_PATH)

    print(f"== 레지스트리 검증: {os.path.basename(REGISTRY_PATH)} · 총 {len(reg)}개 ==\n")
    counts = {"PASS": 0, "EXCLUDED": 0, "ERROR": 0}
    passed = []
    for src in reg:
        sid = str(src.get("source_id", "")).strip()
        name = str(src.get("source_name", ""))
        feed = str(src.get("feed_type", ""))
        track = str(src.get("track", ""))
        rel = str(src.get("data_reliability", ""))
        status, reasons, rel_warn = validate(src)
        counts[status] += 1
        mark = {"PASS": "✅ 처리대상", "EXCLUDED": "⛔ 제외", "ERROR": "❌ 오류"}[status]
        print(f"[{sid}] {name}")
        print(f"   {mark}  (feed={feed}, track={track}, reliability={rel or '-'}){rel_warn}")
        for rr in reasons:
            print(f"     - {rr}")
        if status == "PASS":
            passed.append(sid)
        print()

    print("== 요약 ==")
    print(f"  처리대상(PASS): {counts['PASS']}  |  제외(EXCLUDED): {counts['EXCLUDED']}  |  오류(ERROR): {counts['ERROR']}")
    print(f"  처리대상 목록: {', '.join(passed) if passed else '(없음)'}")
    # 게이트는 통과했으나 구조 오류가 있으면 후속 단계가 막히므로 비정상 종료로 신호
    sys.exit(1 if counts["ERROR"] else 0)


if __name__ == "__main__":
    main()
