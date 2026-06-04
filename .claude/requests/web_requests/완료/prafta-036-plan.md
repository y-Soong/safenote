# prafta-036 — User_01 신규 계정 단건 생성 + 엑셀 대량 업로드 : 작업 분해 계획

> 작성: planner 세션 (2026-05-28).
> 원본 요청서: `.claude/requests/web_requests/prafta-036.md` (User_01 화면에 신규 단건 생성/엑셀 대량 업로드 추가).
> 확정 결정 단일 출처: 본 문서 §1 (채팅 Q&A로 사용자 확정한 D1~D7). 원본 요청서는 본 문서로 **대체(supersede)** 된다 — 개발 착수 시 원본이 아니라 본 문서의 결정/분해를 따른다.
> 정책서 참조: 공통 §3.1·§3.2·§3.5(계정/인증/계정상태), §8(권한), §11(보안·PII).

---

## 0. 개요

- 사용자가 채팅 Q&A로 확정한 결정사항(D1~D7)을 그대로 반영해 작업으로 분해한다.
- 단일 계정 생성은 기존 `UserInfoPop`을 "조회(S)" 외에 "생성(C)" 모드로 확장하여 재사용한다. 별도 신규 팝업을 만들지 않는다(화면 일관성 — 정책서 §13.3).
- 엑셀 대량 업로드는 **양식 다운로드 → 작성 → 업로드 → 결과 표시**의 3단계 흐름. 양식 컬럼·결과 표시 등은 D4·D6 고정.
- "관리자가 만든 계정"의 휴대폰 본인인증 미완료 상태를 위해 **SYS013에 `04=인증대기` 코드 신설**(D2). 로그인 플로우는 인증대기 분기 라우팅 + 인증 완료 시 상태전이 로직만 추가 (로그인 화면 UI 전면 개편은 범위 밖).
- 비밀번호는 **휴대폰번호 11자리(하이픈 제외) BCrypt 해시**가 초기값(D3). 단건/엑셀 동일.

---

## 1. 사용자 확정 결정 (채팅 Q&A 2026-05-28)

| # | 결정 | 내용 |
|---|---|---|
| D1 | **단건 진입점** | User_01 ViewHeader에 "생성" 버튼 노출(`localButtons.create='Y'`), 기존 `UserInfoPop`을 `callmethod_p='C'` 모드로 재사용. 생성 모드에서 SMS 인증 UI/필드 숨김. 권한 드롭다운은 기존 `sortIdx >= authLevel` 필터 유지. |
| D2 | **계정 상태(인증대기)** | SYS013에 `04=인증대기` 신설. 관리자 생성 시 `ACCOUNT_STATUS='04'`로 INSERT. 로그인 플로우에 인증대기 분기 + 인증 완료 시 `'01'` 전이/`MBL_NO_HMAC` 갱신만 포함(로그인 화면 UI 자체 개편은 범위 밖). `UX_TB_USER_MBL_NO` UNIQUE 충돌 시 명확한 한글 에러. |
| D3 | **초기 비밀번호** | 휴대폰번호 11자리(하이픈 제외) BCrypt 해시. 단건·엑셀 공통. 첫 로그인 시 강제 변경은 범위 밖(follow-up). |
| D4 | **엑셀 양식 구조** | 1행 안내문(빨간 글씨 "4행부터 데이터가 저장됩니다."), 2행 한글 헤더, 3행 예시 데이터(파싱 skip), 4행~ 실제 데이터. 컬럼 = [필수] 사용자ID/이름/AUTH_CD/SITE_NO/NODE_CD + [선택] 휴대폰/이메일/성별/생년월일/직급/입사일/고용형태/계약종료일/경력인정개월/SYS042/경력상세. 1행=1사용자=1건 경력만 허용. 양식 다운로드는 `GET /webApi/user01/user-create-template` (xlsx 스트림), 업로드는 `POST /webApi/user01/upload-user-creates` (multipart). |
| D5 | **사업장/부서 입력 규칙** | 엑셀=SITE_NO 기준(서버에서 TB_SITE 조회로 SITE_CD 매핑). 엑셀=NODE_CD 직접 입력(TB_SITE_NODE 존재 검증). 단건=기존 SiteSearchPop/SiteNodeSearchPop 재사용. |
| D6 | **결과 표시** | 기존 `BatchResultPop` 재사용(`identifierLabel='사용자ID'`). 실패 사유 표준 한글 메시지: 사용자ID중복/휴대폰번호중복/사업장번호없음/부서코드없음/권한코드없음/필수값누락/고용형태오류/사유유형오류 등. |
| D7 | **권한·보안 가드** | `AuthRoleUtils.isManager` 필수. 생성하려는 사용자의 AUTH_CD `sortIdx` ≥ 요청자 `authLevel` 강제(서버측 이중 가드). `cmpnyCd`는 토큰 `gv_cmpnyCd` 강제(요청 body 무시). 엑셀 .xlsx만 허용, 최대 1000행/5MB. |

---

## 2. 초안에서 잘라낸 / 보류한 항목

| 초안/유사요구 | 처리 | 이유 |
|---|---|---|
| 회원가입 화면(`/login/insert-user-info`) 흐름 재사용 | **불채택** | 회원가입은 사용자 직접 가입(SMS 인증 통과 + 약관 동의 흐름)이라 관리자 일괄 생성과 책임 분리가 명확함. UserInfoPop 확장이 화면 일관성·재사용성 모두 우수. |
| 로그인 화면 자체 UI 전면 개편 | **범위 밖** | D2 결정대로 "인증대기 분기 라우팅 + 인증 완료 후 상태전이 로직"만 포함. 로그인 화면 시각적 변경/별도 인증대기 전용 화면 신설은 follow-up. |
| 첫 로그인 강제 비밀번호 변경 | **범위 밖** | 기존 `PWD_CHG_DTIME` 컬럼은 존재하나 강제 변경 흐름이 정의돼 있지 않음. 별도 follow-up으로 분리. |
| 모바일(앱) 첫 로그인 인증 흐름 | **범위 밖** | 본 작업은 웹 백엔드/웹 프론트만. 앱은 webview Vue 측 변경이 동일 백엔드 API를 사용하지만 앱 인증 UI는 별도 작업. |
| 단건 생성 시 SMS 본인인증 강제 | **불채택** | D1 결정: 관리자 생성 계정은 휴대폰 인증 미완료 상태로 만들고 첫 로그인 시 인증. 단건 생성 UI에서 SMS 입력란/버튼 숨김. |
| 회사 코드(`cmpnyCd`) 사용자 입력 | **불채택(보안)** | D7: 토큰 `gv_cmpnyCd`로만 결정(IDOR 방지). |
| `tb_user_site_auth` 자동 INSERT | **포함** | 기존 `User01ServiceImpl.updateOneUserInfo`에서 사이트 변경 시 INSERT/DELETE하는 패턴과 동일하게, 신규 INSERT 시에도 `tb_user_site_auth`에 `(cmpnyCd, userCd, siteCd, 'Y')` 한 줄을 함께 INSERT. |

---

## 3. 영향 범위

### 3.1 스키마 / 마이그레이션

| 작업 | 파일 | 변경 |
|---|---|---|
| SYS013에 `04=인증대기` 추가 | `prafta-backend/src/main/resources/sql/migration/prafta-036-sys013-pending.sql` (신규) | `INSERT INTO tb_syst_val_d (SYST_VAL_CD='SYS013', SYST_VAL_D_CD='04', SYST_VAL_D_NM='인증대기', SORT_IDX=4, USE_YN='Y', INSERT_NO='SYSTEM')`. 멱등성: 부재 확인 후 INSERT. |
| `tb_user` 컬럼 변경 | **없음** | 본 작업에서 신규 컬럼 추가 없음. 기존 `ACCOUNT_STATUS`/`MBL_NO_*`/`HIRE_DATE`/`EMPLOYMENT_TYPE`/`CONTRACT_END_DATE`/`RANK_CD` 등 모두 그대로 사용. |
| `tb_user_service_credit` 컬럼 변경 | **없음** | 엑셀에서 1행=1경력만 받으므로 기존 구조(`REASON_TYPE[SYS042]`, `REASON_DETAIL varchar(500)`, `CREDIT_MONTHS int`) 그대로 사용. |
| `UX_TB_USER_ID` / `UX_TB_USER_MBL_NO` UNIQUE 제약 | **그대로 활용** | 사용자ID·휴대폰HMAC 유니크 위반은 batch 결과의 실패 사유로 표준화(D6). |

### 3.2 백엔드

#### 신규/변경 파일 — 단건 생성

| 영역 | 파일 | 변경 |
|---|---|---|
| Controller | `web/user/user01/controller/User01Controller.java` | `POST /webApi/user01/insert-user-info` 신규 endpoint 추가(관리자 단건 생성용). 기존 `/login/insert-user-info`(NoAuth, 회원가입용)와 별개. |
| Request DTO | `web/user/user01/dto/request/UserCreateRequest.java` (신규) | Lombok `@Getter @Setter @NoArgsConstructor`. 필드: `userId, userNm, authCd, siteNo, nodeCd, mblNo, email, gender, birthDt, rankCd, hireDate, employmentType, contractEndDate, creditMonths, creditReasonType, creditReasonDetail`. |
| Param record | `web/user/user01/application/param/UserCreateParam.java` (신규) | `from(UserCreateRequest, claims)` — 토큰에서 `gvCmpnyCd/gvUserCd/gvAuthCd/gvAuthLevel` 흡수. |
| Service | `web/user/user01/service/User01Service.java`(인터페이스)·`User01ServiceImpl.java` | `void insertUserOne(UserCreateParam)` 추가. 권한 가드(`AuthRoleUtils.isManager` + `sortIdx >= gvAuthLevel`), siteNo→siteCd 매핑, NODE_CD 존재 검증, UserId/MBL_NO_HMAC 중복 사전 점검, BCrypt 해시(`PasswordHasher.hash(mblNo11)`), AES-GCM(`mblNo/email/birthDt`) + HMAC, `ACCOUNT_STATUS='04'`, `tb_user_site_auth` INSERT, 경력 1건 INSERT. 모든 단계 단일 트랜잭션. |
| Mapper | `web/user/user01/mapper/User01Mapper.java` + `.xml` | `insertOneUser` (TB_USER INSERT — 기존 `LoginMapper.insertUserInfo` 구조 참고하되 `ACCOUNT_STATUS='04'` 고정·`INSERT_NO=gvUserCd`), `selectSiteCdByNo(cmpnyCd, siteNo)`, `selectSiteNodeExists(cmpnyCd, siteCd, nodeCd)`, `selectUserIdExists(cmpnyCd, userId)`, `selectUserMblHmacExists(cmpnyCd, mblNoHmac)`, `insertUserSiteAuthOne` 신규. (기존 `selectUserExistCount`/`insertUserSiteAuth`(batch)는 그대로 두고 단건 전용으로 분리.) |
| 채번 | `LoginMapper.selectUserCd(cmpnyCd)` 재사용 | 기존 회원가입 채번 SQL(`CONCAT(DATE_FORMAT(NOW(),'%Y%m'), FNC_CMM_SEQ_NEXTVAL(cmpnyCd, 'USER_CD'))`). User01ServiceImpl이 LoginMapper에 의존하기 부담스러우면 동일 SQL을 User01Mapper에 복제. **developer 결정 포인트**. |
| 권한 검증 | `web/user/user01/service/impl/User01ServiceImpl.java` 내부 헬퍼 | 신규 생성 대상의 `AUTH_CD` 권한레벨을 `COM005`(권한 마스터)에서 조회해 `sortIdx >= gvAuthLevel` 검증. `Baim06Mapper` 또는 새 헬퍼 쿼리 1건(`selectAuthSortIdx`). |

#### 신규/변경 파일 — 엑셀 양식 다운로드 / 업로드

| 영역 | 파일 | 변경 |
|---|---|---|
| Controller | `web/user/user01/controller/User01Controller.java` | `GET /webApi/user01/user-create-template`(xlsx 스트림 다운로드, `Content-Disposition: attachment; filename="사용자생성양식.xlsx"`), `POST /webApi/user01/upload-user-creates`(`@RequestParam("file") MultipartFile file` + 토큰). |
| Service | `web/user/user01/service/User01Service.java`/`User01ServiceImpl.java` | `byte[] buildUserCreateTemplate(claims)` — 양식 .xlsx 바이트 생성(D4). `UserBatchUpdateResponse uploadUserCreates(MultipartFile, claims)` — 파일 검증(.xlsx, ≤5MB), 시트 파싱(1행 안내문/2행 헤더/3행 예시 skip/4행~), 행별 검증·생성, `UserBatchUpdateResponse` 반환. |
| Batch Service | `web/user/user01/service/User01BatchService.java`/`User01BatchServiceImpl.java` | `UserBatchUpdateResponse insertUserBatch(List<UserCreateParam>)` 신규. 기존 `updateUserInfoBatch` 패턴(REQUIRES_NEW 트랜잭션·실패 수집·continue) 그대로 따름. |
| Excel 의존성 | `prafta-backend/build.gradle` | Apache POI 신규 추가(현재 미사용). 권장: `org.apache.poi:poi-ooxml:5.2.5` (xlsx). **developer 결정 포인트** — 백엔드에 POI 추가가 부담스러우면 양식 생성도 프론트에서 SheetJS(`xlsx` 0.18.5)로 처리하고 업로드만 백엔드에서 POI 없이 처리(클라이언트가 JSON으로 보내는 방식). planner 권장: **POI 추가**(보안 가드 강화 + 서버 일관성). |
| 파일 검증 | `User01ServiceImpl` 내부 헬퍼 | 확장자 `.xlsx` 강제, 크기 ≤5MB, 데이터 행 ≤1000. 위반 시 `ApiException(CommonErrorCode.COMMON_400_002)` 또는 신규 에러코드. |

#### 신규/변경 파일 — 로그인 흐름 (인증대기 분기)

| 영역 | 파일 | 변경 |
|---|---|---|
| Login Service | `common/cmm/login/service/impl/LoginServiceImpl.java` `Login()` | 비밀번호 검증 성공 후, `userResult.accountStatus` 분기 추가: `'04'` 인증대기면 별도 응답(예: `LoginResponse.requirePhoneAuth=true` + 제한된 토큰 또는 임시 식별자). 토큰 발급 자체를 막을지/제한 토큰으로 발급할지 **developer 결정 포인트**. planner 권장: **임시 토큰(scope=PHONE_AUTH_ONLY) 발급 + 휴대폰 인증 endpoint만 호출 가능**(현행 JWT 구조와의 정합도 검토). |
| Login Response | `common/cmm/login/dto/response/LoginResponse.java` + `LoginResponse.from` | `accountStatus` 또는 `nextStep` 필드 추가. |
| User Result | `common/cmm/login/result/UserResult.java` | `accountStatus` 필드 노출 확인(이미 있으면 그대로). LoginMapper.Login SELECT 컬럼에 `ACCOUNT_STATUS` 포함 여부 확인. |
| Phone Verify Controller | `common/cmm/login/controller/LoginController.java` (또는 신규) | `POST /login/verify-phone-auth`(인증대기 사용자가 SMS 인증 통과 시 호출). 기존 `/comApi/baseinfo/sms-auth-checks`로 인증번호를 1차 검증한 뒤, 본 endpoint가 `ACCOUNT_STATUS='01'`로 전환 + `MBL_NO_HMAC`/`MBL_NO_ENC`/`MBL_NO_LAST4` 업데이트(필요 시 — 관리자가 입력한 번호와 인증한 번호 다를 경우만, 동일하면 변경 없음) + 본격 토큰 발급. **developer 결정 포인트**: 인증 endpoint의 권한 모델(현재 `@NoAuth` 사용 가능 + 임시 토큰 검증). |
| HMAC UNIQUE 충돌 처리 | `LoginServiceImpl.verifyPhoneAuth` | 인증한 휴대폰의 HMAC이 이미 다른 사용자(`ACCOUNT_STATUS='01'`)에게 사용 중이면 명확한 한글 에러(예: "이미 다른 계정에서 사용 중인 휴대폰번호입니다"). 본인이 동일 HMAC이면 OK. |

#### 변경하지 않는 파일

- 기존 `POST /webApi/user01/update-user-infos` 흐름(저장 일괄 수정): **무변경**.
- 기존 `GET /webApi/user01/user-info-lists`: **무변경**.
- 회원가입 `POST /login/insert-user-info`: **무변경**.

### 3.3 프론트엔드

#### 변경 파일 — User_01.vue (생성 진입점 + 엑셀 버튼)

| 영역 | 변경 |
|---|---|
| `localButtons` | `fnButtonControll()`에서 `localButtons.create = 'Y'` 활성화. (기존엔 미정의 → ViewHeader에 노출 없음). |
| ViewHeader 이벤트 | `@create="fnCreate"` 신규 핸들러. 신규 단건 생성용 `UserInfoPop` 호출(`callmethod_p='C'`). |
| 엑셀 영역 UI | 기존 `subtitle-row .custom-btn-area`에 버튼 2개 추가: "양식 다운로드", "엑셀 업로드"(파일 input 트리거). |
| `fnDownloadTemplate` | `axios.get('/webApi/user01/user-create-template', { responseType: 'blob' })` → Blob URL로 다운로드 트리거. |
| `fnUploadExcel` | 숨겨진 `<input type="file" accept=".xlsx" ref="excelFileRef">` + 버튼 클릭 → file 선택 → FormData로 `POST /webApi/user01/upload-user-creates` → 결과를 `BatchResultPop`(`identifierLabel='사용자ID'`)로 표시 후 `fnSearch()` 재호출. |
| 파일 input 사후 처리 | input value 초기화(`event.target.value = ''`)로 같은 파일 재선택 가능. |

#### 변경 파일 — popup/UserInfoPop.vue (생성 모드 추가)

| 영역 | 변경 |
|---|---|
| `props.callmethod_p` | 기존 `"S"`(조회) 외에 `"C"`(생성) 분기 추가. |
| `onMounted` | `callmethod_p === 'C'`면 `fnGetUserInfo` 호출 생략. `fnGetLeaveInfo`도 생략(신규는 leave-info가 없음, 입사일/경력은 폼 입력값으로 직접 INSERT). |
| 사용자ID 필드 | 생성 모드에서 `disabled` 해제, 입력 가능. (조회 모드는 기존대로 disabled.) |
| 사용자명 필드 | 생성 모드에서 `disabled` 해제. |
| SMS 인증 영역 | 생성 모드에서 휴대폰 입력은 받되 인증요청/인증번호 입력/타이머/`smsCertNoChk` 검사를 **모두 숨김/무력화** (D1). `mblNoDisabled=false`, `btnAuthChkDisabledVisible=false`, 검증 우회. |
| 권한 드롭다운 | 생성 모드에서도 기존 `sortIdx >= authLevel` 필터 적용(D1). |
| 입사일 / 경력 인정 섹션 | 생성 모드(`isHrOrMaster && callmethod === 'C'`)에서 빈 폼으로 입력 가능. 입사일 직접 input(YYYY-MM-DD), 경력 인정 0~1건만 허용. |
| 저장 버튼 동작 | `fnUserInfoSave` 내부 분기: 조회 모드는 기존 `/update-user-infos` 유지. 생성 모드는 `POST /webApi/user01/insert-user-info`로 분기 + 경력 1건 함께 전송(서버에서 1트랜잭션). |
| 회원탈퇴/탈퇴취소/비밀번호초기화 버튼 | 생성 모드에서 비표시(`v-if="callmethod_p !== 'C'"`). |
| 폼 validation | 생성 모드: 사용자ID(필수, 4~10자 영문/숫자), 사용자명(필수), 권한(필수), 사업장(필수), 부서(필수), 휴대폰(필수, 11자리), 생년월일(필수), 기타 선택. SMS 인증 검사는 우회. |

#### 신규 파일 — 없음

기존 `BatchResultPop`, `SiteSearchPop`, `SiteNodeSearchPop` 모두 재사용. 신규 팝업 추가 없음.

#### 변경하지 않는 파일 (확인 필요)

- 로그인 화면(`src/views/login/Login.vue` 등): D2의 "인증대기 분기 라우팅"은 **로그인 응답을 받은 axios 응답 핸들러**에서 처리. 응답에 `accountStatus='04'` 또는 `nextStep='PHONE_AUTH'`가 오면 휴대폰 인증 화면으로 라우팅. 기존 인증 라우팅 흐름이 있다면 그것을 재사용. **developer 결정 포인트** — 로그인 화면 정독 후 재사용 가능한 인증 화면이 없다면 follow-up.

### 3.4 SYS013 코드 표

본 작업 후 SYS013 최종 상태(코드 COMMENT에 반영):

| SYST_VAL_D_CD | SYST_VAL_D_NM | 의미 | 비고 |
|---|---|---|---|
| 01 | 활성 | 정상 사용 가능 | 기존 |
| 02 | 미사용 | 관리자 비활성화 | 기존 |
| 03 | 탈퇴 | 탈퇴 처리 | 기존 |
| 04 | **인증대기** | 관리자 생성 직후 (휴대폰 본인인증 미완료) | **prafta-036 신규** |

---

## 4. 작업 단위 분해 (권장 착수 순서)

> 권장 순서: **A → B(병렬: A 완료 후 B/C/D 병렬 가능) → C → D → E**.
> 근거: A(스키마+SYS013)가 B/C/D의 전제. B(단건 생성 BE)와 C(엑셀 BE)는 도메인 로직이 겹치므로 B 먼저 완성 후 C가 그 헬퍼를 재사용. D(프론트)는 B/C API 계약 확정 후 착수가 안전. E(로그인 인증대기 분기)는 A의 SYS013 코드만 의존하므로 B/C/D와 병렬 가능하지만 E2E 검증 측면에서 D와 같은 시점이 안전.

---

### PRAFTA-036-1 (A) — SYS013 코드 신설 + 마이그레이션

- **유형**: backend (DB 마이그레이션만)
- **영역**: web (공통 코드)
- **모듈**: common/cmm (시드)
- **작업 유형**: 신규
- **목적**: 관리자 생성 직후 휴대폰 인증 미완료 상태를 표현하는 `SYS013='04' 인증대기` 코드 도입.
- **산출물**:
  - `prafta-backend/src/main/resources/sql/migration/prafta-036-sys013-pending.sql` (신규)
    - 부재 확인 주석 + `INSERT INTO tb_syst_val_d (SYST_VAL_CD='SYS013', SYST_VAL_D_CD='04', SYST_VAL_D_NM='인증대기', SORT_IDX=4, USE_YN='Y', INSERT_NO='SYSTEM');`
    - 패턴은 `prafta-031-sys045-noti-type.sql` 헤더 형식 그대로.
  - (선택) `tb_user.ACCOUNT_STATUS` COMMENT를 `'계정상태[SYS013] 01:활성 02:미사용 03:탈퇴 04:인증대기'`로 정정하는 ALTER COMMENT 1줄(피드백 규칙 "코드성 컬럼 COMMENT" 준수).
  - `tb_daily_user.ACCOUNT_STATUS`도 같은 SYS013을 참조하므로 COMMENT 정렬 1줄 추가(스키마 일관성).
- **핵심 파일**:
  - `prafta-backend/src/main/resources/sql/migration/prafta-036-sys013-pending.sql`
  - (선택) 같은 파일 안에 `ALTER TABLE tb_user MODIFY COLUMN ACCOUNT_STATUS varchar(20) NOT NULL DEFAULT '01' COMMENT '...'`. `tb_daily_user` 동일.
- **정책서/스키마 출처**:
  - 정책서 공통 §3.5 계정 상태(활성/미사용/탈퇴 — `04 인증대기`는 신규 확장).
  - 스키마: `tb_user.ACCOUNT_STATUS varchar(20) NOT NULL DEFAULT '01' COMMENT '계정상태[SYS013]'` (`schema-full.sql:931`). `tb_daily_user.ACCOUNT_STATUS` (`:213`).
  - 마이그레이션 패턴: `prafta-backend/src/main/resources/sql/migration/prafta-031-sys045-noti-type.sql`.
- **의존성**: 없음 (선행 = 없음).
- **운영 적용**: 사용자 수동(read-only MCP). 본 파일은 작성만, DB 직접 적용 금지.

---

### PRAFTA-036-2 (B) — 단건 생성 백엔드 (`POST /webApi/user01/insert-user-info`)

- **유형**: backend
- **영역**: web
- **모듈**: user/user01
- **작업 유형**: 신규
- **목적**: 관리자가 `UserInfoPop` 생성 모드에서 입력한 1명을 생성한다. 권한·중복·매핑 검증 후 `tb_user`/`tb_user_site_auth`(+ 선택 `tb_user_service_credit`)에 단일 트랜잭션으로 INSERT.
- **산출물**:
  - Controller endpoint 신규: `POST /webApi/user01/insert-user-info`
  - Request DTO `UserCreateRequest` (D4 컬럼 전체)
  - Param `UserCreateParam.from(request, claims)` — 토큰에서 `gvCmpnyCd`/`gvUserCd`/`gvAuthCd`/`gvAuthLevel` 흡수
  - Service `User01ServiceImpl.insertUserOne(UserCreateParam)`:
    1. 권한 가드(`AuthRoleUtils.isManager(gvAuthCd)` 아니면 `UserErrorCode.USER_403_001`)
    2. 생성 대상 AUTH_CD `sortIdx ≥ gvAuthLevel` 검증(권한 상승 금지)
    3. 필수값 검증(사용자ID/명/권한/사업장/부서, 휴대폰, 생년월일)
    4. siteNo → siteCd 매핑(`selectSiteCdByNo`), 부재 시 "사업장번호없음"
    5. NODE_CD 존재 검증(`selectSiteNodeExists`), 부재 시 "부서코드없음"
    6. 사용자ID 중복(`selectUserIdExists`), 중복 시 "사용자ID중복"
    7. 휴대폰 정규화 + HMAC 산출, MBL_NO_HMAC 중복(`selectUserMblHmacExists`), 중복 시 "휴대폰번호중복"
    8. USER_CD 채번(`LoginMapper.selectUserCd` 재사용 또는 동일 SQL 복제 — developer 결정)
    9. 초기 비밀번호 = `passwordHasher.hash(휴대폰11자리)` (D3)
    10. AES-GCM 암호화(휴대폰/이메일/생년월일), HMAC 산출(휴대폰/이메일), LAST4 추출
    11. `tb_user` INSERT: `ACCOUNT_STATUS='04'`, `USE_YN='Y'`, `INSERT_NO=gvUserCd`, `HIRE_DATE`(YYYYMMDD), `EMPLOYMENT_TYPE`, `CONTRACT_END_DATE`(있으면), `RANK_CD`(있으면)
    12. `tb_user_site_auth` INSERT (사용자 ↔ 사이트 한 줄)
    13. 경력 1건 입력 시 `tb_user_service_credit` INSERT(`USE_YN='Y'`)
    14. 모든 단계 단일 트랜잭션(`@Transactional rollbackFor=Exception.class`). 실패 시 전체 롤백.
  - Mapper: `insertOneUser`, `selectSiteCdByNo`, `selectSiteNodeExists`, `selectUserIdExists`, `selectUserMblHmacExists`, `insertUserSiteAuthOne`, `selectAuthSortIdx`
- **핵심 파일**:
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/controller/User01Controller.java` (endpoint 추가)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/dto/request/UserCreateRequest.java` (신규)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/application/param/UserCreateParam.java` (신규)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/application/command/UserCreateCommand.java` (신규)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/service/User01Service.java` + `service/impl/User01ServiceImpl.java`
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/mapper/User01Mapper.java` + `src/main/resources/com/prafta/web/user/user01/mapper/User01Mapper.xml`
- **정책서/스키마 출처**:
  - 정책서 공통 §3.1(필수/선택 입력 — 회원가입 기준 참고), §3.5(계정 상태), §8(권한 결정), §11.1(PII 암호화/최소수집), §11.3(권한 변경/사업장 권한 변경 감사 로그).
  - 스키마: `tb_user` 전체(`schema-full.sql:910~947`), `tb_user_site_auth`(`:1231~1240`), `tb_user_service_credit`(`:1213~1227`), `tb_site`(SITE_NO/SITE_CD 매핑), `tb_site_node`(NODE_CD 존재 검증).
  - 코드 패턴: `LoginServiceImpl.insertUserInfo`(:135~181)의 정규화·AES-GCM·HMAC·채번 흐름 그대로 차용. `LoginMapper.xml insertUserInfo`(:157~205) SQL 구조.
- **의존성**: A 완료(SYS013 04 코드 존재 가정).
- **우선순위 근거**: 신규 핵심 기능 + 보안 영향(PII 신규 적재) → +1단계 격상. C/D보다 먼저.

---

### PRAFTA-036-3 (C) — 엑셀 양식 다운로드 + 대량 업로드 백엔드

- **유형**: backend
- **영역**: web
- **모듈**: user/user01
- **작업 유형**: 신규
- **목적**: 양식 .xlsx 다운로드(D4 구조) + 작성된 .xlsx 업로드 → 행별로 B의 단건 생성 로직 재사용해 일괄 처리 → 실패 모은 `UserBatchUpdateResponse` 반환.
- **산출물**:
  - Endpoint 2건:
    - `GET /webApi/user01/user-create-template` — xlsx 스트림 응답(`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename*=UTF-8''사용자생성양식.xlsx`)
    - `POST /webApi/user01/upload-user-creates` — `MultipartFile file` + JWT
  - Excel 의존성 추가: `prafta-backend/build.gradle`에 `implementation 'org.apache.poi:poi-ooxml:5.2.5'`. (또는 프론트 SheetJS 위임 — developer 결정 포인트.)
  - Service:
    - `byte[] buildUserCreateTemplate(claims)`:
      - Workbook 생성 → 시트 1개("사용자생성")
      - 1행: 셀 0에 안내문 "4행부터 데이터가 저장됩니다.", `XSSFFont` 색상 빨강, bold (셀 fill은 옵션 — developer 결정)
      - 2행: D4 헤더(한글, 15개 컬럼 — 필수 5 + 선택 10)
      - 3행: 예시 데이터 1행(컬럼별 샘플 값. ex: `kim001`, `김프라프타`, `99999`, `0001`, `NODE_001`, `010-1234-5678`, `kim@example.com`, `M`, `900101`, `STAFF`, `20200101`, `REGULAR`, "", `0`, "", "")
      - 컬럼 너비 적절히 설정(15~20)
      - 헤더 행은 굵게/배경색 옅음
      - 셀 데이터 검증(Data Validation) 추가는 옵션: 고용형태 드롭다운(`REGULAR,CONTRACT,DAILY,EXECUTIVE`), 성별(`M,F`). **developer 결정 포인트**.
    - `UserBatchUpdateResponse uploadUserCreates(MultipartFile, claims)`:
      1. 권한 가드(`AuthRoleUtils.isManager`)
      2. 파일 검증: 확장자 `.xlsx`(`originalFilename`/MIME), 크기 ≤5MB, NOT NULL
      3. POI로 시트 0 파싱: 1행/2행/3행 skip, 4행~ 데이터 수집(빈 행 종료)
      4. 데이터 행 수 ≤1000 검증
      5. 행별로 `UserCreateParam`을 만들어 `User01BatchService.insertUserBatch(List)` 호출
      6. 결과 `UserBatchUpdateResponse`(기존 record 재사용 — `successCount/failCount/fails`) 반환
    - 실패 사유 표준 한글 메시지(D6): "사용자ID중복" / "휴대폰번호중복" / "사업장번호없음" / "부서코드없음" / "권한코드없음" / "필수값누락" / "고용형태오류" / "사유유형오류" / "행N: 메시지" 등.
  - Batch Service:
    - `User01BatchService.insertUserBatch(List<UserCreateParam>)` 신규. 기존 `updateUserInfoBatch`의 try/catch/REQUIRES_NEW continue 패턴 그대로 차용. 행 인덱스를 `UserUpdateFailItem.index`에 보존.
- **핵심 파일**:
  - `prafta-backend/build.gradle` (POI 의존성)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/controller/User01Controller.java` (endpoint 2개 추가)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/service/User01Service.java` + `User01ServiceImpl.java` (`buildUserCreateTemplate`, `uploadUserCreates`)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/service/User01BatchService.java` + `User01BatchServiceImpl.java` (`insertUserBatch`)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/util/UserExcelTemplateBuilder.java` (신규, 양식 빌더)
  - `prafta-backend/src/main/java/com/prafta/web/user/user01/util/UserExcelRowParser.java` (신규, 행 파서)
- **정책서/스키마 출처**:
  - 정책서 공통 §3.1(필수 입력), §11.1(파일 업로드 최소수집·검증), §11.3(다운로드 감사 로그).
  - 스키마: `tb_user`/`tb_site`(SITE_NO 매핑)/`tb_site_node`(NODE_CD 검증)/`tb_user_service_credit`(`SYS042` 사유 유형).
  - 코드 패턴: 기존 `User01BatchServiceImpl.updateUserInfoBatch`(:30~91) 완전 차용.
- **의존성**: B 완료(`insertUserOne` 로직을 행별로 재사용).
- **우선순위 근거**: B의 헬퍼 재사용 + 신규 외부 의존성(POI) 도입 → B 직후.

---

### PRAFTA-036-4 (D) — 프론트엔드: User_01 ViewHeader 생성 버튼 + 엑셀 버튼 + UserInfoPop 생성 모드

- **유형**: frontend-screen + frontend-component
- **영역**: web
- **모듈**: user (User_01.vue + popup/UserInfoPop.vue)
- **작업 유형**: 보완
- **목적**: 단건 생성 진입점(D1), 엑셀 양식 다운로드/업로드 UI, UserInfoPop 생성 모드(SMS 인증 숨김·생성용 폼) 추가.
- **산출물 (Vue 골격 — developer가 axios/router/store 영역만 채우면 동작)**:
  - `User_01.vue` 수정:
    - `localButtons.create = 'Y'` 활성화, `@create="fnCreate"` 핸들러
    - `subtitle-row .custom-btn-area`에 버튼 2개 추가: "양식 다운로드", "엑셀 업로드"
    - 숨겨진 `<input type="file" accept=".xlsx" ref="excelFileRef" @change="fnExcelFileChange" style="display:none">`
    - `fnCreate` → `openPop(UserInfoPop, { cmpnyCd_p, userId_p: '', callmethod_p: 'C', onSearch: fnSearch })`
    - `fnDownloadTemplate` 함수 골격(developer가 `axios.get('/webApi/user01/user-create-template', { responseType: 'blob' })` 후 Blob URL 다운로드 채움)
    - `fnUploadExcelClick` → `excelFileRef.value.click()`
    - `fnExcelFileChange(event)` 함수 골격(developer가 FormData + `POST /upload-user-creates` + `BatchResultPop` 호출 채움)
  - `popup/UserInfoPop.vue` 수정:
    - `props.callmethod_p === 'C'` 분기 추가
    - `userId`/`userNm` 필드 disabled 해제(생성 모드 한정)
    - SMS 인증 UI 영역에 `v-show="callmethod_p !== 'C'"` 또는 동등 가드(생성 모드 숨김)
    - 권한 드롭다운은 기존 `sortIdx >= authLevel` 필터 유지(D1)
    - 입사일 input(YYYY-MM-DD, 생성 모드에서 직접 입력)
    - 고용형태/계약종료일 신규 입력 필드(생성 모드 한정 노출 — `EMPLOYMENT_TYPE` 컬럼이 신규에서 필수에 가깝지만 본 작업에서는 선택)
    - 경력 인정 0~1건만 허용(생성 모드 — `fnAddCredit` 가드)
    - `fnUserInfoSave` 분기:
      - 생성 모드 → `POST /webApi/user01/insert-user-info`(body: D4 컬럼) + 경력 1건 같이 전송
      - 조회 모드(기존) → 기존 `/update-user-infos` + `/update-user-credit` 유지
    - 회원탈퇴/탈퇴취소/비밀번호초기화 버튼은 `v-if="callmethod_p !== 'C'"`
  - script 영역에 추가되는 ref/함수 시그니처는 작성하되 본문은 `// TODO(developer)` 주석으로 남김(API 호출/라우터/store 작성 금지 — planner 가드)
  - style: 추가 CSS 변수만 사용. 새 클래스 필요 시 BEM 또는 기존 패턴(`.custom-btn-area`, `.form-row-max`). 하드코딩 색상 금지.
- **핵심 파일**:
  - `prafta-web-frontend/prafta-web-frontend/src/views/user/User_01.vue` (template + script + style 보완)
  - `prafta-web-frontend/prafta-web-frontend/src/views/user/popup/UserInfoPop.vue` (생성 모드 분기 추가)
- **정책서/스키마 출처**:
  - 정책서 공통 §3.1·§3.5·§13(UI/UX 일관성).
  - 스키마: D4 컬럼이 매핑되는 `tb_user`/`tb_user_service_credit` 컬럼 형식 검증(YYYYMMDD/숫자/SYS042 등).
  - 기존 UI 패턴: `User_01.vue`의 ViewHeader + subtitle-row + table-box, `UserInfoPop.vue`의 form-row-max + leave-section, `BatchResultPop.vue`의 결과 그리드.
- **의존성**: B 완료(`/insert-user-info` 계약 확정), C 완료(`/user-create-template` + `/upload-user-creates` 계약 확정).
- **우선순위 근거**: API 계약이 확정된 후 안전. B/C 동시 진행 시 mock으로 병렬화 가능하나 권장 순서는 B/C 후.

---

### PRAFTA-036-5 (E) — 로그인 인증대기 분기 + 휴대폰 인증 완료 시 상태전이

- **유형**: backend (프론트 측 변경은 최소)
- **영역**: web (`common.cmm.login`)
- **모듈**: common/cmm/login
- **작업 유형**: 보완
- **목적**: `ACCOUNT_STATUS='04'`(인증대기) 사용자가 로그인 시 일반 토큰 발급 대신 휴대폰 인증 흐름으로 분기시키고, 인증 완료 시 `'01'`로 전이.
- **산출물**:
  - `LoginServiceImpl.Login(LoginParam)` 수정:
    - 비밀번호 일치 후 `userResult.accountStatus()`가 `'04'`면 일반 토큰 발급(JWT/refresh) 대신 임시 응답으로 분기. **developer 결정 포인트**: (a) 별도 임시 토큰(scope=PHONE_AUTH_ONLY)을 발급 + 응답에 `nextStep='PHONE_AUTH'`, (b) 토큰 발급 자체를 막고 응답 body의 `userCd`/`mblNoLast4` 등으로 인증 endpoint 호출 식별. planner 권장: **(a)** 임시 JWT(만료 10분, claim `gv_scope='PHONE_AUTH'`)를 발급해서 `JwtUtil` 일관성 유지 + `verify-phone-auth` 이외 endpoint는 거부.
    - `'02'`(미사용) / `'03'`(탈퇴)는 기존 동작 유지(로그인 차단).
  - `LoginResponse`에 `accountStatus`/`nextStep` 필드 추가(기존 클라이언트 영향 최소화 위해 nullable).
  - `LoginMapper.Login` SQL이 `ACCOUNT_STATUS` 컬럼을 SELECT하는지 확인 + 미포함 시 추가. `UserResult.accountStatus()` 노출.
  - `POST /login/verify-phone-auth` 신규 endpoint(`@NoAuth`이되 임시 토큰 검증 미들웨어 또는 인자 검증):
    - body: `{ certNo, mblNo }` + Authorization(임시 토큰)
    - 1) 임시 토큰의 `scope=PHONE_AUTH` 검증, `userCd`/`cmpnyCd` 추출
    - 2) 기존 `BaseinfoService.userSmsAuthCheck(mblNo, certNo)` 재사용 (이미 구현됨)
    - 3) 인증 통과 → `tb_user` 대상 사용자 `ACCOUNT_STATUS='01'`로 UPDATE + (관리자가 입력한 mblNo와 인증한 mblNo가 다르면) `MBL_NO_ENC/HMAC/LAST4` 갱신
    - 4) 4단계 갱신 시 다른 사용자의 `MBL_NO_HMAC`와 충돌 → 명확한 한글 에러("이미 다른 계정에서 사용 중인 휴대폰번호입니다.")
    - 5) `PWD_CHG_DTIME` 정책은 본 작업 범위 밖 → NULL 유지
    - 6) 본격 JWT/refresh 발급 후 `LoginResponse` 반환 (기존 Login 응답 형태로)
  - 프론트 변경(최소):
    - 로그인 응답에서 `nextStep='PHONE_AUTH'` 또는 `accountStatus='04'`면 휴대폰 인증 화면/팝업으로 라우팅 분기. **developer 결정 포인트**: 기존 휴대폰 인증을 위한 화면이 있다면 재사용, 없으면 로그인 화면 내에서 SMS 인증요청/인증번호 입력만 노출하는 단순 모드 추가(별도 화면 신설은 follow-up).
- **핵심 파일**:
  - `prafta-backend/src/main/java/com/prafta/common/cmm/login/service/impl/LoginServiceImpl.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/login/controller/LoginController.java` (verify-phone-auth 추가)
  - `prafta-backend/src/main/java/com/prafta/common/cmm/login/dto/response/LoginResponse.java`
  - `prafta-backend/src/main/java/com/prafta/common/cmm/login/result/UserResult.java`
  - `prafta-backend/src/main/resources/com/prafta/common/cmm/login/mapper/LoginMapper.xml` (`Login` SELECT 컬럼 확인 + `verifyPhoneAuth` 업데이트 쿼리)
  - `prafta-backend/src/main/java/com/prafta/common/security/JwtUtil.java` (임시 scope 토큰 발급 메서드 — 기존에 없으면 신규)
- **정책서/스키마 출처**:
  - 정책서 공통 §3.2(로그인), §3.4(토큰), §3.5(계정 상태), §11.1(PII 갱신 시 평문 미저장), §11.3(권한 변경 감사 로그 — `ACCOUNT_STATUS 04→01` 전이 기록 권장).
  - 스키마: `tb_user.ACCOUNT_STATUS`/`MBL_NO_ENC`/`MBL_NO_HMAC`/`MBL_NO_LAST4` (`schema-full.sql:919~931`), `UX_TB_USER_MBL_NO` UNIQUE(`:944`).
  - 코드 패턴: `BaseinfoController.userSmsAuthCheck`(:161~166), `LoginServiceImpl.Login`(:64~125).
- **의존성**: A 완료(SYS013 04 코드 존재).
- **우선순위 근거**: B/C/D와 병렬 가능. E 미완 시 관리자가 만든 계정이 로그인 자체 불가(인증대기 분기가 없으면 일반 토큰 발급되고 휴대폰 미인증 상태로 모든 기능 접근). **B/C/D보다 늦어도 같이 배포해야** 운영 적용 가능. ⇒ B/C 완료 직후 착수 권장.

---

## 5. 작업 순서 / 의존성 그래프

```
A (PRAFTA-036-1, SYS013 마이그)
  └─→ B (PRAFTA-036-2, 단건 생성 BE)
        └─→ C (PRAFTA-036-3, 엑셀 BE)
              └─→ D (PRAFTA-036-4, 프론트)
  └─→ E (PRAFTA-036-5, 로그인 분기)   # B/C/D와 병렬 가능, 단 운영 배포는 동시
```

권장 착수: **A → B → C → D** 직렬 + **E는 B 완료 후 병렬**.

---

## 6. 비기능 요구사항 (전 단계 공통)

### 6.1 보안 가드
- 모든 신규 endpoint에 `AuthRoleUtils.isManager(gvAuthCd)` 가드(B/C). `verify-phone-auth`는 임시 scope 토큰 검증.
- `cmpnyCd`는 토큰 `gv_cmpnyCd`만 사용(요청 body 무시 — IDOR 방지).
- 신규 사용자의 AUTH_CD 권한레벨이 요청자 `gv_authLevel` 이상(`sortIdx >= authLevel`)인지 서버측 이중 검증(권한 상승 금지 — D7).
- 파일 업로드: `.xlsx`만, ≤5MB, 데이터 행 ≤1000. 위반 시 명확한 한글 에러.
- 휴대폰/이메일/생년월일은 AES-GCM + HMAC 인덱스(`Normalizers.normalizePhone/normalizeEmail/normalizeBirth` + `aesGcmCrypto.encrypt` + `hmacSigner.hmacSha256Base64Url` — 기존 `LoginServiceImpl.insertUserInfo`(:135~167) 패턴 그대로).
- 비밀번호: `PasswordHasher.hash(mblNo11)` (BCrypt). 평문 저장 금지.
- 모든 SQL: 명시적 컬럼 나열, `#{}` 바인딩, leading comma, `SELECT *` 금지.

### 6.2 감사 로그 (정책서 §11.3)
- 신규 계정 생성: 누가(`gvUserCd`)·언제(`INSERT_DATE`)·무엇을(`USER_CD`/`USER_ID`)·왜(엑셀 업로드면 파일명) — 최소 로그.
- `ACCOUNT_STATUS 04→01` 전이: 인증 통과 시 `log.info` + 가능하면 별도 감사 테이블 적재(범위 밖이면 application log만).
- 엑셀 양식 다운로드: 다운로드 행위는 정책 §11.3의 "다운로드" 감사 대상 → `log.info("user-create-template downloaded by {}", gvUserCd)` 최소.

### 6.3 i18n / 메시지
- 모든 에러/안내 메시지는 한국어. 표준 메시지는 `messages` 키로 등록(예: `MSG.USER_CREATE_SUCCESS`, `MSG.USER_CREATE_DUP_USER_ID`, `MSG.USER_CREATE_DUP_PHONE`, `MSG.USER_CREATE_BAD_SITE`, `MSG.USER_CREATE_BAD_NODE`, `MSG.USER_CREATE_BAD_AUTH`, `MSG.EXCEL_INVALID_FORMAT`, `MSG.EXCEL_TOO_MANY_ROWS`, `MSG.EXCEL_TOO_LARGE` 등). developer가 기존 `messages.js`에 추가.
- 백엔드 로그·application log는 한국어(prafta 컨벤션).

### 6.4 트랜잭션
- B(단건): `@Transactional(rollbackFor=Exception.class)` 단일 트랜잭션. 사이트 권한·경력 INSERT 모두 동일 트랜잭션.
- C(엑셀): 행별로 `REQUIRES_NEW`(기존 `updateOneUserInfo` 패턴) → 한 행 실패가 다른 행에 영향 주지 않음.
- E(인증 통과 시 상태 전이): 단일 트랜잭션 안에서 `ACCOUNT_STATUS` UPDATE + 필요 시 `MBL_NO_*` UPDATE.

### 6.5 화면 UX (정책서 §13.3)
- 단건 생성 모드에서 인증 UI를 숨길 때 단순 v-show보다 영역 자체를 v-if로 제거해 폼 흐름이 자연스럽게.
- 엑셀 업로드 결과는 `BatchResultPop`로 일관(D6). 성공만 있어도 알럿이 아닌 결과 팝업으로 처리(요약: "요청 N건 중 N건 성공" 명시).
- 양식 다운로드 버튼·엑셀 업로드 버튼은 부서 일괄지정 버튼 옆 같은 줄에 배치(시각적 그룹화). 색상은 기존 `.custom-btn-area .btn-custom`/`.btn-primary` 토큰만 사용.

---

## 7. 미해결 / Follow-up 후보 (이번 범위 밖)

| # | 항목 | 비고 |
|---|---|---|
| F1 | **첫 로그인 비밀번호 강제 변경** | 관리자가 만든 계정(`USER_PW = BCrypt(mblNo11)`)은 초기 비밀번호가 휴대폰번호와 동일 → 첫 로그인 후 강제 변경이 보안상 권장. 기존 `PWD_CHG_DTIME` 컬럼이 있지만 강제 변경 화면/플로우는 미정의. 별도 작업으로 분리. |
| F2 | **로그인 화면 인증대기 전용 UI 화면 신설** | D2 결정: 본 작업은 분기 라우팅과 상태전이만. 인증대기 전용 화면(예: "안녕하세요 OOO님, 휴대폰 본인인증 후 사용 가능합니다" 안내)을 만들지, 기존 로그인 화면에 인라인 노출할지 별도 검토. |
| F3 | **앱(webview) 측 인증대기 분기** | 본 작업은 웹만. 앱에서 인증대기 계정으로 로그인 시 동일 분기 처리가 필요(현재 앱 로그인 흐름 위치: `prafta-app-frontend`). |
| F4 | **엑셀 양식 컬럼 검증 강화** | D4의 Data Validation(드롭다운: 고용형태/성별, 숫자 제한: 경력개월) 적용 여부 — POI Data Validation 작성 부담 vs 양식 편집 편의 트레이드오프. 우선 미적용으로 가고 follow-up. |
| F5 | **다운로드 감사 로그** | 정책서 §11.3은 "다운로드"를 감사 대상으로 명시. 본 작업은 application log만 남기는 수준. 별도 감사 테이블 적재(예: `tb_audit_log`)는 follow-up. |
| F6 | **엑셀 업로드 비동기/스트리밍** | 1000행 제한 안에서는 동기 처리로 충분하나, 향후 수천 행 지원이 필요하면 비동기 잡 큐 도입. |
| F7 | **단건 생성 시 `tb_user_site_auth` 다중 사이트 권한** | 현재 단건 생성은 1개 사이트만 권한 부여. 다중 사이트 권한이 필요한 사용자는 생성 후 별도 `Baim` 권한 화면에서 추가. |
| F8 | **임시 토큰(scope=PHONE_AUTH) JwtUtil 확장** | 현재 `JwtUtil`이 scope 클레임/만료 10분 토큰을 지원하는지 정독 미완. 없으면 신규 메서드 추가가 E에 포함되어야 함(planner 표기 — developer가 정독 결정). |
| F9 | **신규 SYS013 04 코드의 다국어/대시보드 라벨** | 본 작업은 한글만. 다른 화면(Attd 등)에서 ACCOUNT_STATUS 표시할 때 인증대기가 적절히 렌더되는지 회귀 확인. |

---

**최종 업데이트**: 2026-05-28 — planner 분해 완료. 본 문서는 후속 작업(developer/security/qa) 모든 결정의 단일 출처(SSOT)다.
