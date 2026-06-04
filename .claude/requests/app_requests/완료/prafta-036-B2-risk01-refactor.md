# prafta-036-B2 — app.risk.risk01 풀 리팩터 (B-1 패턴 적용)

> 원본 요청서: `.claude/requests/prafta-036.md`
> 사용자 확정사항(2026-05-28) Q2 = **B-원안**: chkLst01 → risk01 순서로 풀 리팩터. 한 모듈씩 검증 사이클.
> 선행 작업: `prafta-036-B1-chkLst01-refactor.md` 완료 + Flutter 회귀 OK
> 본 단계 = `com.prafta.app.risk.risk01` 패키지 전체를 B-1과 동일 컨벤션으로 재구성 + risk01 고유 결함 동시 수정.

---

## 0. 작성 목적 / 성격

- 이 문서는 **개발 명령(작업지시서)** 이며 developer 에이전트가 실행한다.
- B-1과 동일한 To-Be 구조/플로우를 적용한다. 본 문서는 risk01 고유 정찰 결과 + B-1과 다른 차이점만 별도 명시.
- B-1의 §2(목표 패키지 구조), §4(multipart 처리 보존), §5(record 위치매핑 사전점검), §6(신규 로직 추가 금지) 원칙은 모두 동일하게 적용 — 본 문서에서 항목별로 재기술하지 않고 **"B-1 §X 패턴 동일"** 로 참조.

---

## 1. 배경 — 현재 구조 정찰 결과

### 1.1 현재 패키지 구조

`prafta-backend/src/main/java/com/prafta/app/risk/risk01/`:
```
controller/AppRisk01Controller.java
service/AppRisk01Service.java
service/impl/AppRisk01ServiceImpl.java
mapper/AppRisk01Mapper.java
dto/RiskInfoReq.java               (요청 DTO — Lombok @Value @Builder)
dto/RiskInfoRes.java               (응답 DTO — Lombok @Value @Builder(toBuilder=true))
dto/RiskInfoQry.java               (조회 파라미터 — Lombok @Value @Builder)
dto/RiskAssessmentReq.java         (multipart 요청 — Lombok @Data)
dto/RiskAssessmentSave.java        (저장 파라미터 — Lombok @Value @Builder)
vo/RiskCategory.java               (Lombok @Value @Builder)
vo/RiskHazard.java                 (Lombok @Value @Builder)
vo/RiskType.java                   (Lombok @Value @Builder — ⚠ 필드명 오타)
```
매퍼 XML: `prafta-backend/src/main/resources/com/prafta/app/risk/risk01/mapper/AppRisk01Mapper.xml`

### 1.2 현재 엔드포인트 (변경 금지)

| URL | 메서드 | 요청 | 호출처(앱 FE) |
|---|---|---|---|
| `/risk01/risk-type-infos` | GET | `@ModelAttribute RiskInfoReq` | `Risk_01.vue` L570 `axios.get('/appApi/risk01/risk-type-infos', { params: { siteCd }})` |
| `/risk01/save-risk-assessments` | POST multipart | `@ModelAttribute RiskAssessmentReq` + `@RequestPart(value="item") MultipartFile file` | `Risk_01.vue` L698 `axios.post('/appApi/risk01/save-risk-assessments', formData)` |

URL/메서드는 **이미 케밥-케이스** 이므로 그대로 유지.

### 1.3 현재 응답 키 (보존 대상)

- `risk-type-infos` 응답: `{ riskCategoryList: [...], riskTypeList: [...], riskHazardList: [...] }`
  - `Risk_01.vue` L576-580: `data.riskCategoryList`, `data.riskTypeList`, `data.riskHazardList` 사용
  - **3개 키 유지 필수**
- 각 항목 필드명:
  - `RiskCategory`: `baimValDCd, baimValDNm, sortIdx` → **FE 사용 키 확인 필요(grep)**
  - `RiskType`: `ProcessCd`(⚠ 대문자 시작 오타), `riskTypeCd`, `riskTypeNm` → **FE 사용 키 확인 필요**
  - `RiskHazard`: `riskTypeCd, hazardCd, hazardNm` → **FE 사용 키 확인 필요**
- `save-risk-assessments` 응답: 200 OK 본문 빈값. 변경 없음.

### 1.4 발견된 결함 (리팩터 시 동시 수정 대상)

| # | 위치 | 결함 | 수정 방향 |
|---|---|---|---|
| D-R1 | `vo/RiskType.java` | `String ProcessCd;` — 첫 글자 대문자(Lombok 생성자/getter도 `getProcessCd` / 직렬화 시 JSON 키 `ProcessCd`로 나감) | 신규 `RiskTypeResult` record에서 `processCd`로 정정. **단, FE 호환 확인 필수**: `Risk_01.vue` 가 `processCd` 또는 `ProcessCd` 어느 쪽을 destructure 하는지 grep |
| D-R2 | `AppRisk01ServiceImpl.selectRiskTypeInfo` L48-72 | `RiskInfoRes resDto = null;` 후 `if(riskCategoryList != null) { resDto = builder() }` 진행. 이어서 `if(riskTypeList != null) { resDto = resDto.toBuilder() }` — riskCategory가 NULL이면 resDto가 NULL인 채로 toBuilder 호출 → NPE | 단일 빌더 패턴으로 정리: `RiskInfoRes.builder().riskCategoryList(c).riskTypeList(t).riskHazardList(h).build()` — null 체크 불필요(빈 리스트면 그냥 빈 리스트로 응답) |
| D-R3 | `AppRisk01ServiceImpl.saveRiskAssessments` L78-112 | `catch (Exception e) { throw new ApiException(COMMON_500_001); }` — 원인 예외 e가 ApiException 생성자에 전달 안 됨. 로그도 없음 | `log.error("[risk01] saveRiskAssessments 실패", e)` + `throw new ApiException(CommonErrorCode.COMMON_500_001, e)` (CommonErrorCode 생성자가 cause 받는지 확인 후) |
| D-R4 | `AppRisk01ServiceImpl` | `@Transactional` 없음. 파일 저장 + DB 저장 부분커밋 가능 | `@Transactional(rollbackFor = Exception.class)` |
| D-R5 | `AppRisk01Mapper.java` L8-10 | `import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoQry; import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoSave; import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;` — chkLst01 메서드(`selectChkLstInfo`, `mergeChkptInspectAnswer`) 잘못 복붙 | risk01 Mapper interface에서 두 메서드 선언 제거 |
| D-R6 | `AppRisk01Mapper.xml` L159-218 | 동일 SQL 두 개(`selectChkLstInfo`, `mergeChkptInspectAnswer`) 복붙 | XML에서 두 select/insert 블록 삭제. 네임스페이스가 달라 런타임 충돌은 없으나 위생 문제 + 향후 사고 위험 |
| D-R7 | `AppRisk01Controller.java` L70-95 | 주석 처리된 chkLst01 메서드 잔재 | 정리 시 삭제 |
| D-R8 | `AppRisk01ServiceImpl.java` L116-213 | 주석 처리된 chkLst01 구현 잔재 | 정리 시 삭제 |
| D-R9 | `RiskInfoRes` import | `import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;` — 미사용 import | 삭제 |
| D-R10 | `RiskAssessmentSave` 의 `initRiskLv` | SQL에서 `IFNULL(#{param.initLikelihoodScore}, 1) * IFNULL(#{param.initSeverityScore}, 1)` 로 계산하면서 `param.initRiskLv` 는 파라미터로만 받고 컬럼에 안 들어감 (주석처리됨) | 현재 동작 보존. 단, record로 전환 시 컴포넌트 유지 (제거하면 컨트롤러 시그니처가 깨질 수 있음 — 본 단계는 신규 동작 도입 금지) |
| D-R11 | `controller` | `@NoAuth` 클래스 레벨. 결함 D-4와 동일 — 보존 | 후속 별도 |
| D-R12 | mapper XML `selectRiskCategory/Type/Hazard` 컬럼 별칭 누락 | `SELECT BAIM_VAL_D_CD, BAIM_VAL_D_NM, SORT_IDX` 등 별칭 없이 SELECT. resultType이 `vo.RiskCategory`(camelCase 필드)와 매핑되려면 MyBatis underscore-to-camelCase 활성이 필요. 현재 동작한다면 활성된 것 → record 전환 시 컴포넌트명을 동일 camelCase로 유지하면 그대로 매핑됨 | **§5 사전점검에서 mybatis-config의 mapUnderscoreToCamelCase 설정 확인 후 기록** |

---

## 2. 목표 패키지 구조 (To-Be)

B-1 §2 패턴 동일. risk01 특이사항만 명시:

```
com.prafta.app.risk.risk01/
├── controller/
│   └── AppRisk01Controller.java
├── application/
│   ├── command/
│   │   └── RiskAssessmentSaveCommand.java       (저장 명령 — record)
│   ├── param/
│   │   ├── RiskTypeInfoParam.java               (request → service — record, from(request, tokenInfo))
│   │   └── RiskAssessmentSaveParam.java         (multipart request + file → service — record, from(req, file, tokenInfo))
│   └── query/
│       └── RiskTypeInfoQuery.java               (param → mapper — record)
├── dto/
│   ├── request/
│   │   ├── RiskTypeInfoRequest.java             (Lombok @Getter @Setter @NoArgsConstructor)
│   │   └── RiskAssessmentRequest.java           (Lombok @Data — multipart fields)
│   └── response/
│       └── RiskTypeInfoResponse.java            (응답 키 보존: `riskCategoryList`, `riskTypeList`, `riskHazardList`)
├── mapper/
│   └── AppRisk01Mapper.java                     (chkLst01 메서드 제거)
├── result/
│   ├── RiskCategoryResult.java                  (record — 컬럼 매핑 명시)
│   ├── RiskTypeResult.java                      (record — ⚠ processCd 정정)
│   └── RiskHazardResult.java                    (record)
└── service/
    ├── AppRisk01Service.java
    └── impl/
        └── AppRisk01ServiceImpl.java
└── (vo/ 디렉토리는 제거)
```

매퍼 XML: `prafta-backend/src/main/resources/com/prafta/app/risk/risk01/mapper/AppRisk01Mapper.xml` (chkLst01 SQL 블록 삭제)

### 2.1 변경 전/후 클래스 매핑 표

| 현재 (As-Is) | 변경 후 (To-Be) | 비고 |
|---|---|---|
| `dto/RiskInfoReq.java` | `dto/request/RiskTypeInfoRequest.java` (`@Getter @Setter @NoArgsConstructor`) | `@ModelAttribute` 바인딩 |
| `dto/RiskInfoRes.java` | `dto/response/RiskTypeInfoResponse.java` (`@Builder @Getter`) | 응답 키 3개 보존, toBuilder 제거(단일 빌더 패턴) |
| `dto/RiskInfoQry.java` | `application/query/RiskTypeInfoQuery.java` (record) | from(Param) |
| `dto/RiskAssessmentReq.java` | `dto/request/RiskAssessmentRequest.java` (`@Data`) | multipart binding 유지 |
| `dto/RiskAssessmentSave.java` | `application/command/RiskAssessmentSaveCommand.java` (record) | from(Param, fileMgmtCd) |
| `vo/RiskCategory.java` | `result/RiskCategoryResult.java` (record) | 컴포넌트: `baimValDCd, baimValDNm, sortIdx` |
| `vo/RiskType.java` | `result/RiskTypeResult.java` (record) | ⚠ 컴포넌트: `processCd`(소문자 시작 정정), `riskTypeCd, riskTypeNm` |
| `vo/RiskHazard.java` | `result/RiskHazardResult.java` (record) | 컴포넌트: `riskTypeCd, hazardCd, hazardNm` |
| (없음) | `application/param/RiskTypeInfoParam.java` (record) | from(Request, TokenInfo) |
| (없음) | `application/param/RiskAssessmentSaveParam.java` (record) | from(Request, MultipartFile, TokenInfo) |

### 2.2 응답 키 보존 확인

`RiskTypeInfoResponse`:
```java
@Builder
@Getter
public class RiskTypeInfoResponse {
    private List<RiskCategoryResult> riskCategoryList;  // 키 보존
    private List<RiskTypeResult> riskTypeList;          // 키 보존
    private List<RiskHazardResult> riskHazardList;      // 키 보존
}
```
3개 필드명을 정확히 그대로 유지. 변경 시 FE 깨짐.

### 2.3 multipart 컨트롤러 시그니처

`save-risk-assessments`는 chkLst01과 다르게 **단일 파일**(`@RequestPart MultipartFile file`)을 받는다. `@RequestParam Map<String,MultipartFile>`이 아님.

```java
@PostMapping(value = "/save-risk-assessments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> saveRiskAssessments(
        @ModelAttribute RiskAssessmentRequest request,
        @RequestPart(value = "item", required = false) MultipartFile file,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    appRisk01Service.saveRiskAssessments(
        RiskAssessmentSaveParam.from(request, file, tokenInfo)
    );
    return ResponseEntity.status(HttpStatus.OK).build();
}
```

- `@RequestPart(value = "item")` 의 키 이름 `item` 유지 (FE `formData.append('item', ...)` 와 일치, `Risk_01.vue` L695)
- `required = false` 유지 (사진 없는 경우도 허용)

---

## 3. 작업 항목 분해 (developer 착수 단위)

| # | 항목 | 산출 |
|---|---|---|
| B2-1 | 디렉토리 신설 | `application/{command,param,query}`, `dto/{request,response}`, `result` |
| B2-2 | Result record 3종 작성 | `RiskCategoryResult`, `RiskTypeResult`(processCd 정정), `RiskHazardResult` — §5 검증 선행 |
| B2-3 | Query / Param / Command record 작성 | static factory 포함 |
| B2-4 | Request / Response DTO 작성 | 응답 키 3종 보존 |
| B2-5 | Mapper interface 정렬 | **chkLst01 메서드 2종(`selectChkLstInfo`, `mergeChkptInspectAnswer`) 제거**. 시그니처를 Query/Command record로 교체 |
| B2-6 | Mapper XML 정렬 | parameterType/resultType 경로 변경. **chkLst01 SQL 블록 2종(L159-218) 삭제**. risk01 SELECT 컬럼에 별칭 추가 권장(가독성, §5.4 참조) |
| B2-7 | Service 정렬 | D-R2(NPE) 단일 빌더로, D-R3(원인 보존+로그) 적용, D-R4(@Transactional) 추가 |
| B2-8 | Controller 정렬 | D-R7 주석 잔재 삭제. URL/메서드 변경 금지 |
| B2-9 | 구버전 클래스 삭제 | `dto/RiskInfoReq.java`, `dto/RiskInfoRes.java`, `dto/RiskInfoQry.java`, `dto/RiskAssessmentReq.java`, `dto/RiskAssessmentSave.java`, `vo/RiskCategory.java`, `vo/RiskHazard.java`, `vo/RiskType.java` (모두 삭제) |
| B2-10 | chkLst01 잔재 정리 | B-1 §7.4에서 후순위로 미룬 경우, B-2 완료 후 chkLst01 구 vo/dto 클래스 일괄 삭제 |
| B2-11 | 컴파일 검증 | `./gradlew.bat compileJava --no-daemon -q` (timeout 300s) |
| B2-12 | Vue 호출부 점검 | `views/risk/Risk_01.vue` L570-585 destructure 키가 응답 키와 일치 확인 + RiskCategory/Type/Hazard 항목 필드명 사용 패턴 grep |
| B2-13 | MCP 결과셋 컬럼 검증 | `selectRiskCategory/Type/Hazard` SELECT 실제 실행 컬럼 메타 확인 |

---

## 4. multipart 처리 보존

B-1 §4 패턴 동일. 차이점:
- 본 엔드포인트는 **단일 파일** (`@RequestPart MultipartFile file`) — chkLst01과 다름
- `@RequestPart(value = "item")` 의 키 "item" 보존
- items JSON 파싱이 **없음**(chkLst01과 다름). 단순히 단일 파일 + form 필드들만 받음
- `FileService.fileSave(FileInfoParam.from(...))` 호출 시그니처 보존 (기존 호출 그대로)

---

## 5. Record 위치매핑 사전점검 체크리스트

> ⚠ B-1 §5 사례 인용. risk01도 동일하게 사전 점검.

### 5.1 SELECT 컬럼 vs record 컴포넌트 (사전 일치 확인)

**`selectRiskCategory`** SELECT 컬럼:

| # | SELECT 컬럼 | 별칭 | 신규 record 컴포넌트 | 비고 |
|---|---|---|---|---|
| 1 | `BAIM_VAL_D_CD` | (없음) | `baimValDCd` | mapUnderscoreToCamelCase 활성 가정 |
| 2 | `BAIM_VAL_D_NM` | (없음) | `baimValDNm` | |
| 3 | `SORT_IDX` | (없음) | `sortIdx` (?) | **MCP로 DESCRIBE TB_BAIM_VAL_D 후 타입 결정** |

→ 컬럼 3개, record 컴포넌트 3개. 이름 매칭 OK.

**`selectRiskType`** SELECT 컬럼:

| # | SELECT 컬럼 | 별칭 | 신규 record 컴포넌트 | 비고 |
|---|---|---|---|---|
| 1 | `PROCESS_CD` | (없음) | `processCd` | ⚠ 기존 vo는 `ProcessCd` (대문자) → record 정정 |
| 2 | `RISK_TYPE_CD` | (없음) | `riskTypeCd` | |
| 3 | `RISK_TYPE_NM` | (없음) | `riskTypeNm` | |

→ 3:3. **단, D-R1 정정으로 인해 직렬화 JSON 키가 `ProcessCd` → `processCd`로 변경됨. FE 호환 확인 필수.**

**`selectRiskHazard`** SELECT 컬럼:

| # | SELECT 컬럼 | 별칭 | 신규 record 컴포넌트 |
|---|---|---|---|
| 1 | `RISK_TYPE_CD` | (없음) | `riskTypeCd` |
| 2 | `HAZARD_CD` | (없음) | `hazardCd` |
| 3 | `HAZARD_NM` | (없음) | `hazardNm` |

→ 3:3.

### 5.2 D-R1 (RiskType.ProcessCd → processCd) FE 호환 확인 (필수)

developer 착수 시 다음 grep 실행:
```bash
grep -rn "ProcessCd\|processCd" PRAFTA/prafta-app-frontend/prafta-app-frontend/src/views/risk/
grep -rn "ProcessCd\|processCd" PRAFTA/prafta-web-frontend/prafta-web-frontend/src/views/risk/
```

판정:
- **FE가 `processCd`(소문자) 사용**: 그대로 record 정정. 현재 백엔드가 `ProcessCd`로 직렬화하고 있다면 **현재 FE는 `processCd` 접근 시 undefined**가 나오고 있을 것 → 본 리팩터가 동시에 버그 수정 효과
- **FE가 `ProcessCd`(대문자) 사용**: D-R1 정정 시 FE 깨짐. 옵션:
  - (a) record를 `processCd`로 두되 `@JsonProperty("ProcessCd")` 추가하여 응답 키만 보존 (Lombok record 호환 확인 필요. Java 17+에서는 record 컴포넌트에 어노테이션 가능)
  - (b) FE 동시 정렬 (별도 보고 후 작업 확장 승인 필요)
- **FE가 아예 안 씀**: 정정 자유

발견 결과를 본 명령서 처리 시 사용자에게 보고하고, FE 깨짐 위험이 있으면 (a) 응답 키 보존 옵션 선택.

### 5.3 mybatis-config underscore-to-camelCase 확인

developer 착수 시 다음 확인:
```bash
grep -rn "mapUnderscoreToCamelCase" PRAFTA/prafta-backend/src/main/resources/
grep -rn "map-underscore-to-camel-case" PRAFTA/prafta-backend/src/main/resources/
```

- 활성(`true`)이면 SELECT 별칭 없이도 record 컴포넌트 camelCase 매핑 정상
- 비활성이면 SELECT에 명시적 AS 별칭 추가 필수 (예: `BAIM_VAL_D_CD AS baimValDCd`)
- 현재 코드가 동작한다는 사실로부터 활성 추정 — 단 검증 후 §5.1 표를 확정

### 5.4 SELECT 별칭 보강 권장

가독성 + 향후 사고 방지 차원에서 SELECT에 명시적 별칭 추가 권장 (mybatis 설정과 무관하게 안전):
```sql
SELECT
    BAIM_VAL_D_CD   AS baimValDCd
  , BAIM_VAL_D_NM   AS baimValDNm
  , SORT_IDX        AS sortIdx
FROM TB_BAIM_VAL_D
...
```
(단, SQL 본문 변경이므로 회귀 위험 발생 → 보강을 옵션으로 두고, 활성 확인되면 기존 SQL 그대로 둬도 무방)

### 5.5 INSERT 계산식 보존

`mergeRiskAssessment` 의 `IFNULL(#{param.initLikelihoodScore}, 1) * IFNULL(#{param.initSeverityScore}, 1)` 계산식은 INIT_RISK_LV 컬럼에 자동 저장. 본 SQL 본문은 **변경하지 않음**. parameterType 패키지 경로만 record로 교체.

---

## 6. 신규 화면/로직 추가 금지

B-1 §6 동일. 본 단계는 순수 리팩터 + 결함 D-R1~D-R10 동시 수정만.

---

## 7. 검증 체크리스트

### 7.1 Claude (developer) 자체 검증

- [ ] `./gradlew.bat compileJava --no-daemon -q` 통과 (timeout 300s)
- [ ] 신규 디렉토리 구조가 §2 와 일치
- [ ] 구버전 클래스 8종 삭제 완료
- [ ] Controller URL/메서드 변경 없음
- [ ] Mapper interface에서 chkLst01 메서드 2종 제거
- [ ] Mapper XML에서 chkLst01 SQL 블록 2종 제거
- [ ] Mapper XML 본문(risk01 SQL) 변경 없음 (parameterType/resultType 경로만 변경)
- [ ] Response 키 3종(`riskCategoryList`, `riskTypeList`, `riskHazardList`) 보존
- [ ] §5.1 표의 record 컴포넌트 이름 일치
- [ ] D-R1 (ProcessCd → processCd) 정정 + FE 호환 확인 결과 보고
- [ ] D-R2 (NPE 위험) 단일 빌더로 해소
- [ ] D-R3 (원인 보존) `log.error + ApiException(cause)` 적용
- [ ] D-R4 (@Transactional) 추가
- [ ] D-R7, D-R8 주석 잔재 삭제
- [ ] D-R9 미사용 import 삭제
- [ ] MCP MySQL로 3개 SELECT 결과셋 컬럼 1회 검증
- [ ] B-1에서 chkLst01 구 vo/dto가 deprecate 상태였다면 B-2 완료 시점에 일괄 삭제

### 7.2 사용자 위임 검증 (Flutter 재빌드 후)

- [ ] 위험성평가 신규 등록 화면 진입 시 구분/분류/발생상황 드롭다운 정상 표시
- [ ] 구분 선택 → 분류 필터링 → 발생상황 필터링 정상 (FE 로직이 `processCd` 또는 `ProcessCd` 키로 분류하는지에 따라 D-R1 정정 영향)
- [ ] 평가 점수 입력 + 사진 첨부 + 저장 정상
- [ ] 저장 후 DB의 `INIT_RISK_LV` 컬럼이 likelihood × severity로 채워졌는지 확인
- [ ] 의도된 오류(점수 누락 등)에서 silent swallow가 아닌 명확한 에러 surface

### 7.3 회귀 위험 항목

- [ ] B-1 완료 후 chkLst01 모듈 정상 동작 확인된 상태에서 B-2 착수
- [ ] B-2 작업 중 chkLst01 모듈에 의도치 않은 영향 없음 (compileJava로 확인)
- [ ] `RiskInfoRes` 의 toBuilder 제거로 인한 호출처 영향: 동일 모듈 내부에서만 사용 → 영향 없음
- [ ] grep으로 `com.prafta.app.risk.risk01.dto`, `com.prafta.app.risk.risk01.vo` 의 외부 사용 확인

---

## 8. 위험 / 주의

| 항목 | 내용 |
|---|---|
| **응답 키 변경 사고** | `riskCategoryList/riskTypeList/riskHazardList` 3종 키 보존 필수. Response 작성 직후 `Risk_01.vue` L576-580과 1:1 대조 |
| **D-R1 ProcessCd 정정 위험** | §5.2 grep 결과에 따라 FE 호환 옵션 (a)/(b) 선택. **사용자에게 결과 보고 후 진행 권장** |
| **chkLst01 SQL 잔재 삭제** | XML 블록 삭제 시 line range 정확히. mapper namespace 라인은 보존 |
| **mapper interface 메서드 삭제** | `selectChkLstInfo`, `mergeChkptInspectAnswer` 두 메서드만 제거. 다른 risk01 메서드는 보존 |
| **B-1 결과물 회귀** | B-1에서 risk01 mapper의 chkLst01 의존을 미리 정리한 경우 B-2에서 중복 작업 없음 확인 |
| **단일 파일 multipart** | chkLst01과 시그니처 다름. `@RequestPart` + 단일 `MultipartFile` 유지 (Map 아님) |
| **`@NoAuth` 클래스 레벨 유지** | 현행 보존 |
| **git 브랜치 분리** | 권장: `refactor/prafta-036-b2-risk01`. B-1 머지 완료 + 회귀 OK 후 본 브랜치 분기 |
| **MCP read-only 제약** | DDL 변경 없음. DESCRIBE / SHOW / SELECT 만 사용 |

---

## 9. 비즈니스 정책서 출처

본 단계도 **순수 리팩터**(URL/메서드/비즈니스 룰 변경 없음, 단 D-R1 정정으로 JSON 키 1개 변경 가능성).

참고만:
- 위험성평가 도메인 자체 정책 — `.claude/context/policies/` 내 명시적 정책서 부재
- 감사 로그 (silent swallow 제거로 가시성 회복) → `.claude/context/policies/common/11-security-privacy.md` §11.3

> **해당 정책 부재**: 본 단계는 정책서 출처 매핑이 직접적이지 않음. 기술 정책서(`developer.md`)의 DTO 플로우 규약과 `CLAUDE.md`의 MyBatis 규약을 따른다.

---

## 10. 산출물

- 수정 파일:
  - `prafta-backend/src/main/java/com/prafta/app/risk/risk01/controller/AppRisk01Controller.java`
  - `prafta-backend/src/main/java/com/prafta/app/risk/risk01/service/AppRisk01Service.java`
  - `prafta-backend/src/main/java/com/prafta/app/risk/risk01/service/impl/AppRisk01ServiceImpl.java`
  - `prafta-backend/src/main/java/com/prafta/app/risk/risk01/mapper/AppRisk01Mapper.java`
  - `prafta-backend/src/main/resources/com/prafta/app/risk/risk01/mapper/AppRisk01Mapper.xml`
- 신규 파일:
  - `application/command/RiskAssessmentSaveCommand.java`
  - `application/param/RiskTypeInfoParam.java`
  - `application/param/RiskAssessmentSaveParam.java`
  - `application/query/RiskTypeInfoQuery.java`
  - `dto/request/RiskTypeInfoRequest.java`
  - `dto/request/RiskAssessmentRequest.java`
  - `dto/response/RiskTypeInfoResponse.java`
  - `result/RiskCategoryResult.java`
  - `result/RiskTypeResult.java`
  - `result/RiskHazardResult.java`
- 삭제 파일:
  - `dto/RiskInfoReq.java`
  - `dto/RiskInfoRes.java`
  - `dto/RiskInfoQry.java`
  - `dto/RiskAssessmentReq.java`
  - `dto/RiskAssessmentSave.java`
  - `vo/RiskCategory.java`
  - `vo/RiskHazard.java`
  - `vo/RiskType.java`
  - (디렉토리) `vo/`
- (B-1 후순위였다면 동시 삭제) chkLst01 구 vo/dto 6종
- DDL/마이그레이션 없음
- 앱 FE 변경: 응답 키 보존되면 0건. D-R1 정정 결과에 따라 `Risk_01.vue` 일부 destructure 변경 가능성 (별도 보고 후 진행)

---

## 11. 후속 단계

- **D-R11 `@NoAuth` 검토**: security 에이전트 별도 요청
- **chkLst01 모듈 추가 결함**(예: `@NoAuth` 의도 검토): 별도 요청서
- **app 모듈 잔여 화면**(`project_prafta_app_vite_and_api_align` 메모리): 별도 요청서로 분해

---

**최종 업데이트**: 2026-05-28 — prafta-036 Q2(B-원안) 분해 결과 2/2.
