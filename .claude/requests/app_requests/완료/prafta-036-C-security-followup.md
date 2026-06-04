# prafta-036-C — 앱 chkLst01·risk01 보안 follow-up (High 3건)

> 발주 배경: `.claude/requests/prafta-036-B1-chkLst01-refactor.md` 및 `prafta-036-B2-risk01-refactor.md` security 검토에서 발견된 기존 보존 위험 3건의 분리 처리.
> 사용자 확정 결정 (2026-05-28):
> - **Q1 = (a)** `siteCd`는 `tokenInfo.gv_siteCd()`로 강제 캐노니컬라이즈 (request siteCd 무시).
> - **Q2 = (a)** controller 클래스 레벨 `@NoAuth` 제거 (AuthAspect는 메서드 레벨만 인식 → 어차피 실효 없음, misleading dead code 정리).

---

## 0. 작성 목적 / 성격

- 본 문서는 **개발 명령(작업지시서)** 이며 developer 에이전트가 실행한다.
- 대상은 두 모듈 동시: `com.prafta.app.chkLst.chkLst01`, `com.prafta.app.risk.risk01`.
- prafta-036 B-1/B-2 가 "As-Is 보존 리팩터"로 통과시킨 기존 위험을 본 PR이 일괄 해소한다.
- **FE 무변경**. 백엔드만 손댐.

---

## 1. 해소 대상 위험 (security 검토 출처)

### H-1 — `@NoAuth` 클래스 레벨 misleading dead code
- **현상**: `AppChkLst01Controller.java:41` + `AppRisk01Controller.java:40` 에 클래스 레벨 `@NoAuth`. AuthAspect 의 pointcut `@annotation(NoAuth)` 는 메서드 레벨만 인식 → 실제 JWT 면제는 발생하지 않음(현재도 보호 중). 그러나:
  - 의도와 실효가 불일치 (코드/주석은 "면제 유지"라고 기술)
  - 누군가 메서드로 옮기는 순간 익명 노출
- **목표 행동**: 두 controller 의 **클래스 레벨 `@NoAuth` 제거**. `import com.prafta.common.annotation.NoAuth;` 가 다른 메서드에서도 미사용이면 제거. javadoc 의 "별도 보안 검토" 관련 잔재 주석도 정정 ("AuthAspect JWT 검증 정상 적용").
- **회귀 위험**: 없음 — 현재도 AuthAspect 가 JWT 검증 중. 동작 무변경.

### H-2 — Cross-site IDOR (siteCd request 신뢰)
- **현상**: 두 모듈 모두 `Param.from()` 에서 `request.getSiteCd()` 를 그대로 채택하여 mapper 까지 흘림. 동일 회사 내 권한 없는 사업장 코드를 임의 전달 시 조회/등록 가능.
- **목표 행동**: `Param.from()` 에서 `siteCd = tokenInfo.gv_siteCd()` 로 캐노니컬라이즈. request 의 `siteCd` 필드는 **여전히 받되 무시**(FE 호환). 단, request siteCd 가 token siteCd 와 다르면 `log.warn` 1회 (정보 출력 시 PII 없음 — siteCd 만, userCd 만).
- **영향 영역**:
  - chkLst01:
    - `application/param/ChecklistInfoParam.java:from()`
    - `application/param/InspectResultSaveParam.java:from()`
    - service 의 `FileService.fileSave(...)` 호출 시 `param.siteCd()` 가 이제 token 출처가 되어 H-3 도 자동 해소
  - risk01:
    - `application/param/RiskTypeInfoParam.java:from()`
    - `application/param/RiskAssessmentSaveParam.java:from()`
- **회귀 위험**: FE 가 sessionStorage 의 `gv_siteCd` 만 보내고 있어 request.siteCd ≡ token.siteCd 동일. 동작 무변경. 다중 사업장 사용자는 본 정책상 미지원(향후 도입 시 옵션 B `TB_USER_SITE_AUTH` 검증으로 별도 작업).

### H-3 — FileService 디렉토리 경로 공격 가능
- **현상**: `AppChkLst01ServiceImpl` / `AppRisk01ServiceImpl` 에서 `FileInfoParam.from(... param.siteCd() ...)` 호출. siteCd 가 request 출처라 다른 사업장 디렉토리에 파일 쓰기 가능.
- **목표 행동**: H-2 해소로 `param.siteCd()` 가 token 출처가 되므로 **자동 해소**. 별도 코드 변경 없음. service 호출부에 주석 1줄로 명시 권장 (`// param.siteCd() 는 Param.from 에서 token 캐노니컬라이즈됨`).
- **추가 권장(선택, 본 PR 범위 외)**: `FileServiceImpl.fileSave` 의 `param.cmpnyCd()`/`param.siteCd()` 화이트리스트 정규식 검증 — `common/cmm/file` 영역 별도 작업.

---

## 2. 변경 대상 파일

### chkLst01 (3~4개)
1. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/controller/AppChkLst01Controller.java`
   - 클래스 레벨 `@NoAuth` 제거 + 미사용 import 정리 + 관련 javadoc 정정
2. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/application/param/ChecklistInfoParam.java`
   - `from()` 에서 `siteCd = tokenInfo.gv_siteCd()` 로 채움. request.siteCd 와 다르면 `log.warn` 1회. token siteCd 가 비어 있으면 400.
3. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/chkLst/chkLst01/application/param/InspectResultSaveParam.java`
   - 동일 패턴 적용.
4. (필요 시) `AppChkLst01ServiceImpl.java` — 코드 변경 없음 예상. `FileInfoParam.from(...)` 호출부에 주석 1줄.

### risk01 (3~4개)
1. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/risk/risk01/controller/AppRisk01Controller.java`
   - 클래스 레벨 `@NoAuth` 제거 + 미사용 import 정리 + 관련 javadoc 정정
2. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/risk/risk01/application/param/RiskTypeInfoParam.java`
   - `from()` 에서 siteCd 캐노니컬라이즈 + warn 로그
3. `PRAFTA/prafta-backend/src/main/java/com/prafta/app/risk/risk01/application/param/RiskAssessmentSaveParam.java`
   - 동일
4. (필요 시) `AppRisk01ServiceImpl.java` — 코드 변경 없음 예상. 주석 1줄.

### 변경하지 않는 영역
- mapper xml — `#{param.siteCd}` 그대로 유지 (param.siteCd 가 이제 token 출처)
- request DTO — 필드 유지 (FE 호환)
- response DTO, result, query, command — 무관
- FE — 무변경

---

## 3. 코드 작성 가이드

### 3.1 Param.from() 패턴 (예: ChecklistInfoParam)

```java
private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ChecklistInfoParam.class);

public static ChecklistInfoParam from(ChecklistInfoRequest request, TokenInfo tokenInfo) {
    if (request == null) throw new ApiException(CommonErrorCode.COMMON_400_001);
    if (tokenInfo == null) throw new ApiException(CommonErrorCode.COMMON_400_003);

    String tokenSiteCd = tokenInfo.gv_siteCd();
    if (!StringUtils.hasText(tokenSiteCd)) {
        throw new ApiException(CommonErrorCode.COMMON_400_003);
    }
    // 클라이언트가 다른 siteCd 를 보낸 경우 경고만(action: 토큰 값으로 강제)
    String reqSiteCd = request.getSiteCd();
    if (StringUtils.hasText(reqSiteCd) && !tokenSiteCd.equals(reqSiteCd)) {
        log.warn("[chkLst01] siteCd 캐노니컬라이즈: 요청={}, 토큰={} → 토큰값 사용 (userCd={})",
                reqSiteCd, tokenSiteCd, tokenInfo.gv_userCd());
    }

    // 이후 chkptCd 등 기존 검증 + record 생성 (siteCd 는 tokenSiteCd 사용)
    return new ChecklistInfoParam(
        tokenSiteCd          // ← token 출처
        , request.getChkptCd()
        , /* ... 기존 필드 ... */
    );
}
```

- `record` 가 `private static final` 필드를 보유할 수 있으므로 logger 직접 선언 가능.
- 동일 패턴을 4개 Param 에 모두 적용.
- 기존 검증 로직(chkptCd null/empty 400 등)은 보존.

### 3.2 Controller 정리

```java
// 제거 전
@NoAuth   // ← 제거
@RestController
@RequestMapping("/chkLst01")
public class AppChkLst01Controller {
```

```java
// 제거 후
@RestController
@RequestMapping("/chkLst01")
public class AppChkLst01Controller {
```

- `import com.prafta.common.annotation.NoAuth;` 가 다른 곳에서 안 쓰이면 제거.
- 클래스 javadoc 에 "별도 보안 검토(D-R11) 분리" 같은 잔재 문구 있으면 "AuthAspect JWT 검증 정상 적용" 으로 정정.

### 3.3 ServiceImpl 주석 (선택)

```java
// param.siteCd() 는 Param.from 에서 token gv_siteCd 로 캐노니컬라이즈됨
fileService.fileSave(FileInfoParam.from(
    tokenInfo.gv_cmpnyCd()
    , tokenInfo.gv_userCd()
    , param.siteCd()  // ← token 캐노니컬라이즈됨
    , FILE_TYPE_DAILY_INSPECT
    , fileMgmtCd
    , img
));
```

코드 변경 0이며 주석만 추가.

---

## 4. 검증

1. **컴파일**: `./gradlew.bat compileJava --no-daemon -q` 통과 (타임아웃 300초). PowerShell 권장.
2. **mapper SQL 무변경 확인**: `git diff` 로 mapper xml 변경 0건 확인.
3. **응답 키 무변경 확인**: response/result/query/command record 변경 0건 확인.
4. **시뮬레이션 추적**: token gv_siteCd = "S001", request.siteCd = "S002" 시나리오 머릿속 추적 — service/mapper 가 "S001" 만 사용하는지 확인.
5. **FE 무변경 확인**: ChkLst.vue, Risk_01.vue 의 axios 호출 형태는 변경 없이 그대로 동작해야 함. FE 코드 직접 수정 0건.
6. **단위 동작**: param.from() 단위 검증 코드는 본 PR 에선 작성하지 않음 (cycle 비용 큼). 컴파일 + 명세 점검으로 갈음.

---

## 5. 보고 양식

developer 보고 시:
1. 변경 파일 목록 (예상 6~8개)
2. 빌드 결과
3. mapper xml / response / result 무변경 검증 결과
4. siteCd 캐노니컬라이즈 로직 검증 (token 강제 시뮬레이션 결과)
5. 잔여 follow-up (있다면)

---

## 6. 정책서 출처

- 본 작업은 인증/인가 강화이므로 `.claude/context/policies/common/08-permissions.md` 또는 `03-account-auth.md` 와 정합. (정책서에 명시되지 않은 추측 룰은 적용 금지 — security 검토에서 발견된 행동 보존 정책만 명세에 반영.)
- 변경 사항이 정책 충돌 의심되면 즉시 보고.

---

## 7. 미해결 / 후속 항목

- **prafta-036-A Medium**: `user-ids`·`user-id-duple-checks` POST→GET 전환으로 PII URL 노출. 본 PR 범위 외 (백엔드 controller 변경 + FE 동시 변경 필요). 별도 follow-up.
- **prafta-036-A Low**: `update-user-password` SMS 인증 토큰 미검증. 정책서 `common/03-account-auth.md` 확인 후 별도 작업.
- **FileServiceImpl 화이트리스트 정규식**: H-3 추가 방어. `common/cmm/file` 영역 별도 작업.
- **다중 사업장 사용자 지원**: 본 PR 은 단일 사업장 세션 가정. 향후 도입 시 옵션 B(`TB_USER_SITE_AUTH` 검증) 별도 작업.

---

**최종 업데이트**: 2026-05-28 — 분해 마스터 메시지로 본 명세 발주.
