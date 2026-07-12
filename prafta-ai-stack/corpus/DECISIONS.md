# 코퍼스 파이프라인 — §7 미결정 4건 확정 기록

> 확정일 2026-07-02 · 근거: 작업지시서 `.claude/refs/작업지시서_AI코퍼스_수집적재_파이프라인.md` §7
> "판정은 사람, 실행은 코드" 원칙에 따라 사람이 확정. 파이프라인 구현 시 아래를 그대로 반영한다.

## ① HAZARD_TEXT 매핑 → **전용 필드 있을 때만 매핑, 없으면 NULL**

- 자유텍스트(사고경위)에서 규칙/LLM으로 유해요인을 **도출하지 않는다**(오분류·할루시네이션 위험 회피).
- registry `preprocess.hazard_field` 에 실제 컬럼이 지정된 자료만 그 값을 HAZARD_TEXT 로 매핑.
  - 예: 건설사고사례(15108262) → `hazard_field: null` → HAZARD_TEXT = NULL
  - 예: SIF(15140383) 등 유해·위험요인 컬럼 보유 자료 → 해당 컬럼 매핑
- 검색은 CONTENT 임베딩으로 도므로 NULL이어도 검색 품질에 영향 없음. HAZARD_TEXT는 보조·필터용.

## ② CHUNK_ID 생성 → **content_hash 기반 결정적 ID (멱등 UPSERT)**

- TB_CMM_SEQ(MySQL 순번) 미사용 — 순번은 재실행 시 중복 적재를 유발하고, 별도 PostgreSQL+Python 배치에 부적합.
- 결정적 ID = 동일 입력이면 항상 동일 ID → UPSERT로 멱등 보장(§6). 소스 버전 갱신 시 바뀐 행만 신규 반영.
- **스킴(VARCHAR(40) 준수)**: `{SOURCE_ID}_{sha256(SOURCE_ID + '|' + locator + '|' + CONTENT)[:16]}`
  - 예: `15108262_a1b2c3d4e5f60718` (데이터셋ID 8 + '_' + 해시 16 = 25자, 40자 이내)
  - locator 없으면 빈 문자열. 동일 SOURCE 내 완전 동일 CONTENT는 같은 ID로 자연 dedup(허용).
- 소스 버전 갱신 대응: 파일 날짜 접미사 변경 감지 시 해당 SOURCE_ID 구청크 USE_YN='N' 후 재적재(이력 유지, §6).

## ③ 마스킹 강도 → **익명 치환 + 주소 시군구까지 보존**

- 업체명 → `○○건설`, 성명 → `○○○` 형태 **익명 치환**(문장 구조 보존 → 임베딩 맥락 유지).
- 주소 → **시군구까지만 보존**, 상세(동/번지 이하) 제거. (시군구는 개인 식별정보 아님, 지역 위험패턴 검색에 유용)
- **적용 범위**: `mask_fields` 지정 필드뿐 아니라 **CONTENT(사고경위) 자유텍스트 내 박힌 업체명/성명도 정규식으로 스캔**해 치환.
  - 패턴 예: `(주)X`, `X건설`, `X산업`, `X종합건설`, `X씨`, 성명+직함 등.
- **불확실 시**: 패턴이 애매해 오치환 위험이 크면 해당 필드/구절 **통째 제거 후 로그**(§4-2). 미마스킹 데이터는 어떤 산출물에도 남기지 않음(하드가드 #2).
- ⚠️ **선행 확인 필요**: 실제 raw CSV를 열어 국토안전관리원 데이터가 **원천에서 이미 익명화**돼 있는지 확인 → 마스킹 규칙 최종 튜닝. (이미 익명화면 정규식은 안전망 역할)

## ④ AI유형 자료 3조건(문체부) → **MVP 제외/보류**

- 현재 대상 자료(건설사고사례 "제한없음", SIF)는 AI유형 비해당 → 지금 3조건 이행 설계 안 함.
- 실제 AI유형 라이선스 자료를 registry에 채택하는 시점에 별도 설계:
  1. 동일·유사 산출물 방지 → 기존 **이중 트랙 물리 분리**(verbatim/recompose) 위에 확장
  2. RAG 직접인용 출처명시 → 기존 **tb_ai_corpus_source + SOURCE_LOCATOR** 활용
  3. 학습데이터 재판매 금지 → 계약/정책 사항(코드 아님)

---

## 보류/백로그

- ~~**15140227 (기인물 유해위험요인 PDF) 전처리 보류**~~ → **해결·적재 완료 (2026-07-03)**:
  - data.go.kr 페이지의 'CSV' 표기는 오류였고 실제로는 PDF만 제공(다운로드 버튼에 CSV 없음).
  - Anaconda base가 Python 3.7.3라 base pip(구버전 pytoml)·권한(ProgramData admin) 문제로 설치가 막혔으나,
    **`pip install --user`로 최신 pip + pdfplumber 0.9.0 를 사용자 영역에 설치**(관리자 불필요)하여 해결.
  - `read_pdf`(preprocess.py): pdfplumber `extract_tables()` 괘선 기반 셀 복원 → 개요(page0) 제외,
    순번 숫자 행만, 기인물명 병합셀 forward-fill. 332행 → 332청크(유해요인·대책 100%).
  - ⚠️ 부작용: pdfplumber가 끌어온 cryptography 45가 base pyOpenSSL을 깨 `requests` import 실패 →
    **user-site cryptography 제거**로 해결(암호화 안 된 PDF는 pdfminer가 cryptography 불필요).

## 이 결정들이 남긴 선행 조건 (파이프라인 착수 전)

- [ ] `corpus/raw/` 에 실제 원본 파일 투입 (사용자)
- [ ] `corpus/registry/sources.xlsx` 에 자료별 라이선스 판정 기입 + `license_checked: TRUE` (사람 확정)
- [ ] raw CSV 실물 확인 → ③ 마스킹 정규식 최종 튜닝
