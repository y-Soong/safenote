# prafta-036-B1 — app.chkLst.chkLst01 풀 리팩터 (웹 컨벤션 적용)

> 원본 요청서: `.claude/requests/prafta-036.md`
> 사용자 확정사항(2026-05-28) Q2 = **B-원안**: app.chkLst.chkLst01 → app.risk.risk01 순서로 풀 리팩터. 한 모듈씩 검증 사이클.
> 본 단계 = `com.prafta.app.chkLst.chkLst01` 패키지 전체를 웹 모듈(`com.prafta.web.tbm.tbm01` 등)과 동일 컨벤션으로 재구성.
> 위험 완화 핵심: **응답 필드 키 보존**(앱 FE 호출부 무변경), **multipart 처리 보존**, **record 위치매핑 사고 사전점검**(Tbm04Mapper 사례).

---

## 0. 작성 목적 / 성격

- 이 문서는 **개발 명령(작업지시서)** 이며 developer 에이전트가 실행한다.
- 본 단계는 **앱 백엔드 패키지 1개(chkLst01) + Vue 호출부 1개(`views/chkLst/ChkLst.vue`)** 만 손댄다.
- 앱 FE 호출부 변경은 **응답 키가 보존되면 0건**, 변경되면 destructure 라인 1-2개. 핵심은 응답 키 유지.
- Flutter 재빌드는 사용자 위임. 본 명령서는 백엔드 + Vue 호출 정렬까지만.

---

## 1. 배경 — 현재 구조 정찰 결과

### 1.1 현재 패키지 구조

`prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/`:
```
controller/AppChkLst01Controller.java
service/AppChkLst01Service.java
service/impl/AppChkLst01ServiceImpl.java
mapper/AppChkLst01Mapper.java
dto/ChecklistInfoReq.java          (요청 DTO — Lombok @Value @Builder)
dto/ChecklistInfoRes.java          (응답 DTO — Lombok @Value @Builder)
dto/ChecklistInfoQry.java          (조회 파라미터 — Lombok @Value @Builder)
dto/ChecklistInfoSave.java         (저장 파라미터 — Lombok @Value @Builder)
dto/SaveInspectResultReq.java      (multipart 요청 — Lombok @Data, MultipartFile 포함)
vo/ChecklistInfo.java              (결과 VO — Lombok @Value @Builder)
```
매퍼 XML: `prafta-backend/src/main/resources/com/prafta/app/chkLst/chkLst01/mapper/AppChkLst01Mapper.xml`

### 1.2 현재 엔드포인트 (변경 금지)

| URL | 메서드 | 요청 | 호출처(앱 FE) |
|---|---|---|---|
| `/chkLst01/checklist-infos` | GET | `@ModelAttribute ChecklistInfoReq` | `ChkLst.vue` L272 `axios.get('/appApi/chkLst01/checklist-infos', { params: { siteCd, chkptCd }})` |
| `/chkLst01/save-inspect-result` | POST multipart | `@ModelAttribute SaveInspectResultReq` + `@RequestParam Map<String, MultipartFile> file` | `ChkLst.vue` L213 `axios.post('/appApi/chkLst01/save-inspect-result', formData)` |

URL/메서드는 **이미 케밥-케이스** 이므로 그대로 유지. 본 리팩터는 **클래스 구조와 DTO 플로우만** 손댄다.

### 1.3 현재 응답 키 (앱 FE 호출부가 의존하는 것 — 보존 대상)

- `checklist-infos` 응답: `{ checklistInfos: [ { cmpnyCd, chklstType, chkptDesc, inspectItemCd, inspectItemSubj, inspectValue, sortIdx } ] }`
  - 앱 FE 호출부(`ChkLst.vue` L278): `Array.isArray(res.data) ? res.data : res.data?.checklistInfos || []` 로 양쪽 모두 허용. **`checklistInfos` 키 유지 필수**.
  - 앱 FE는 각 항목에서 `x.inspectItemCd`, `x.inspectItemSubj`, `x.inspectValue` 만 사용 → **이 3개 필드명 변경 금지**.
- `save-inspect-result` 응답: 200 OK 본문 빈값. 변경 없음.

### 1.4 발견된 결함 (리팩터 시 동시 수정 대상)

리팩터 작업 중 함께 정리. 별도 사고 방지.

| # | 위치 | 결함 | 수정 방향 |
|---|---|---|---|
| D-1 | `AppChkLst01ServiceImpl.saveInspectResult` L130-132 | `catch (Exception e) {}` — silent swallow. 실패 시에도 200 OK 반환되어 클라가 성공으로 오인 | catch에서 `ApiException(CommonErrorCode.COMMON_500_001)` throw + `log.error("[chkLst01] saveInspectResult 실패", e)` |
| D-2 | `AppChkLst01ServiceImpl` | `@Transactional` 없음. 파일 저장 N건 중 일부 실패 시 부분 커밋 발생 가능 | `@Transactional(rollbackFor = Exception.class)` 추가 |
| D-3 | `AppChkLst01Mapper.xml` `selectChkLstInfo` | `WHERE A.CMPNY_CD = #{token.gv_cmpnyCd}` 정상이나 `B.CMPNY_CD` 조인에 `A.CMPNY_CD = B.CMPNY_CD` 만 있고 `B.CMPNY_CD = #{token.gv_cmpnyCd}` 명시 없음 (조인으로 전파됨, 명시 안 함도 OK) | 유지 (방어적으로 추가도 가능, 우선순위 낮음) |
| D-4 | `controller` | `@NoAuth` 가 클래스 레벨에 붙어 있음. 점검 결과 저장은 사용자 식별이 필요한 액션 → NoAuth가 의도된 것인지 검토 필요 | 우선 유지(현행 동작 보존). 추후 security 검토 위임. 본 리팩터에서는 변경 금지 |
| D-5 | `vo/ChecklistInfo.java` | `String sortIdx` — DB 컬럼이 INT일 가능성. 현재 매핑 `B.SORT_IDX AS sortIdx` resultType=ChecklistInfo는 MyBatis underscore-to-camelCase가 아닌 별칭 매핑 사용 중. 작동은 함. 단, record 전환 시 타입 일치 확인 필요(MCP로 컬럼 타입 확인 권장) | 리팩터 시 MCP로 `DESCRIBE TB_CHKPT_INSPECT_ITEM`로 SORT_IDX 타입 확인 후 record 필드 타입 결정 |
| D-6 | `dto/ChecklistInfoSave.java`에 `userCd` 없음 | INSERT_NO에 token.gv_userCd 사용으로 무관 (mapper에서 직접 token 참조) | 유지 |

---

## 2. 목표 패키지 구조 (To-Be)

웹 컨벤션(`com.prafta.web.tbm.tbm01` 등)과 동일하게:
```
com.prafta.app.chkLst.chkLst01/
├── controller/
│   └── AppChkLst01Controller.java
├── application/
│   ├── command/
│   │   └── InspectResultSaveCommand.java       (저장 명령 — record)
│   ├── model/
│   │   └── InspectAnswerItemModel.java         (multipart items 파싱 결과 — record)
│   ├── param/
│   │   ├── ChecklistInfoParam.java             (request → service 진입 — record, from(request) static factory)
│   │   └── InspectResultSaveParam.java         (multipart request → service — record, from(req, files, tokenInfo) static factory)
│   └── query/
│       └── ChecklistInfoQuery.java             (param → mapper — record)
├── dto/
│   ├── request/
│   │   ├── ChecklistInfoRequest.java           (Lombok @Getter @Setter @NoArgsConstructor — @ModelAttribute 바인딩용)
│   │   └── SaveInspectResultRequest.java       (Lombok @Data — multipart fields + MultipartFile items)
│   └── response/
│       └── ChecklistInfoResponse.java          (Lombok @Builder @Getter — 응답 key 보존: `checklistInfos`)
├── mapper/
│   └── AppChkLst01Mapper.java                  (record param + TokenInfo)
├── result/
│   └── ChecklistInfoResult.java                (mapper SELECT 결과 — record, 컬럼명 매핑 명시)
├── service/
│   ├── AppChkLst01Service.java
│   └── impl/
│       └── AppChkLst01ServiceImpl.java
└── (vo/ 디렉토리는 제거)
```

매퍼 XML 경로(변경 없음): `prafta-backend/src/main/resources/com/prafta/app/chkLst/chkLst01/mapper/AppChkLst01Mapper.xml`

### 2.1 DTO 플로우 (웹 컨벤션)

```
Controller(@ModelAttribute Request)
    ↓ Param.from(request, tokenInfo)
Service(Param)
    ↓ Query.from(param) 또는 직접 매핑
Mapper(Query) → Result (record)
    ↓
Response.builder().checklistInfos(results).build()
    ↓
Controller가 ResponseEntity로 반환
```

Multipart 흐름(`save-inspect-result`):
```
Controller(@ModelAttribute Request + Map<String, MultipartFile> files)
    ↓ Param.from(request, files, tokenInfo)  ←  multipart 처리 보존
Service(Param)
    ↓ items MultipartFile 파싱 → List<InspectAnswerItemModel>
    ↓ files Map 정규식 매칭 → Map<itemCd, MultipartFile>
    ↓ each item에 대해:
       - FileService.fileSave (기존 호출 그대로)
       - Command 빌드 → Mapper.mergeChkptInspectAnswer(Command, TokenInfo)
```

### 2.2 변경 전/후 클래스 매핑 표

| 현재 (As-Is) | 변경 후 (To-Be) | 비고 |
|---|---|---|
| `dto/ChecklistInfoReq.java` (`@Value @Builder`) | `dto/request/ChecklistInfoRequest.java` (`@Getter @Setter @NoArgsConstructor`) | `@ModelAttribute` 바인딩을 위해 setter 필요 → Lombok `@Value` → 표준 POJO로 전환 |
| `dto/ChecklistInfoRes.java` | `dto/response/ChecklistInfoResponse.java` | `checklistInfos` 키 보존. 타입을 `List<ChecklistInfoResult>` (record) 로 |
| `dto/ChecklistInfoQry.java` | `application/query/ChecklistInfoQuery.java` (record) | static factory `from(ChecklistInfoParam)` |
| `dto/ChecklistInfoSave.java` | `application/command/InspectResultSaveCommand.java` (record) | static factory `from(InspectResultSaveParam, FileInfoModel item, String fileMgmtCd)` |
| `dto/SaveInspectResultReq.java` (`@Data`) | `dto/request/SaveInspectResultRequest.java` (`@Data`) | multipart 바인딩이라 `@Data` 유지(MultipartFile field) |
| `vo/ChecklistInfo.java` (`@Value @Builder`) | `result/ChecklistInfoResult.java` (record) | SELECT 컬럼 ↔ record 컴포넌트 순서 **사전 일치 검증 필수**(§5) |
| (없음) | `application/param/ChecklistInfoParam.java` (record) | `from(ChecklistInfoRequest, TokenInfo)` — siteCd/chkptCd 등 |
| (없음) | `application/param/InspectResultSaveParam.java` (record) | `from(SaveInspectResultRequest, Map<String,MultipartFile>, TokenInfo)` |
| (없음 — 현재 `FileInfoModel` 재사용) | `application/model/InspectAnswerItemModel.java` (record) | items JSON 파싱 결과를 chkLst01 도메인 모델로 흡수. (현재 `FileInfoModel`을 차용하고 있는데 의미상 맞지 않음. 단, 변경 시 FE/JSON 키 호환 확인 필요) |

> 모델 클래스 신설(`InspectAnswerItemModel`) 판단: 현재 `FileInfoModel`을 차용하는 이유가 `itemCd, answerDesc, inspectValue, fileName` 필드만 쓰기 위함이라면, **별도 모델을 신설하지 않고 `FileInfoModel` 재사용 유지**도 가능. 의미상 분리하려면 신설하되 JSON 키 호환을 위해 동일 필드명 유지. **권장: 현재 사용하는 4-5개 필드명만 매칭하면 되므로 신설하여 책임 분리.** 단, **현재 잘 동작하는 부분(파일 키 매칭 정규식 등)은 그대로 옮긴다.**

---

## 3. 작업 항목 분해 (developer 착수 단위)

| # | 항목 | 산출 | 비고 |
|---|---|---|---|
| B1-1 | 디렉토리 신설 | `application/{command,model,param,query}`, `dto/{request,response}`, `result` 디렉토리 생성 | 빈 디렉토리 생성 |
| B1-2 | Result record 작성 | `result/ChecklistInfoResult.java` — SELECT 컬럼/타입 일치 검증 후 작성 | §5 검증 선행 |
| B1-3 | Query / Param / Command / Model record 작성 | `application/query/`, `application/param/`, `application/command/`, `application/model/` 각 클래스 | static factory 메서드 포함 |
| B1-4 | Request / Response DTO 작성 | `dto/request/ChecklistInfoRequest.java` (`@Getter @Setter @NoArgsConstructor`), `dto/request/SaveInspectResultRequest.java` (`@Data` + MultipartFile), `dto/response/ChecklistInfoResponse.java` (`@Builder @Getter`, 응답 키 `checklistInfos` 보존) | |
| B1-5 | Mapper 인터페이스/XML 정렬 | `AppChkLst01Mapper.java` 시그니처를 Query/Command record로 교체. XML resultType 패키지 경로 변경(`vo.ChecklistInfo` → `result.ChecklistInfoResult`). 매개변수 클래스 경로 변경 | XML 본문(SELECT/INSERT)은 변경 없음, parameterType/resultType만 변경 |
| B1-6 | Service 인터페이스/구현 정렬 | Param 받아서 Mapper 호출 + Response 빌드. D-1, D-2 결함 동시 수정 | `@Transactional`, `catch → ApiException` |
| B1-7 | Controller 정렬 | Request 받아서 `Param.from(request, tokenInfo)` 호출 후 Service 진입. Service 반환 → Response 빌드는 service 내부에서 처리 후 그대로 반환 | URL/메서드 변경 금지 |
| B1-8 | 구버전 클래스 삭제 | `dto/ChecklistInfoReq.java`, `dto/ChecklistInfoRes.java`, `dto/ChecklistInfoQry.java`, `dto/ChecklistInfoSave.java`, `dto/SaveInspectResultReq.java`, `vo/ChecklistInfo.java` 삭제 | 컴파일 통과 후 삭제 |
| B1-9 | 컴파일 검증 | `./gradlew.bat compileJava --no-daemon -q` (timeout 300s) | 통과까지 반복 |
| B1-10 | Vue 호출부 점검 | `views/chkLst/ChkLst.vue` L272, L278, L213 destructure 키가 응답 키와 일치하는지 확인 | 응답 키 보존되었으므로 변경 없음 예상. 변경 필요 시 본 명령서로 반환 보고 |
| B1-11 | MCP 결과셋 컬럼 검증 | MCP MySQL로 `selectChkLstInfo`의 실제 결과셋 컬럼 1회 조회 | §5 사전점검 외 사후검증 |

---

## 4. multipart 처리 보존 (필수 점검)

`save-inspect-result` 는 multipart/form-data 엔드포인트로, 다음 3가지 요소가 함께 도착한다:

1. **단순 필드** (`cmpnyCd`, `siteCd`, `userCd`, `chkptCd`, `workDate`) — `SaveInspectResultRequest` 의 일반 필드로 받음
2. **JSON 묶음 파일** (`items`) — `MultipartFile items` 필드. 서비스가 readTree로 파싱
3. **이미지 파일들** (`files[ITEM_CD]`) — `@RequestParam Map<String, MultipartFile> file` 로 받음. 정규식 `^files\[(.+)]$` 로 itemCd 추출

### 4.1 보존 규칙

- `@PostMapping(value = "/save-inspect-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` 유지
- `@ModelAttribute SaveInspectResultRequest request` 유지 (`@RequestBody`로 바꾸면 multipart 깨짐)
- `@RequestParam(required = false) Map<String, MultipartFile> file` 유지 — 변수명 `file` (싱귤러) 그대로. 변수명 변경 시 form 키 매핑이 안 맞아 hang/누락 가능
- 정규식 `Pattern.compile("^files\\[(.+)]$")` 유지
- `MultipartFile items` 의 `.getBytes()` → UTF-8 String → readTree → JsonNode 순회 패턴 유지
- 변환된 항목 모델(`InspectAnswerItemModel` 또는 `FileInfoModel` 재사용)의 **JSON 키 호환 보존**: 앱 FE가 보내는 JSON 필드명(`itemCd`, `answerDesc`, `inspectValue`, `fileName` 등)과 record 컴포넌트명 1:1 매칭. 변경 시 앱 FE도 동시 수정 필요(범위 외).

### 4.2 컨트롤러 시그니처 권장 형태

```java
@PostMapping(value = "/save-inspect-result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> saveInspectResult(
        @ModelAttribute SaveInspectResultRequest request,
        @RequestParam(required = false) Map<String, MultipartFile> file,
        @RequestHeader(value = "Authorization", required = false) String authorization) {

    TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
    appChkLst01Service.saveInspectResult(
        InspectResultSaveParam.from(request, file, tokenInfo)
    );
    return ResponseEntity.status(HttpStatus.OK).build();
}
```

Service 시그니처는 Param 1개로 정리(현재의 `(request, files, tokenInfo)` 3개 매개변수 → 1개).

---

## 5. Record 위치매핑 사전점검 체크리스트

> ⚠ **Tbm04Mapper IndexOutOfBoundsException 사고 사례**: result를 record로 도입할 때, MyBatis가 record 컴포넌트 **선언 순서**로 SELECT 컬럼을 매핑(별칭 매핑 활성화 안 된 경우 또는 SELECT 컬럼 수 ≠ record 컴포넌트 수)하여 런타임 예외 발생. 본 사고를 반복하지 않기 위한 사전점검.

### 5.1 SELECT 컬럼 수 vs record 컴포넌트 수 사전 일치 확인

**`selectChkLstInfo`** SELECT 컬럼(현재 매퍼 XML):

| # | SELECT 별칭 | 현재 vo 필드명 | 신규 record 컴포넌트(권장) | 비고 |
|---|---|---|---|---|
| 1 | `A.CMPNY_CD AS cmpnyCd` | `cmpnyCd` (String) | `cmpnyCd` (String) | |
| 2 | `A.CHKLST_TYPE AS chklstType` | `chklstType` (String) | `chklstType` (String) | |
| 3 | `A.CHKPT_DESC AS chkptDesc` | `chkptDesc` (String) | `chkptDesc` (String) | |
| 4 | `B.INSPECT_ITEM_CD AS inspectItemCd` | `inspectItemCd` (String) | `inspectItemCd` (String) | |
| 5 | `B.INSPECT_ITEM_SUBJ AS inspectItemSubj` | `inspectItemSubj` (String) | `inspectItemSubj` (String) | |
| 6 | `'Y' AS inspectValue` | `inspectValue` (String) | `inspectValue` (String) | 리터럴 |
| 7 | `B.SORT_IDX AS sortIdx` | `sortIdx` (String) | `sortIdx` (?) | **MCP로 DESCRIBE 후 타입 결정** |

→ 컬럼 7개, record 컴포넌트 7개. 순서 일치.

### 5.2 record 작성 직전 MCP 확인 명령 (developer가 실행)

```sql
DESCRIBE TB_CHKPT_TYPE_MGMT;
DESCRIBE TB_CHKPT_INSPECT_ITEM;
SHOW CREATE TABLE TB_CHKPT_INSPECT_ITEM;
DESCRIBE TB_CHKPT_INSPECT_ANSWER;
```
확인 항목:
- `B.SORT_IDX` 타입 (INT인지 VARCHAR인지) → record 필드 타입 결정
- `CMPNY_CD`, `SITE_CD`, `CHKLST_TYPE`, `CHKPT_CD`, `CHKPT_DESC`, `INSPECT_ITEM_CD`, `INSPECT_ITEM_SUBJ`, `INSPECT_ANSWER_TYPE`, `ANSWER_DESC`, `FILE_MGMT_CD`, `WORK_DATE` 모든 타입 확인

### 5.3 MyBatis 설정 확인 (선택)

- 프로젝트 `mybatis-config.xml` 또는 application.yml에서 `mapUnderscoreToCamelCase=true` 활성 여부 확인
- 활성이면 SELECT 별칭 없이 컬럼명으로도 매핑 가능
- 단, 본 SELECT는 이미 모든 컬럼에 명시적 별칭이 있으므로 별칭 매핑이 우선
- record 매핑 시 MyBatis는 **컴포넌트 이름으로 매칭**(컬럼 별칭 ↔ record 컴포넌트 이름). 위치 매칭이 아니므로 §5.1 표의 이름 일치만 보장하면 안전

### 5.4 SUM/AVG/COUNT 같은 집계 컬럼 사전점검

- 현재 `selectChkLstInfo`에 집계 없음 → 해당 없음
- 향후 추가 시: record primitive `int`/`long` 필드에 매핑되는 집계는 `NULL` 시 `IFNULL(SUM(...), 0)` 강제. NULL → primitive 박싱 실패로 NPE 발생

---

## 6. 신규 화면/로직 추가 금지

본 단계는 **순수 리팩터**. 다음은 모두 금지:
- 신규 엔드포인트 추가
- 비즈니스 로직 변경 (값 계산식, 분기, 검증 룰)
- DDL 추가/수정
- 코드 시드 추가
- 권한 정책 변경 (`@NoAuth` 유지/제거 모두 금지, 단 §1.4 D-4는 후속 별도)
- 응답 키 변경

§1.4의 D-1, D-2만 동시 수정. (오류 안전성 회복이며 동작 변화 없음. D-1은 silent → 실패 시 5xx, D-2는 부분커밋 → 전체 롤백.)

---

## 7. 검증 체크리스트 (Claude 자체 + 사용자 위임)

### 7.1 Claude (developer) 자체 검증

- [ ] `./gradlew.bat compileJava --no-daemon -q` 통과 (timeout 300s)
- [ ] 신규 디렉토리 구조가 §2 와 일치
- [ ] 구버전 클래스 6종 삭제 완료(`dto/ChecklistInfoReq/Res/Qry/Save.java`, `dto/SaveInspectResultReq.java`, `vo/ChecklistInfo.java`)
- [ ] Controller URL/메서드 변경 없음 (`/chkLst01/checklist-infos` GET, `/chkLst01/save-inspect-result` POST multipart)
- [ ] Mapper XML 본문 변경 없음 (parameterType/resultType 경로만 변경)
- [ ] Response 키 `checklistInfos` 유지 — `ChecklistInfoResponse` 의 필드명이 정확히 `checklistInfos`
- [ ] `ChecklistInfoResult` 의 7개 컴포넌트 이름이 §5.1 표와 정확히 일치
- [ ] `@Transactional(rollbackFor = Exception.class)` 추가 (`saveInspectResult`)
- [ ] `catch (Exception e)` 가 `log.error + throw new ApiException(COMMON_500_001, e)` 패턴으로 변경
- [ ] multipart 컨트롤러 시그니처에서 `@ModelAttribute`, `Map<String, MultipartFile> file`, 정규식 패턴 모두 보존
- [ ] MCP MySQL로 `selectChkLstInfo` SQL 실제 실행 (개발 DB) — 결과셋 컬럼 7개와 record 매칭 확인. (test row 없어도 NoOp 가능, 단 컬럼 메타데이터 확인)
- [ ] `views/chkLst/ChkLst.vue` 호출부 변경 필요 여부 점검 (응답 키 보존되었으면 변경 0)

### 7.2 사용자 위임 검증 (Flutter 재빌드 후)

- [ ] 점검 화면 진입 시 항목 목록이 정상 표시 (양호/불량 라디오 노출)
- [ ] 불량 선택 → 사진 첨부 → 저장 정상
- [ ] 동일 항목 재저장(UPSERT 동작) 정상
- [ ] 의도된 오류(권한 없음 등)에서 silent swallow가 아닌 명확한 에러 surface

### 7.3 회귀 위험 항목

- [ ] 다른 모듈(`com.prafta.app.risk.risk01.mapper.AppRisk01Mapper`)이 `vo/ChecklistInfo`를 import 하고 있음(B-2 작업의 정찰 결과). **B-1에서 vo 클래스 삭제 시 risk01 모듈 컴파일 깨짐 가능** → §7.4 처리 절차로 해소
- [ ] 다른 모듈이 chkLst01 의 구버전 DTO를 import 하고 있는지 grep: `grep -r "com.prafta.app.chkLst.chkLst01.dto.ChecklistInfo" prafta-backend/src/main/java/` — 발견 시 본 B-1에서 처리 불가능한 영향(B-2가 받음)

### 7.4 risk01 의존 처리

`com.prafta.app.risk.risk01.mapper.AppRisk01Mapper`가 chkLst01의 `vo.ChecklistInfo`, `dto.ChecklistInfoQry`, `dto.ChecklistInfoSave` 를 import하고 있다. 이는 risk01에 chkLst01 메서드(`selectChkLstInfo`, `mergeChkptInspectAnswer`)가 잘못 복붙되어 있는 결함이다.

**B-1 처리 절차:**
1. 우선 chkLst01의 구 vo/dto 클래스를 **삭제하지 않고 deprecate 주석만 추가** (`@Deprecated` + `// TODO: B-2 이후 삭제`)
2. risk01 Mapper의 `selectChkLstInfo`, `mergeChkptInspectAnswer` 메서드는 B-2에서 제거됨 (불필요한 복붙)
3. B-2 완료 후 본 B-1 후속 PR로 chkLst01 구 vo/dto 삭제

또는, **B-1 작업 범위 내에서** risk01 mapper interface에서 두 메서드 선언을 함께 제거 + risk01 mapper XML에서도 동일 SQL 두 개 제거(B-2와 일부 작업이 겹치지만 컴파일 깨짐을 방지하기 위한 최소 처리). developer가 판단해 둘 중 선택. **권장: 후자(B-1 내 처리)** — 단, 변경 내역 PR 설명에 명시.

---

## 8. 위험 / 주의

| 항목 | 내용 |
|---|---|
| **응답 키 변경 사고** | `checklistInfos` 키를 다른 이름으로 짓는 순간 앱 FE 깨짐. Response 작성 직후 `ChkLst.vue` L278과 키 1:1 대조 |
| **record 위치 매핑 사고 재발** | §5 사전점검 누락 시 NoSuchMethodException 또는 ArrayIndexOutOfBoundsException. SELECT 컬럼 별칭과 record 컴포넌트 이름을 한 화면에서 동시에 보면서 1:1 매칭 |
| **multipart 컨트롤러 어노테이션 실수** | `@ModelAttribute`를 `@RequestBody`로 바꾸면 multipart 깨짐. 반드시 `@ModelAttribute` 유지 |
| **MultipartFile 변수명** | `file` 단수형 그대로. axios FormData append 키와 어긋나면 매핑 누락 |
| **`@Transactional` 도입 시 메서드 가시성** | public 메서드여야 적용됨. 현재 `saveInspectResult`는 public이므로 OK |
| **risk01 의존 깨짐** | §7.4 처리 절차 따르지 않으면 컴파일 실패. `grep -r "ChecklistInfo" prafta-backend/src/main/java/com/prafta/app/risk/` 로 의존성 사전 확인 |
| **`@NoAuth` 클래스 레벨 유지** | 현행 보존. 변경 시 모바일 흐름이 인증 강제되어 동작 불가 가능성 |
| **git 브랜치 분리** | 권장: `refactor/prafta-036-b1-chkLst01`. B-2(`refactor/prafta-036-b2-risk01`)와 분리. 검증 사이클 분리 |
| **MCP read-only 제약** | 본 단계는 DDL 변경 없음. MCP는 DESCRIBE / SHOW / SELECT 만 사용. INSERT/ALTER 시도 금지 |

---

## 9. 비즈니스 정책서 출처

본 단계는 **순수 리팩터**(URL/메서드/응답/비즈니스 룰 변경 없음). 비즈니스 정책서에 새로운 매핑이 없다.

참고만:
- 안전 점검(체크리스트) 도메인 자체 정책 — `.claude/context/policies/` 내 명시적 정책서 부재 (chkLst 도메인은 PRAFTA 전용 화면 사양으로만 존재. 정책서 신설 시 별도 요청)
- PII/감사 로그 → `.claude/context/policies/common/11-security-privacy.md` §11.3 (작업 중 사용자 식별 변경 없음, 단 silent swallow 제거가 감사 로그 가시성을 회복)

> **해당 정책 부재**: 본 단계는 정책서 출처 매핑이 직접적이지 않음. 기술 정책서(`developer.md`)의 DTO 플로우 규약과 `CLAUDE.md`의 MyBatis 규약을 따른다.

---

## 10. 산출물

- 수정 파일:
  - `prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/controller/AppChkLst01Controller.java`
  - `prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/service/AppChkLst01Service.java`
  - `prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/service/impl/AppChkLst01ServiceImpl.java`
  - `prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/mapper/AppChkLst01Mapper.java`
  - `prafta-backend/src/main/resources/com/prafta/app/chkLst/chkLst01/mapper/AppChkLst01Mapper.xml` (parameterType/resultType 경로만)
- 신규 파일:
  - `application/command/InspectResultSaveCommand.java`
  - `application/model/InspectAnswerItemModel.java` (신설하는 경우)
  - `application/param/ChecklistInfoParam.java`
  - `application/param/InspectResultSaveParam.java`
  - `application/query/ChecklistInfoQuery.java`
  - `dto/request/ChecklistInfoRequest.java`
  - `dto/request/SaveInspectResultRequest.java`
  - `dto/response/ChecklistInfoResponse.java`
  - `result/ChecklistInfoResult.java`
- 삭제 파일:
  - `dto/ChecklistInfoReq.java`
  - `dto/ChecklistInfoRes.java`
  - `dto/ChecklistInfoQry.java`
  - `dto/ChecklistInfoSave.java`
  - `dto/SaveInspectResultReq.java`
  - `vo/ChecklistInfo.java`
  - (디렉토리) `vo/`
- DDL/마이그레이션 없음
- 앱 FE 변경: 없음 (응답 키 보존). 변경 필요 시 별도 보고

---

## 11. 후속 단계

- **B-2 (risk01 풀 리팩터)**: 본 B-1 완료 + Flutter 회귀 OK 후 착수. B-1과 동일 패턴 적용 + risk01 고유 결함(§7.4의 chkLst01 메서드 복붙 정리, RiskType 필드명 대문자 오타 `ProcessCd` → `processCd`, `RiskInfoRes` toBuilder NPE 등) 동시 수정
- **D-4 `@NoAuth` 검토**: security 에이전트가 별도 요청으로 처리

---

**최종 업데이트**: 2026-05-28 — prafta-036 Q2(B-원안) 분해 결과 1/2.
