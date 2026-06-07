# prafta-052 — 엑셀 업로드 실패 항목 재업로드용 다운로드 (2시트)

## 작업 영역
웹/백엔드 (`PRAFTA/prafta-backend`, `PRAFTA/prafta-web-frontend`)

## 배경
User_01(사용자관리) 화면의 엑셀 업로드(사용자 일괄 생성) 결과는 공용 팝업 `BatchResultPop`으로 표시된다.
현재 팝업에는 "엑셀 출력" 버튼이 이미 있으나, **단일 시트("처리결과")에 `사용자ID + 비고(사유)` 2컬럼만** 내려준다.
실패한 행의 원본 입력값이 없어, 사용자가 실패분을 고쳐서 재업로드하기 어렵다.

## 요구사항 (B안)
엑셀 업로드에서 insert 실패한 행을, **재업로드 가능한 형태**로 다운로드할 수 있게 한다.

1. **시트1 "실패 항목"**: 업로드 양식(`UserExcelTemplateBuilder`/`UserExcelRowParser.HEADERS`)과 **동일한 16컬럼 헤더 + 실패한 행의 원본 입력값**.
   - 사용자가 셀만 수정해서 그대로 재업로드할 수 있어야 한다(헤더/컬럼 순서 양식과 1:1 일치).
2. **시트2 "실패 사유"**: 행별 실패 사유.
   - 권장 컬럼: `엑셀 행번호`, `사용자ID`, `실패 사유(message)`, `에러코드(errorCode)`.
   - 엑셀 행번호 = 양식 기준 실제 행. 데이터는 4행부터 시작(1행 안내/2행 헤더/3행 예시), 파싱 index는 0-based 이므로 `index + 4` 로 환산.

## 확정 사항 / 제약
- **공용 팝업 영향 최소화 (핵심)**: `BatchResultPop`은 User_01 한 곳에서만 실제 사용되며, 그 안에서 3개 경로가 공유한다.
  - (a) 그리드 다중체크 저장 (`POST /user01/update-user-infos`) — **원본 행 데이터 없음**
  - (b) 동기 엑셀 업로드 (`/user01/upload-user-creates`)
  - (c) 비동기 엑셀 업로드 잡 (`/user01/upload-user-creates-async` + 폴링)
  - → **원본 행 데이터가 실려 있을 때만 2시트로 분기**하고, 없으면(=그리드 저장 경로) 기존 단일 시트 동작을 그대로 유지해야 한다. 기존 호출부 회귀 금지.
  - (참고) `PolicyGrantPreviewPop.vue`의 BatchResultPop 언급은 CSS 주석일 뿐 실제 컴포넌트 사용 아님.
- **백엔드: 실패 항목에 원본 행 값 추가**
  - `UserUpdateFailItem`(record: `index, errorItem, errorCode, message`)에 원본 입력 행을 담을 수단 추가.
  - 동기 경로(`User01BatchServiceImpl.insertUserBatch`)와 **비동기 경로 양쪽** 반영 필수:
    - 비동기는 `UploadJobAsyncRunnerImpl`에서 `failsJson`으로 직렬화 → `UploadJobServiceImpl.parseFails`로 역직렬화 → `UserUploadJobStatusResponse.fails`로 노출. 원본 행 필드가 직렬화/역직렬화 양쪽에서 보존돼야 한다.
  - 원본 행 출처: `UserCreateParam`(16개 필드 전부 보유). 그리드 저장 경로(`updateUserInfoBatch`)의 fail item에는 원본 행을 채우지 않아 하위호환 유지.
  - PII 주의: 원본 행에 휴대폰/이메일/생년월일 등 평문 입력값이 포함됨. 응답 DTO/로그/직렬화에 노출 범위가 늘어나므로 security 에이전트가 점검.
- **프론트**: `BatchResultPop.fnExportExcel`을 확장. 원본 행 존재 시 2시트 생성(`xlsx` aoa_to_sheet + book_append_sheet 이미 사용 중).
- DB 스키마 변경 없음(업로드 잡 `failsJson` 컬럼은 기존 텍스트, payload만 확장).

## 검증 포인트 (QA 강조 — 사용자 명시 요청)
- `BatchResultPop`의 **다른 호출 경로 회귀 없음**을 디테일하게 확인:
  - 그리드 체크저장 실패 시 기존처럼 단일 시트(`사용자ID + 비고`)로 정상 출력되는지.
  - 동기/비동기 업로드 실패 시 2시트가 양식과 1:1로 정확히 생성되는지(헤더 순서, 행번호 환산 `index+4`, 컬럼 누락 없음).
- 부분 성공(partial) 시 성공 행은 제외되고 실패 행만 다운로드되는지.
- 동일 엑셀에 같은 사용자ID 중복 입력 시(첫 행 성공/둘째 행 USER_400_041) 시트1/시트2의 행 표기가 혼동되지 않는지.
- 비동기 잡 경로에서 `failsJson` 직렬화/역직렬화 라운드트립으로 원본 행 값이 유실되지 않는지.

## 정책서 출처
- 신규 비즈니스 룰 없음(기존 PRAFTA-036/037 엑셀 업로드 기능의 UX 개선). 도메인 정책 변경 없음.
