# prafta-052 작업지시서 — 엑셀 업로드 실패 항목 재업로드용 2시트 다운로드 (B안)

> 출처 요청서: `.claude/requests/web_requests/prafta-052.md`
> 작업 영역: 웹/백엔드 (`PRAFTA/prafta-backend`, `PRAFTA/prafta-web-frontend`)
> 정책서 출처: **신규 비즈니스 룰 없음** — PRAFTA-036/037 엑셀 업로드 기능의 UX 개선. 도메인 정책 변경 없음.
> 스키마 변경: **없음** (업로드 잡 `failsJson` 컬럼은 기존 텍스트, payload만 확장).

---

## 0. 핵심 설계 결정 (developer 착수 전 필독)

이 작업의 본질은 **"실패한 행의 원본 입력값(16컬럼)을 응답까지 실어 나르고, 프론트가 그게 있을 때만 2시트로 분기"** 하는 것이다. 세 가지 절대 제약을 먼저 못 박는다.

### D1. 공용 팝업 회귀 금지 (최우선 제약)
`BatchResultPop`은 `User_01.vue` 한 곳에서만 실사용되며 그 안에서 **3개 호출 경로**가 공유한다.
- (a) 그리드 다중체크 저장 `fnSave` → `POST /webApi/user01/update-user-infos` → `updateUserInfoBatch` — **원본 행 데이터 없음**
- (b) 동기 엑셀 업로드 `fnExcelFileChange`(현재는 비동기로 라우팅) — (역사적 경로) `uploadUserCreates` → `insertUserBatch`
- (c) 비동기 엑셀 업로드 잡 폴링 `fnPollUploadJob` → `getStatus` → `parseFails`

→ **원본 행 데이터가 존재할 때만 2시트로 분기**한다. (a) 경로는 원본 행이 null이므로 **기존 단일 시트(`사용자ID + 비고`) 동작을 100% 유지**한다.
> (참고) `PolicyGrantPreviewPop.vue`의 `BatchResultPop` 언급은 CSS 주석일 뿐 실제 컴포넌트 사용이 아니다. 회귀 대상 아님.

### D2. 원본 행 필드는 nullable (하위호환의 핵심 메커니즘)
`UserUpdateFailItem` record에 원본 행을 담는 필드를 **nullable**로 추가한다.
- (b)(c) 엑셀 경로: `UserCreateParam`(16필드 전부 보유)에서 원본 행을 채운다 → 필드 non-null.
- (a) 그리드 경로(`updateUserInfoBatch`): 원본 행을 **채우지 않는다**(null 유지). `UserInfoModel`에는 16컬럼 양식 원본이 없으므로 억지로 채우지 않는다.
- 프론트는 이 필드가 **존재하고 비어있지 않을 때만** 2시트 분기. null이면 기존 단일 시트.

### D3. 직렬화/역직렬화 라운드트립 보존
비동기 경로는 `UploadJobAsyncRunnerImpl.serializeFails`(Jackson `writeValueAsString`) → DB `failsJson` 텍스트 → `UploadJobServiceImpl.parseFails`(`readValue` + `TypeReference<List<UserUpdateFailItem>>`)로 왕복한다.
- 원본 행 필드를 `UserUpdateFailItem`에 추가하면 Jackson이 자동으로 양방향 직렬화/역직렬화한다. **별도 mapper 작업 불필요**(record + Jackson 기본 동작).
- 단, record에 필드를 추가하면 **역직렬화 시 기존 저장분(필드 없는 구버전 JSON)과의 호환**이 필요할 수 있다. 운영 중 진행 중이던 잡의 기존 `failsJson`은 신규 필드가 빠진 형태 → Jackson 기본은 누락 필드를 null로 채우므로 호환된다(아래 4-3 검증 포인트에서 명시 확인).

---

## 1. 작업 분해 요약 (의존 순서 포함)

| 작업 ID | 유형 | 영역 | 의존 | 요약 |
|---------|------|------|------|------|
| **PRAFTA-052-01** | backend | web/user/user01 | (없음, 선행) | `UserUpdateFailItem`에 원본 행(`sourceRow`) nullable 필드 추가 |
| **PRAFTA-052-02** | backend | web/user/user01 | 01 | 동기 배치(`insertUserBatch`)에서 fail item에 `UserCreateParam` 원본 행 채움 |
| **PRAFTA-052-03** | backend | web/user/user01/upload | 01 | 비동기 잡(`UploadJobAsyncRunnerImpl`)에서 fail item에 원본 행 채움 + 직렬화 라운드트립 검증 |
| **PRAFTA-052-04** | frontend-component | web (popup) | 01·02·03 | `BatchResultPop.fnExportExcel` 2시트 분기 (원본 행 존재 시) |

**의존 순서**: 01 → (02, 03 병렬 가능) → 04.
04는 백엔드 응답 계약(원본 행 필드명)이 확정된 뒤 착수. 단, 프론트 단일 시트 경로(그리드 저장)는 백엔드와 무관하므로 회귀만 안 깨지면 됨.

---

## 2. PRAFTA-052-01 — 실패 DTO에 원본 행 필드 추가 (선행)

- **유형**: backend
- **영역**: web
- **모듈**: user/user01
- **작업 유형**: 보완
- **정책서 출처**: 없음(UX 개선, 신규 룰 없음)

### 핵심 요구사항
1) `UserUpdateFailItem` record에 실패한 행의 원본 입력값을 담을 nullable 필드를 추가한다.
2) 그리드 저장 경로 하위호환을 위해 이 필드는 **null 허용**이어야 한다.
3) Jackson 직렬화/역직렬화가 자동으로 라운드트립되도록 표준 record 필드로 둔다(커스텀 직렬화기 금지).

### 영향 받는 파일
- `prafta-backend/src/main/java/com/prafta/web/user/user01/dto/UserUpdateFailItem.java`

### 구현 지시 (developer)
`UserUpdateFailItem`을 다음과 같이 확장한다. 원본 행은 **양식 16컬럼과 동일한 순서·의미를 가진 `List<String>`** 로 담는다(엑셀 시트1에 그대로 펼치기 위함).

```java
package com.prafta.web.user.user01.dto;

import java.util.List;

/**
 * 일괄 처리 실패 항목.
 *
 * <p>{@code sourceRow} 는 prafta-052 — 엑셀 업로드 실패 행의 원본 입력값(양식 16컬럼 순서)이다.
 * 그리드 다중체크 저장 경로(updateUserInfoBatch)에는 원본 양식 행이 없으므로 null 로 둔다(하위호환).
 * 엑셀 동기/비동기 업로드 경로에서만 채워진다.
 * Jackson 기본 직렬화/역직렬화로 failsJson 라운드트립에서 보존된다.
 */
public record UserUpdateFailItem(
        int index,          // 몇 번째 요청인지(0-based)
        String errorItem,
        String errorCode,
        String message,
        List<String> sourceRow   // prafta-052: 엑셀 양식 16컬럼 원본값. 그리드 경로는 null.
) {}
```

> **주의(컴파일 영향)**: record에 필드를 추가하면 기존 생성자 호출부가 전부 컴파일 에러가 난다. 호출처는 정확히 4곳이다 — 02/03에서 모두 수정한다. **그리드 경로(`updateUserInfoBatch`)의 2곳도 `null`을 명시 전달**해야 컴파일된다(아래 2-B 참조).

### 호출처 전수 (selectNextJobId 단계 — DB 컬럼 변경 아님이므로 mapper 영향 없음)
`new UserUpdateFailItem(...)` 호출처 4곳:
1. `User01BatchServiceImpl.updateUserInfoBatch` 2곳(ApiException catch / Exception catch) → **그리드 경로 = null 전달** (2-B)
2. `User01BatchServiceImpl.insertUserBatch` 2곳 → **엑셀 동기 경로 = 원본 행 전달** (2-A)
3. `UploadJobAsyncRunnerImpl.runAsync` 1곳 → **엑셀 비동기 경로 = 원본 행 전달** (3)

> 위 "4곳"은 record가 직접 생성되는 지점이고, async가 추가되어 총 5개 생성 지점이다. developer는 grep `new UserUpdateFailItem(` 로 재확인 후 전부 5인자 시그니처로 맞춘다.

---

## 3. PRAFTA-052-02 — 동기 배치 경로 원본 행 주입 + 그리드 경로 하위호환

- **유형**: backend / **영역**: web / **모듈**: user/user01 / **작업 유형**: 보완
- **선행 작업**: PRAFTA-052-01
- **정책서 출처**: 없음

### 영향 받는 파일
- `prafta-backend/.../user01/service/impl/User01BatchServiceImpl.java`

### 2-A. 엑셀 동기 경로 — `insertUserBatch`에서 원본 행 주입
`insertUserBatch(List<UserCreateParam> params)`의 두 catch 블록(`ApiException`, `Exception`)에서 fail item 생성 시 `p`(`UserCreateParam`)로부터 양식 16컬럼 순서의 `List<String>`을 만들어 5번째 인자로 전달한다.

원본 행 변환은 **단일 출처 헬퍼**로 만들어 02/03이 공유한다. `UserCreateParam` → 양식 16컬럼 매핑은 `UserExcelRowParser.HEADERS` 순서와 1:1 일치해야 한다(시트1 재업로드 호환의 핵심).

신규 정적 헬퍼 추가 위치 권장: **`UserExcelRowParser`에 `toSourceRow(UserCreateParam)` 정적 메서드** (HEADERS/parse와 같은 단일 출처에 둬서 컬럼 순서 drift 방지).

```java
// UserExcelRowParser 에 추가 (HEADERS 와 동일 순서 — 16컬럼)
/**
 * prafta-052 — 실패 행 재업로드용 원본 행(양식 16컬럼 순서)으로 변환한다.
 * {@link #HEADERS} 순서와 1:1 일치해야 한다(시트1 재업로드 호환).
 * creditMonths(Integer)는 문자열로, null 은 빈 문자열로 정규화한다.
 */
public static List<String> toSourceRow(UserCreateParam p) {
    if (p == null) return java.util.Collections.emptyList();
    return java.util.Arrays.asList(
            nz(p.userId())            // 0  사용자ID(필수)
          , nz(p.userNm())            // 1  사용자명(필수)
          , nz(p.authCd())            // 2  권한코드(필수)
          , nz(p.siteNo())            // 3  사업장번호(필수)
          , nz(p.nodeCd())            // 4  소속부서코드(필수)
          , nz(p.mblNo())             // 5  휴대폰번호(필수)
          , nz(p.email())             // 6  이메일
          , nz(p.gender())            // 7  성별(M/F)
          , nz(p.birthDt())           // 8  생년월일(YYMMDD)
          , nz(p.rankCd())            // 9  직급코드
          , nz(p.hireDate())          // 10 입사일(YYYYMMDD)
          , nz(p.employmentType())    // 11 고용형태
          , nz(p.contractEndDate())   // 12 계약종료일(YYYYMMDD)
          , p.creditMonths() == null ? "" : String.valueOf(p.creditMonths()) // 13 경력인정개월수
          , nz(p.creditReasonType())  // 14 경력인정사유유형(SYS042)
          , nz(p.creditReasonDetail())// 15 경력인정상세
    );
}

private static String nz(String s) { return s == null ? "" : s; }
```

> **주의**: `additionalSiteCdList`, `gv*`(토큰 클레임)은 양식 16컬럼에 **포함하지 않는다**. 양식은 16컬럼이며 토큰/추가권한은 업로드 양식에 없는 항목이다. 시트1은 정확히 16컬럼이어야 재업로드된다.

`insertUserBatch` 두 catch 수정:
```java
} catch (ApiException e) {
    ApiErrorCode code = e.getErrorCode();
    fails.add(new UserUpdateFailItem(
            i, p.userId(), code.code(), e.getResolvedMessage(),
            UserExcelRowParser.toSourceRow(p)));   // prafta-052
} catch (Exception e) {
    log.error("엑셀 일괄 생성 행 처리 실패 - index={}, userId={}", i, p.userId(), e);
    fails.add(new UserUpdateFailItem(
            i, p.userId(), CommonErrorCode.COMMON_500_001.code(),
            "처리 중 오류가 발생했습니다.\n관리자에게 문의해주세요.",
            UserExcelRowParser.toSourceRow(p)));   // prafta-052
}
```

### 2-B. 그리드 경로 하위호환 — `updateUserInfoBatch`는 원본 행 null
`updateUserInfoBatch`의 두 catch 블록은 양식 원본 행이 없으므로 5번째 인자에 **`null`을 명시 전달**한다. (단순 컴파일 정합 + 프론트 단일 시트 유지.)
```java
fails.add(new UserUpdateFailItem(i, model.userId(), e.getErrorCode().code(), e.getResolvedMessage(), null)); // prafta-052: 그리드 경로 원본 행 없음
// Exception catch 도 동일하게 마지막 인자 null
```

### 산출물
- `User01BatchServiceImpl.java`(수정), `UserExcelRowParser.java`(`toSourceRow` 헬퍼 추가)

---

## 4. PRAFTA-052-03 — 비동기 잡 경로 원본 행 주입 + 직렬화 라운드트립

- **유형**: backend / **영역**: web / **모듈**: user/user01/upload / **작업 유형**: 보완
- **선행 작업**: PRAFTA-052-01 (02와 병렬 가능, `toSourceRow` 헬퍼는 02에서 신설하므로 02 헬퍼 부분 선행 권장)
- **정책서 출처**: 없음

### 영향 받는 파일
- `prafta-backend/.../user01/upload/service/impl/UploadJobAsyncRunnerImpl.java` (생성 지점)
- (확인만) `prafta-backend/.../user01/upload/service/impl/UploadJobServiceImpl.java` (`parseFails` — 변경 불필요, 동작 확인)
- (확인만) `prafta-backend/.../user01/upload/dto/response/UserUploadJobStatusResponse.java` (필드 변경 불필요, `UserUpdateFailItem` 확장 자동 반영)

### 4-1. 원본 행 주입
`runAsync`의 fail 누적 지점(현재 75행):
```java
fails.add(new UserUpdateFailItem(i, p == null ? null : p.userId(), errorCode, message,
        UserExcelRowParser.toSourceRow(p)));   // prafta-052: 원본 행 보존
```
> `toSourceRow`는 `p == null` 가드를 내장하므로 null이면 빈 리스트 반환. 기존 `p == null ? null : p.userId()` 패턴과 일관.

### 4-2. 직렬화/역직렬화 (변경 없음 — 보존 메커니즘 명시)
- 직렬화: `serializeFails` → `objectMapper.writeValueAsString(fails)`. record에 `sourceRow`가 추가되면 Jackson이 `"sourceRow":["..."(16개)]`를 자동 포함. **코드 변경 불필요.**
- 역직렬화: `UploadJobServiceImpl.parseFails` → `readValue(json, TypeReference<List<UserUpdateFailItem>>)`. record 신규 필드 자동 매핑. **코드 변경 불필요.**
- DB `failsJson` 컬럼은 기존 텍스트(payload만 길어짐). **스키마 변경 없음.** 단, 16컬럼 × 행수 만큼 payload가 커지므로 컬럼 길이 한계 확인(아래 4-3 검증 포인트).

### 4-3. developer 자가검증 포인트
1. `new UserUpdateFailItem(` grep 결과 5개 생성 지점이 전부 5인자로 컴파일되는지.
2. `failsJson` 직렬화 후 `parseFails`로 역직렬화 시 `sourceRow` 16개 값이 그대로 복원되는지(라운드트립). — QA V5와 연결.
3. **구버전 호환**: `sourceRow` 키가 없는 기존 `failsJson`(운영 진행 중 잡)을 역직렬화하면 `sourceRow == null`로 채워지고 예외 없이 통과하는지(Jackson record 기본 동작 확인). — 예외나면 `@JsonInclude`/디폴트 대응 필요, 그러나 기본 동작상 통과 예상.
4. `failsJson` 컬럼 타입/길이 확인: 16컬럼 평문 × 다행이면 TEXT 한계(64KB)는 1000행 한도 내 대체로 안전하나, developer는 컬럼 정의(`SHOW CREATE TABLE`로 업로드 잡 테이블 확인)로 TEXT/MEDIUMTEXT 여부를 점검하고 한계 의심 시 보고.

### 산출물
- `UploadJobAsyncRunnerImpl.java`(수정). 그 외 파일은 자동 반영(코드 무변경).

---

## 5. PRAFTA-052-04 — `BatchResultPop` 2시트 분기 (프론트)

- **유형**: frontend-component / **영역**: web / **모듈**: components/popup
- **선행 작업**: PRAFTA-052-01·02·03 (응답 계약 `sourceRow` 확정 후)
- **정책서 출처**: 없음
- **화면 신규 아님**: 기존 공용 팝업 `fnExportExcel`만 확장. Vue 신규 골격 작성 불필요.

### 영향 받는 파일
- `prafta-web-frontend/.../src/components/popup/BatchResultPop.vue` (`fnExportExcel` 확장)
- (무변경, 회귀 확인 대상) `src/views/user/User_01.vue` — 3개 호출부 props는 그대로(`dataList`에 `sourceRow`가 실려옴). **수정 없음**이 원칙.

### 5-1. 분기 규칙 (D1·D2 구현)
`fnExportExcel` 안에서 `props.dataList` 중 **하나라도 `sourceRow`가 존재하고 비어있지 않으면** 2시트 모드, 아니면 기존 단일 시트 모드.
- 그리드 경로(a): `sourceRow == null` → 기존 단일 시트(`사용자ID + 비고`) 그대로.
- 엑셀 경로(b,c): `sourceRow` 16개 존재 → 2시트.

> 판정은 "전부" 아닌 "**하나라도 있으면**" 2시트로 한다(부분 성공·혼합 방어). 단 엑셀 경로의 fail item은 전부 sourceRow를 가지므로 실제 혼합은 발생하지 않는다.

### 5-2. 시트1 "실패 항목" (재업로드용)
- 헤더 = 양식 16컬럼. **백엔드 `UserExcelRowParser.HEADERS`와 1:1 일치해야 함.** 프론트엔드에는 HEADERS 단일출처가 없으므로 **상수 배열을 컴포넌트 내에 정의**하고 주석으로 "백엔드 `UserExcelRowParser.HEADERS`와 순서·문자열 동일 유지(prafta-052)" 명시. (drift 위험 — QA V2에서 헤더 일치 강조 검증.)
- 데이터 = 각 fail의 `sourceRow`(16개 셀)를 그대로 한 행으로.
- **시트1은 양식과 1:1**(안내행/예시행 없이 헤더 + 데이터). 사용자는 셀 수정 후 그대로 재업로드. (재업로드 파서는 1행 안내/2행 헤더/3행 예시/4행~데이터를 기대하지만, 시트1을 단순 헤더+데이터로 만들면 재업로드 시 파서의 `DATA_START_ROW_INDEX=3`과 어긋난다 → **아래 5-2-주의 확정 필요**.)

#### 5-2-주의 (developer/QA 모두 확인 — 재업로드 양식 정합)
업로드 파서 `UserExcelRowParser`는 **1행 안내 / 2행 헤더 / 3행 예시 / 4행(index 3)부터 데이터**를 가정한다. 시트1을 그대로 재업로드하려면 시트1도 동일 구조여야 한다. 두 가지 선택:
- **옵션 A (권장)**: 시트1을 양식과 동일 구조로 생성 — 1행 안내문, 2행 헤더(16컬럼), 3행 예시(빈/생략 가능하나 파서가 3행을 skip하므로 **반드시 placeholder 1행 필요**), 4행부터 실패 행 데이터. 이러면 다운로드 → 무수정 재업로드가 파서와 정합.
- **옵션 B**: 시트1을 헤더+데이터(2행 구조)로 단순화 → 재업로드 전 사용자가 양식에 복붙해야 함(요청서 "셀만 수정해서 그대로 재업로드" 취지에 어긋남).

→ **요청서 §요구사항 1 "셀만 수정해서 그대로 재업로드"** 를 충족하려면 **옵션 A**가 정답이다. developer는 시트1을 파서 구조(1행 안내 / 2행 헤더 / 3행 예시 placeholder / 4행~ 데이터)로 생성한다. 안내행 문자열은 양식과 동일하게(또는 "수정 후 이 시트를 그대로 업로드하세요" 안내) 둔다.
> ⚠️ 이 결정(옵션 A)은 요청서가 "시트1 = 양식과 동일 16컬럼 헤더 + 데이터"라고만 명시하고 안내/예시행 존재 여부를 명시하지 않은 부분이다. **재업로드 무수정 정합을 위해 옵션 A로 확정**하되, planner는 사용자에게 이 한 줄을 확인받는다(아래 §7 질문 1).

### 5-3. 시트2 "실패 사유"
권장 컬럼(요청서 §요구사항 2): `엑셀 행번호`, `사용자ID`, `실패 사유(message)`, `에러코드(errorCode)`.
- `엑셀 행번호` = `index + 4` (0-based index → 양식 4행부터 데이터: 1행 안내/2행 헤더/3행 예시).
- `사용자ID` = `errorItem`
- `실패 사유` = `message`
- `에러코드` = `errorCode`

### 5-4. 구현 스케치 (XLSX는 이미 import됨 — `aoa_to_sheet` + `book_append_sheet` 기존 사용)
```js
const fnExportExcel = () => {
  const list = props.dataList || [];
  const hasSourceRow = list.some(
    (r) => Array.isArray(r.sourceRow) && r.sourceRow.length > 0
  );

  if (!hasSourceRow) {
    // 기존 단일 시트 경로(그리드 저장 등) — 절대 변경 금지(회귀 방지)
    const summary = `요청 ${props.totalCount}건 중 ${props.successCount}건 성공 / ${props.failCount}건 실패`;
    const header = [props.identifierLabel, "비고"];
    const rows = list.map((r) => [r.errorItem ?? "", r.message ?? ""]);
    const wsData = [[summary], [], ["상세항목"], header, ...rows];
    const ws = XLSX.utils.aoa_to_sheet(wsData);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "처리결과");
    XLSX.writeFile(wb, `처리결과_${new Date().toISOString().slice(0, 10)}.xlsx`);
    return;
  }

  // prafta-052 — 엑셀 업로드 실패분: 재업로드용 2시트
  // [중요] EXCEL_HEADERS 는 백엔드 UserExcelRowParser.HEADERS 와 순서·문자열 동일 유지
  const EXCEL_HEADERS = [ /* 16컬럼 — 5-2 참조 */ ];
  const NOTICE = "4행부터 데이터가 저장됩니다. 헤더 행은 수정/삭제하지 마세요. 셀 수정 후 이 시트를 그대로 업로드하세요.";
  const EXAMPLE_PLACEHOLDER = new Array(EXCEL_HEADERS.length).fill(""); // 3행 예시(파서 skip 대상)

  // 시트1 "실패 항목" — 양식과 동일 구조(파서 DATA_START_ROW_INDEX=3 정합)
  const sheet1Rows = list.map((r) =>
    Array.isArray(r.sourceRow) ? r.sourceRow : []
  );
  const ws1Data = [
    [NOTICE],
    EXCEL_HEADERS,
    EXAMPLE_PLACEHOLDER,
    ...sheet1Rows,
  ];
  const ws1 = XLSX.utils.aoa_to_sheet(ws1Data);

  // 시트2 "실패 사유"
  const ws2Header = ["엑셀 행번호", "사용자ID", "실패 사유", "에러코드"];
  const ws2Rows = list.map((r) => [
    (r.index ?? 0) + 4,
    r.errorItem ?? "",
    r.message ?? "",
    r.errorCode ?? "",
  ]);
  const ws2 = XLSX.utils.aoa_to_sheet([ws2Header, ...ws2Rows]);

  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws1, "실패 항목");
  XLSX.utils.book_append_sheet(wb, ws2, "실패 사유");
  XLSX.writeFile(wb, `사용자업로드_실패항목_${new Date().toISOString().slice(0, 10)}.xlsx`);
};
```
> 스타일/CSS 변수 변경 없음(이 작업은 export 함수만 수정, 템플릿/스타일 무변경). 하드코딩 색상 추가 금지.

### 산출물
- `BatchResultPop.vue`(수정)

---

## 6. PII / security 점검 항목 (security 에이전트 전달)

원본 행에는 **휴대폰(`mblNo`), 이메일(`email`), 생년월일(`birthDt`) 등 평문 입력값**이 포함된다. 노출 범위가 다음 지점으로 확대되므로 security가 점검한다.

| # | 점검 지점 | 내용 |
|---|----------|------|
| S1 | 응답 DTO 확대 | `UserUpdateFailItem.sourceRow`가 `UserBatchUpdateResponse`(동기)·`UserUploadJobStatusResponse`(비동기) 응답 body에 평문 PII를 싣는다. 권한 가드(`isManager`)가 세 경로 모두에 이미 있는지 재확인(있음: insertUserBatch/uploadUserCreates/startUpload/getStatus 전부 `AuthRoleUtils.isManager`). |
| S2 | 비동기 IDOR | `getStatus`는 본인 잡만 조회(`job.userCd()` 비교, USER_404_002). 원본 행 PII가 타인에게 노출되지 않는지 — 기존 IDOR 가드가 sourceRow 추가 후에도 유효한지 확인. |
| S3 | `failsJson` 평문 저장 | 비동기 경로는 PII가 **DB `failsJson` 텍스트에 평문으로 영속화**된다(기존엔 userId+message만, 이제 휴대폰/이메일/생년월일 추가). 저장 PII 확대가 정책상 허용 범위인지 security 판단. (AES-GCM 미적용 평문 저장 지점 — 보존 기간/잡 삭제 정책 점검 필요.) |
| S4 | 로그 노출 | `serializeFails` 실패/`parseFails` 실패 시 로그에 JSON 전체를 찍지 않는지(현재 코드는 jobId만 로깅 — 유지). developer가 디버깅 로그에 `sourceRow`/`failsJson` 본문을 추가하지 않도록. |
| S5 | 프론트 노출 | 2시트 엑셀이 클라이언트에서 생성되어 평문 PII가 파일로 떨어진다. 이는 관리자(master/hr)만 도달하는 경로이며 기존 양식 다운로드와 동급 노출. 신규 위험 아님이나 명시. |

---

## 7. QA 검증 포인트 (요청서 §검증 포인트 원문 녹임 — QA 강조)

> 사용자 명시 요청: `BatchResultPop` **다른 호출 경로 회귀 없음**을 디테일하게 검증.

| # | 검증 포인트 | 기대 |
|---|------------|------|
| **V1** | 그리드 체크저장 실패(경로 a) | `sourceRow == null` → **기존 단일 시트(`사용자ID + 비고`)** 로 정상 출력. 시트명 "처리결과", 구조(summary/빈행/상세항목/헤더/데이터) 완전 동일. **회귀 0.** |
| **V2** | 동기/비동기 업로드 실패(경로 b,c) | 2시트 생성. 시트1 헤더가 **양식 16컬럼과 1:1**(순서·문자열). 시트1 구조가 파서 정합(1행 안내/2행 헤더/3행 예시/4행~ 데이터) → **무수정 재업로드 가능**. 컬럼 누락 0. |
| **V3** | 행번호 환산 | 시트2 `엑셀 행번호 = index + 4`. (index 0 → 4행, index 1 → 5행 …) |
| **V4** | 부분 성공(partial) | 성공 행 제외, **실패 행만** 시트1/시트2에 노출. successCount/failCount 합 일치. |
| **V5** | 비동기 라운드트립 | `failsJson` 직렬화 → 역직렬화 후 `sourceRow` 16값 유실 없음. 폴링 응답 `fails[].sourceRow`가 그대로 도달. |
| **V6** | 중복 사용자ID 혼동 방지 | 동일 엑셀에 같은 사용자ID 2행(첫 행 성공 / 둘째 행 USER_400_041) → 시트1/시트2의 **행 표기(index+4)가 둘째 행을 정확히 가리키는지**(성공한 첫 행과 혼동 없음). |
| **V7** | 구버전 호환 | `sourceRow` 키 없는 기존 `failsJson` 역직렬화 시 예외 없이 `sourceRow=null` 처리(02/03 진행 중이던 잡 호환). |
| **V8** | 헤더 단일출처 drift | 프론트 `EXCEL_HEADERS` ↔ 백엔드 `UserExcelRowParser.HEADERS` 문자열·순서 일치(코드 대조). 향후 양식 변경 시 양쪽 동시 수정 필요함을 리스크로 보고. |

> **QA 추가 지시(스펙 도전)**: 시트1을 옵션 A(양식 구조)로 만들었을 때 "다운로드 → 무수정 업로드"가 실제 파서(`UserExcelRowParser.parse`)를 통과하는지 **왕복 시나리오**로 검증. 3행 예시 placeholder가 빈 행이면 파서 `isEmptyRow`가 시트 종료로 오판하지 않는지 확인(파서는 `DATA_START_ROW_INDEX=3`부터 읽으므로 3행=index 2는 애초에 skip 범위 → 안전하나, 4행 이후 실패 데이터가 정상 파싱되는지 확인).

---

## 8. 모호함 / 사용자 확인 필요 (planner → 사용자)

작업을 Notion 등록하기 전 다음 1건만 확인이 필요하다(추측 금지 항목).

**[질문 1] 시트1 구조** — 요청서 §요구사항 1은 "양식과 동일한 16컬럼 헤더 + 실패 행 원본값"이며 "셀만 수정해서 그대로 재업로드"를 명시한다. 무수정 재업로드가 파서(`DATA_START_ROW_INDEX=3`)와 정합하려면 시트1을 **양식 동일 구조(1행 안내 / 2행 헤더 / 3행 예시 placeholder / 4행~ 실패 데이터)** 로 생성해야 한다(§5-2 옵션 A). 이 구조로 확정해도 되는지? (단순 헤더+데이터 2행 구조로 하면 재업로드 전 양식 복붙이 필요해 요청 취지에 어긋남.)

그 외는 요청서·코드로 모두 결정 가능하여 추가 질문 없음.
