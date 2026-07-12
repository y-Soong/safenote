# -*- coding: utf-8 -*-
"""
마스킹 유틸 (결정③: 익명 치환 + 주소 시군구까지 보존, 불확실 시 제거).

- mask_company: 업체명((주)X, X건설/산업/발전 등) → 유형 접미어 보존 익명치환(○○건설 등)
- mask_person : 성명+직함(홍길동 소장 등) 보수적 익명치환(○○○)
- mask_known  : 레코드의 알려진 식별값(facilNm 등)을 본문에서 치환
- truncate_address: 주소를 시/도+시군구(앞 2토큰)까지만 보존

하드가드#2: 마스킹은 필수. 패턴이 애매하면 호출부에서 필드 제거를 택한다.
"""
import re

# 업체명 마스킹:
#   (1) 명시적 법인표기(㈜/(주)/(유)/주식회사) — 고신뢰
#   (2) 업체 접미(X건설/산업/중공업...) — 단, 접미가 '단어 끝'일 때만(뒤에 한글 없음).
#       이 (?![가-힣]) 경계 덕에 '대우건설'(회사)은 잡고 '건설현장·산업도로·산업안전'(일반어)은
#       접미 뒤에 한글이 이어져 매칭되지 않는다.
_COMPANY = re.compile(
    r"㈜\s*[가-힣A-Za-z0-9]{1,20}"
    r"|\(주\)\s*[가-힣A-Za-z0-9]{1,20}"
    r"|\(유\)\s*[가-힣A-Za-z0-9]{1,20}"
    r"|주식회사\s*[가-힣A-Za-z0-9]{1,20}"
    r"|[가-힣A-Za-z0-9]{2,20}\s*(?:주식회사|㈜|\(주\)|\(유\))"
    r"|[가-힣]{2,6}(?:종합건설|엔지니어링|중공업|건설|산업|이엔씨)(?![가-힣])"
)

# 본문 상세주소 → 시/도+시군구까지만(동/읍/면·번지 이하 제거). 시군구는 지역 보존.
_ADDR_DETAIL = re.compile(
    r"([가-힣]{2,10}(?:특별시|광역시|특별자치시|특별자치도|도)\s*)?"   # (선택) 시/도
    r"([가-힣]{1,10}(?:시|군|구))"                                     # 시군구 (여기까지 보존)
    r"((?:\s+[가-힣0-9]{1,12}(?:동|읍|면|리|가|로|길))"                # 상세: 동/읍/면/로... (앞 공백 필수)
    r"(?:\s*[0-9]+(?:-[0-9]+)?(?:번지|번길|호|번|가)?)?)+"            # 번지/호
)


def truncate_addr_in_text(text):
    """본문 서술 속 상세주소를 시/도+시군구까지로 절단."""
    if not text:
        return text
    return _ADDR_DETAIL.sub(lambda m: (m.group(1) or "") + m.group(2), text)

# 성명: 3자 한글 성명 + '씨' + (조사/경계). '씨' 뒤에 조사(가/는/이/를…)나 공백·문장부호가
#   오는 경우만 성명으로 본다. '씨앗·씨족' 등 명사(씨 뒤 앗/족)는 제외.
#   ※ '군(郡)'=행정구역, '양'=다수 단어, 직함(소장/반장)은 현장소장 등 오탐이라 전부 제외.
_PERSON = re.compile(r"[가-힣]{3}\s*씨(?=[\s,.)\]]|가|는|은|이|를|의|도|와|과|에|께|만|$)")


def mask_company(text):
    if not text:
        return text
    return _COMPANY.sub("○○사", text)


def mask_person(text):
    if not text:
        return text
    return _PERSON.sub("○○○씨", text)


def mask_known(text, names):
    """레코드의 알려진 식별값(리스트)을 본문에서 placeholder 로 치환."""
    if not text:
        return text
    for nm in names or []:
        nm = (nm or "").strip()
        if len(nm) >= 2:
            text = text.replace(nm, "○○시설")
    return text


def truncate_address(addr):
    """주소를 앞 2토큰(시/도 + 시군구)까지만 보존. 상세 이하 제거."""
    a = (addr or "").strip()
    if not a:
        return a
    toks = a.split()
    return " ".join(toks[:2])


def mask_body(text, addr_values=None, other_values=None):
    """본문 마스킹(필드유형 구분):
    - addr_values: 본문에 등장하면 시군구까지로 절단(지역 보존, 상세 제거)
    - other_values: 조직명 등 식별값 → '○○' 치환
    - 이후 업체명(㈜..)·성명(이름+씨) 종합 마스킹
    """
    if not text:
        return text
    for a in addr_values or []:
        a = (a or "").strip()
        if len(a) >= 4 and a in text:
            text = text.replace(a, truncate_address(a))
    for o in other_values or []:
        o = (o or "").strip()
        if len(o) >= 2 and o in text:
            text = text.replace(o, "○○")
    text = truncate_addr_in_text(text)   # 본문 상세주소 → 시군구
    text = mask_company(text)
    text = mask_person(text)
    return text
