# prafta-037-F1 — 첫 로그인 강제 비밀번호 변경 : 작업 분해 계획

> 작성: planner 세션 (2026-05-29).
> 원본 요청서: `.claude/requests/web_requests/prafta-037.md` §3 (F1).
> 상위 컨텍스트: `.claude/requests/web_requests/prafta-036-plan.md` (verify-phone-auth + LoginResponse 구조).
> 정책서 출처: 공통 §3.2(로그인), §3.3(비밀번호), §3.4(토큰), §3.5(계정 상태).
> 본 plan 은 F1 의 단일 출처(SSOT)다.

---

## 0. 개요

- 관리자 생성 계정 초기 PW = 휴대폰번호 11자리 BCrypt 해시(prafta-036 D3). 사용자가 알고 있는 값이라 첫 로그인 직후 강제 변경이 필수.
- prafta-036 의 `nextStep` 분기 패턴(PHONE_AUTH)을 그대로 차용해 `nextStep='PASSWORD_CHANGE'` 분기를 추가한다.
- 신규 스키마 컬럼 추가 없음. 기존 `tb_user.PWD_CHG_DTIME` (`datetime DEFAULT NULL`, `schema-full.sql:935`) 활용 — NULL ⇒ "첫 PW 변경 미완료".
- 백엔드는 기존 `POST /webApi/user01/update-my-passwd` + `updateMyPw` 서비스 **재사용**. 신규 endpoint 신설 없음.
- 프론트는 신규 강제 변경 팝업 1개(`ForcedPasswordChangePop.vue`). 기존 `MyInfoPop` 재사용 검토했으나 모드 분기 비용·UX 차이(취소=로그아웃, 닫기 차단) 때문에 별도 팝업이 청결.
- 인증대기(PHONE_AUTH) 통과 직후에도 강제 변경 흐름이 자연스럽게 합류(D-1).

---

## 1. 사용자 확정 결정 + planner 결정 포인트

### 사용자 확정 (2026-05-29 채팅)

| # | 결정 | 내용 |
|---|---|---|
| **F1-1** | "첫 로그인" 정의 | `tb_user.PWD_CHG_DTIME IS NULL` 인 **모든 사용자**. 관리자 생성 계정만이 아니라 회원가입 직접 가입자도 강제. 식별자 컬럼 추가 없이 기존 NULL 판정만으로. |
| **F1-2** | 강제 변경 거부 시 동작 | **메인 진입 차단**. 닫기/취소 → 로그인 화면 복귀 + sessionStorage/refreshToken/userStore clean + `POST /comApi/login/logout` 호출(활성 세션 revoke). |

### planner 자율 결정 (본 문서 권위, 재논의 없음)

| # | 결정 | 내용 | 근거 |
|---|---|---|---|
| **D-1** | 인증대기 통과 후 강제 변경 합류 | **합류 강제**. `verify-phone-auth` 응답에도 `nextStep='PASSWORD_CHANGE'` 자동 노출. 인증대기 통과 직후 PWD_CHG_DTIME 그대로 NULL 이라 자연 합류. | 04→01→PW변경 의 3단계 전이. 중간 자유 진입 허용하면 휴대폰번호=PW 상태로 메인 접근 가능 → 보안 의미 사라짐. |
| **D-2** | 동일 PW 거부 (현재 PW = 신규 PW) | **백엔드 가드**. `User01ServiceImpl.updateMyPw` 의 신규 PW 정책 검증 직후 `passwordHasher.matches(newPw, userPwResult.userPw())` true 면 `USER_400_010` 신규 에러. 자발/강제 변경 공통 적용. | 첫 로그인 강제 변경의 의도가 "휴대폰번호=PW 해소"인데 현재 PW 재입력은 의미 무효. 자발 변경(MyInfoPop)도 동일 거부가 일관성 있음. 정책서 §3.3 정신과 일치. |
| **D-3** | 변경 후 자동 로그인 vs 재로그인 강제 | **현재 토큰 유지 + 메인 즉시 진입**. `update-my-passwd` 응답 후 토큰 폐기 없이 정상 라우팅. `updateMyPw` 가 이미 PWD_FAIL_CNT=0/PWD_LOCK_YN='N' 초기화. | 재로그인 강제는 마찰. 토큰 클레임에 PW 해시 없음 → JWT 무효화 이유 없음. verify-phone-auth 도 즉시 정식 토큰 발급 패턴. |
| **D-4** | 기존 사용자 PWD_CHG_DTIME=NULL 처리 | **마이그레이션 없음 / 자연스러운 처리**. 다음 로그인 시 자동으로 강제 변경 합류. 마이그 NOW() 일괄은 보안 후퇴. | 휴대폰번호=PW 상태 일괄 통과 방지. 단 운영자가 사용자 사전 공지 필요(§7). |
| **D-5** | 앱(F3) 합류 시점 | **본 분해는 웹 단독**. F3 완료 후 별도 PR. 본 문서 §3.4 에 앱 합류 점검 포인트만 메모. | F3 미착수. 의존성·테스트 범위 분리. |
| **D-6** | PW 정책 일관성 | 강제 변경 = 자발 변경 = **6~15자 + 숫자/영문/특수 중 2가지** (`User01ServiceImpl.isValidPassword` 재사용). 정책 강화는 별도 작업. | 일관성. 강화는 기존 사용자 전수 영향 → 별도 정책 결정 필요. |
| **D-7** | 신규 에러코드 채번 | `USER_400_010("새 비밀번호는 현재 비밀번호와 동일할 수 없습니다.")`. 충돌 시 다음 번호 시프트. | D-2 의 한글 메시지. |
| **D-8** | PHONE_AUTH 와 PASSWORD_CHANGE 동시 처리 | 인증대기 통과 직후 응답의 `nextStep` 우선순위 = `PASSWORD_CHANGE`. 평면 상태머신: `04→PHONE_AUTH→(통과)→PASSWORD_CHANGE→(완료)→메인`. | 누수 없는 자연 합류. |
| **D-9** | 강제 변경 endpoint 의 권한 모델 | **기존 `update-my-passwd` 그대로**. 별도 scope 토큰 신설 안 함. 첫 로그인 케이스도 정식 토큰 이미 발급된 상태. | 신규 endpoint/scope 불필요. PW 변경은 정식 사용자 권한으로 진행이 자연. |

---

## 2. 초안에서 잘라낸 / 보류한 항목

| 항목 | 처리 | 이유 |
|---|---|---|
| PW 만료 정책(90일) | 범위 밖 → §7 | 별도 정책 결정 필요. |
| 이전 PW 재사용 금지(N개 비교) | 범위 밖 → §7 | 신규 테이블 또는 컬럼 필요. |
| 마이그 일괄 NULL→NOW() | 불채택 (D-4) | 보안 후퇴. |
| PW 정책 강화(8자 이상, 특수문자 필수 등) | 범위 밖 → §7 | 자발 변경과 일관 유지(D-6). |
| 앱(F3) 동시 합류 | 별도 PR (D-5) | F3 미착수. |
| 강제 변경 시 본인인증 재요구 | 불채택 | 정식 토큰 이미 발급된 상태(D-9). |
| 변경 후 재로그인 강제 | 불채택 (D-3) | 마찰. |
| PW 변경 감사 테이블 적재 | 범위 밖 → F5 통합 | 감사 인프라 결정 시 함께. |

---

## 3. 영향 범위

### 3.1 스키마 / 마이그레이션

**무변경**. 기존 `tb_user.PWD_CHG_DTIME` 활용.

### 3.2 백엔드

#### LoginResponse 합류

| 영역 | 파일 | 변경 |
|---|---|---|
| Response DTO | `common/cmm/login/dto/response/LoginResponse.java` | `mustChangePassword: Boolean` 필드 추가(JSON 호환 위해 박싱). |
| Factory | `LoginResponse.from(userResult, refreshToken, token)` | `userResult.pwdChgDtime()` 이 null/blank 이면 `mustChangePassword=true`/`nextStep="PASSWORD_CHANGE"`. |
| Factory | `LoginResponse.phoneAuthPending(...)` | 무변경(아직 정식 토큰 발급 전). |

`LoginServiceImpl.Login` 과 `verifyPhoneAuth` 는 둘 다 마지막에 `LoginResponse.from` 호출 → **자동 합류**. 별도 코드 변경 없음.

#### updateMyPw 동일 PW 거부 가드

| 영역 | 파일 | 변경 |
|---|---|---|
| Service | `web/user/user01/service/impl/User01ServiceImpl.java` `updateMyPw` | 3단계(정책 검증) 직후, 4단계(해시+UPDATE) 직전 가드 추가: `if (passwordHasher.matches(newPw, userPwResult.userPw())) throw USER_400_010`. |
| Error Code | `common/error/user/UserErrorCode.java` | `USER_400_010` 신규 추가. |

#### 변경하지 않는 파일

- `POST /webApi/user01/update-my-passwd` endpoint: 무변경.
- `MyPasswdParam.from`: 무변경(IDOR 가드 유지).
- `LoginMapper.Login` SQL: 무변경(`pwdChgDtime` 이미 노출).
- `selectUserByUserCd`(verify-phone-auth 가 사용): 무변경.
- `JwtUtil`: 무변경.
- `User01Mapper.updateMyPw` SQL: 무변경(이미 PWD_CHG_DTIME=NOW() + 잠금 초기화).

### 3.3 프론트엔드 (웹)

#### 신규 — ForcedPasswordChangePop.vue

위치: `prafta-web-frontend/prafta-web-frontend/src/views/login/popup/ForcedPasswordChangePop.vue`

**props/emits**:
- props: `onSuccess: Function (required)`, `onCancel: Function (required)`
- emits: `close`

**와이어프레임**:
```
+----------------------------------------------+
|  비밀번호 변경 (필수)                    [X] |
+----------------------------------------------+
| ⓘ 첫 로그인 시 보안을 위해 비밀번호 변경이   |
|    필요합니다. 변경 미완료 시 메인 진입 불가  |
|                                              |
|  현재 비밀번호  [ ******** ]                  |
|  새 비밀번호    [ ******** ]                  |
|                 (6~15자, 숫자/영문/특수 2종)  |
|  비밀번호 확인  [ ******** ]                  |
+----------------------------------------------+
|              [ 변경 ]  [ 취소 ]               |
+----------------------------------------------+
```

**상태별**: loading(변경 버튼 disabled), empty(현재 PW 자동 포커스), error($alert + 입력 유지), success($alert → onSuccess()).

**사용자 플로우**:
1. 로그인/인증대기 통과 응답 nextStep=PASSWORD_CHANGE 수신.
2. 부모(LoginView) `openPop(ForcedPasswordChangePop, { onSuccess, onCancel })`. 토큰 이미 정식 발급(D-3).
3. 입력 → 변경 → `POST /webApi/user01/update-my-passwd` body=`{currentPw, newPw}` (cmpnyCd/userCd 토큰 강제).
4. 성공 → alert → `emit('close')` → `props.onSuccess()` → 메인 라우팅.
5. 취소/X → `$confirm("비밀번호 변경을 취소하면 로그인 화면으로 돌아갑니다. 계속하시겠습니까?")` → OK → `props.onCancel()` → 토큰 폐기 + `/comApi/login/logout` + 로그인 화면.

**Vue 골격** (PhoneAuthPop 패턴 차용, scoped CSS + CSS 변수만):

```vue
<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>비밀번호 변경 (필수)</span>
          <button class="icon-button" @click="fnCancel">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <p class="forced-notice">
            ⓘ 첫 로그인 시 보안을 위해 비밀번호 변경이 필요합니다.<br />
            변경을 완료하지 않으면 메인 화면으로 진입할 수 없습니다.
          </p>

          <div class="form-row-max">
            <label>현재 비밀번호</label>
            <input
              ref="currentPwFcs"
              type="password"
              v-model="currentPw"
              placeholder="현재 비밀번호"
              autocomplete="current-password"
            />
          </div>

          <div class="form-row-max">
            <label>새 비밀번호</label>
            <input
              type="password"
              v-model="newPw"
              placeholder="6~15자, 숫자/영문/특수 중 2가지 이상"
              autocomplete="new-password"
            />
          </div>

          <div class="form-row-max">
            <label>비밀번호 확인</label>
            <input
              type="password"
              v-model="newPwConfirm"
              placeholder="새 비밀번호 재입력"
              autocomplete="new-password"
            />
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="loading"
              @click="fnChangePassword"
            >
              변경
            </button>
            <button
              class="btn btn-secondary"
              :disabled="loading"
              @click="fnCancel"
            >
              취소
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
// TODO(developer): API 호출/메시지 import
// import axios from "@/api/axios";
// import { getMessage, MSG } from "@/messages";
// import { resolveApiErrorMessage } from "@/utils/apiError";

// =========================== Define ===========================
const emit = defineEmits(["close"]);
const props = defineProps({
  // 변경 성공 시 부모가 메인 라우팅 — script 본문에서 router 의존 금지(골격 규칙)
  onSuccess: { type: Function, required: true },
  // 취소 시 부모가 토큰 폐기 + 로그아웃 + 로그인 화면 복귀
  onCancel: { type: Function, required: true },
});

// =========================== Ref ===========================
const modalRef = ref(null);
const currentPw = ref("");
const newPw = ref("");
const newPwConfirm = ref("");
const currentPwFcs = ref(null);
const loading = ref(false);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3,
});

// =========================== Life Cycle ===========================
onMounted(() => {
  if (currentPwFcs.value) currentPwFcs.value.focus();
});

// =========================== Methods ===========================
const fnChangePassword = async () => {
  if (!currentPw.value) {
    await proxy.$alert("현재 비밀번호를 입력해 주세요.");
    return;
  }
  if (!newPw.value) {
    await proxy.$alert("새 비밀번호를 입력해 주세요.");
    return;
  }
  if (newPw.value !== newPwConfirm.value) {
    await proxy.$alert("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    return;
  }

  // TODO(developer): API 호출 + 에러 처리 + 성공 시 onSuccess 호출
  // - loading.value = true
  // - POST /webApi/user01/update-my-passwd { currentPw, newPw }
  // - 200 OK 시: $alert(getMessage(MSG.MY_INFO_PW_CHANGED)) → emit('close') → props.onSuccess()
  // - 실패 시: resolveApiErrorMessage 로 백엔드 에러 메시지 alert
  //   * USER_400_003 현재 PW 불일치
  //   * USER_400_004 정책 위반
  //   * USER_400_010 (신규) 동일 PW 거부
  // - finally: loading.value = false
};

const fnCancel = async () => {
  if (loading.value) return;
  const ok = await proxy.$confirm(
    "비밀번호 변경을 취소하면 로그인 화면으로 돌아갑니다. 계속하시겠습니까?"
  );
  if (!ok) return;
  // TODO(developer): emit('close') → props.onCancel()
  // 부모가 sessionStorage.clear() + localStorage.removeItem('refreshToken')
  //   + userStore.logout() + POST /comApi/login/logout(실패 무시) 수행
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.2rem;
  max-width: 460px;
  margin: 0 auto;
}

.forced-notice {
  background: var(--color-info-bg, #eff6ff);
  border-radius: var(--input-radius, 10px);
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: var(--color-info-text, #1d4ed8);
  line-height: 1.5;
  margin: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.75rem 1.2rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #f9fafb);
}

.btn-secondary {
  background: #ffffff;
  border: 1px solid var(--color-border, #e5e7eb);
  color: var(--color-text, #374151);
}
.btn-secondary:hover {
  background: var(--color-bg, #f9fafb);
}
</style>
```

#### LoginView 변경

| 파일 | 변경 |
|---|---|
| `src/views/login/LoginView.vue` | import `ForcedPasswordChangePop`. `fnApplyLoginResponse` 첫 줄에 PASSWORD_CHANGE 분기 추가 — PhoneAuthPop 통과 후에도 동일 콜백을 거치므로 한 곳에서 합류 처리(권장 리팩터). 신규 헬퍼 3개 추가. |

**LoginView 권장 리팩터** (developer 가 작성):

```javascript
const fnApplyLoginResponse = (data) => {
  // 인증대기 통과 후/일반 로그인 모두 PASSWORD_CHANGE 자동 합류
  if (data?.nextStep === "PASSWORD_CHANGE" || data?.mustChangePassword) {
    fnApplyLoginPartialSession(data);
    openPop(ForcedPasswordChangePop, {
      onSuccess: () => fnApplyLoginResponseAfterPwChange(data),
      onCancel: fnForcedPwCancel,
    });
    return;
  }
  // ... 기존 흐름 그대로
};

// 토큰/식별자만 세션 세팅, 라우팅 안 함(API 호출용 정식 토큰 활성화)
const fnApplyLoginPartialSession = (data) => {
  sessionStorage.setItem("token", data.token);
  sessionStorage.setItem("gv_cmpnyCd", data.cmpnyCd);
  sessionStorage.setItem("gv_userCd", data.userCd);
  sessionStorage.setItem("gv_userId", data.userId);
  // userStore 등도 마찬가지 — 변경 가능한 최소만
};

// PW 변경 성공 후 — 약관 체크부터
const fnApplyLoginResponseAfterPwChange = (data) => {
  if (rememberId.value) localStorage.setItem("savedUserId", data.userId);
  // 나머지 세션 슬롯도 정식 세팅(siteCd/siteNo/siteNm/nodeCd/nodeNm/authCd/authLevel/refreshToken)
  // 약관 체크 → 메인
  fnUserTermsAgrChk();
};

const fnForcedPwCancel = async () => {
  try {
    await axios.post("/comApi/login/logout"); // 활성 세션 revoke (실패 무시)
  } catch {}
  sessionStorage.clear();
  localStorage.removeItem("refreshToken");
  userStore.logout();
  userId.value = "";
  password.value = "";
};
```

`PhoneAuthPop.vue`: **무변경**. `props.onSuccess(response.data)` 가 LoginView 의 `fnApplyLoginResponse` 를 호출하므로 PASSWORD_CHANGE 분기는 LoginView 측에서 자동 처리(D-1).

`MyInfoPop.vue`: 무변경. D-2 의 동일 PW 거부 가드는 백엔드 단에서 적용 → `resolveApiErrorMessage` 가 USER_400_010 메시지를 자동 노출.

### 3.4 프론트엔드 (앱) — D-5 별도 PR

본 분해 범위 밖. F3 완료 후 합류. 점검 포인트:
1. 앱 로그인 응답 핸들러에 `mustChangePassword/nextStep` 분기 추가.
2. 앱용 ForcedPasswordChangePop(모달 vs 별도 라우트) — webview 적합성 결정.
3. 취소 시 토큰 폐기 + 로그인 화면 복귀(앱 router 패턴 — 메모리 `project_prafta_app_vite_and_api_align`).
4. 백엔드 endpoint 프리픽스(`appApi` vs `webApi`) 정독.

---

## 4. 작업 단위 분해 (권장 착수 순서)

### PRAFTA-037-F1-A (BE-1) — LoginResponse 에 mustChangePassword/nextStep 합류

- **유형**: backend
- **영역**: web
- **모듈**: common/cmm/login
- **목적**: 로그인 응답(일반 + verify-phone-auth)에 PWD_CHG_DTIME IS NULL 판정 결과 노출.
- **산출물**:
  - `LoginResponse` 에 `mustChangePassword: Boolean` 필드 추가.
  - `LoginResponse.from(userResult, refreshToken, token)`: `boolean mustChange = userResult.pwdChgDtime() == null || userResult.pwdChgDtime().isBlank();` → `mustChangePassword=mustChange`, `nextStep = mustChange ? "PASSWORD_CHANGE" : null`.
  - `LoginResponse.phoneAuthPending(...)` 무변경.
- **핵심 파일**: `common/cmm/login/dto/response/LoginResponse.java`
- **정책서/스키마 출처**: §3.3, `tb_user.PWD_CHG_DTIME` (`schema-full.sql:935`).
- **의존성**: 없음.

### PRAFTA-037-F1-B (BE-2) — updateMyPw 에 동일 PW 거부 가드 추가

- **유형**: backend
- **영역**: web
- **모듈**: user/user01
- **목적**: D-2 — 자발/강제 변경 공통으로 "현재 PW = 신규 PW" 거부.
- **산출물**:
  - `UserErrorCode.USER_400_010("새 비밀번호는 현재 비밀번호와 동일할 수 없습니다.")` 신규 추가(채번 충돌 시 시프트).
  - `User01ServiceImpl.updateMyPw` 의 3단계 직후 가드 추가:
    ```java
    if (passwordHasher.matches(param.newPw(), userPwResult.userPw())) {
        throw new ApiException(UserErrorCode.USER_400_010);
    }
    ```
- **핵심 파일**:
  - `common/error/user/UserErrorCode.java`
  - `web/user/user01/service/impl/User01ServiceImpl.java`
- **의존성**: 없음(A 와 병렬 가능).

### PRAFTA-037-F1-C (BE-3) — verifyPhoneAuth 응답 합류 검증

- **유형**: backend(검증만, 코드 변경 없음)
- **목적**: F1-A 의 `LoginResponse.from` 자동 처리로 verifyPhoneAuth 응답에도 nextStep=PASSWORD_CHANGE 가 실리는지 검증.
- **산출물**: 단위/통합 테스트 — 관리자 생성 계정(PWD_CHG_DTIME=NULL) 인증대기 통과 직후 응답에 mustChangePassword=true/nextStep=PASSWORD_CHANGE.
- **핵심 파일**: `common/cmm/login/service/impl/LoginServiceImpl.java` (검증만)
- **의존성**: F1-A.

### PRAFTA-037-F1-D (FE-1) — ForcedPasswordChangePop 신규 + LoginView 분기 추가

- **유형**: frontend-component + frontend-screen
- **영역**: web
- **모듈**: login
- **목적**: 강제 변경 팝업 신설 + LoginView 응답 핸들러에 PASSWORD_CHANGE 분기 추가.
- **산출물**:
  - 신규: `views/login/popup/ForcedPasswordChangePop.vue` (§3.3 골격)
  - 수정: `LoginView.vue` — import, `fnApplyLoginResponse` 분기, 신규 헬퍼 3개(`fnApplyLoginPartialSession`, `fnApplyLoginResponseAfterPwChange`, `fnForcedPwCancel`).
  - 미변경: `PhoneAuthPop.vue`, `MyInfoPop.vue`.
- **핵심 파일**:
  - `prafta-web-frontend/prafta-web-frontend/src/views/login/popup/ForcedPasswordChangePop.vue` (신규)
  - `prafta-web-frontend/prafta-web-frontend/src/views/login/LoginView.vue`
- **사용자 플로우 검증 시나리오**:
  1. 회원가입 직접 가입자 첫 로그인 → 일반 응답 nextStep=PASSWORD_CHANGE → 강제 팝업 → 변경 → 약관 → 메인.
  2. 관리자 생성 계정 → nextStep=PHONE_AUTH → PhoneAuthPop → 통과 응답 nextStep=PASSWORD_CHANGE → 강제 팝업 → 변경 → 메인.
  3. 강제 변경 거부 → confirm OK → 토큰 폐기 + 로그아웃 호출 + 로그인 화면. 재로그인 시 동일 흐름 반복.
  4. 기존 운영 사용자(PWD_CHG_DTIME NULL) → 다음 로그인 시 합류(D-4).
  5. 이미 PW 변경한 사용자 → nextStep=null → 메인 직진.
- **의존성**: F1-A, F1-B.

---

## 5. 의존성 그래프

```
F1-A (LoginResponse 합류)
  ├─→ F1-C (verifyPhoneAuth 합류 검증)
  └─→ F1-D (프론트)
F1-B (동일 PW 거부, USER_400_010)
  └─→ F1-D (USER_400_010 메시지 노출)
```

권장 착수: **F1-A 와 F1-B 병렬 → F1-C 검증 → F1-D**.

---

## 6. 비기능 요구사항

### 6.1 보안

- 강제 변경 거부 시 즉시 토큰 폐기: sessionStorage.clear() + localStorage.removeItem('refreshToken') + userStore.logout() + `POST /comApi/login/logout`(실패해도 클라이언트 폐기 진행).
- 강제 변경 endpoint = 기존 `update-my-passwd` 재사용 — `MyPasswdParam.from` 의 cmpnyCd/userCd 토큰 강제 흡수(IDOR 가드 유지).
- 동일 PW 거부(D-2) — 휴대폰번호=PW 그대로 유지 차단.
- BCrypt 적용 유지(`passwordHasher.hash`). 평문 PW 응답/로그/sessionStorage 저장 금지.
- 강제 변경 팝업 backdrop 클릭으로 닫히지 않음. ESC 처리 미적용 또는 명시적 취소만.

### 6.2 감사 로그 (정책서 §11.3)

- application log 만: `log.info("비밀번호 변경 완료 - userCd={}, 강제={}", userCd, isForced)`. 강제/자발 구분은 PWD_CHG_DTIME 이전값으로 판정(이전 NULL = 강제).
- 별도 감사 테이블 적재는 prafta-037 F5 와 통합.

### 6.3 i18n / 메시지

- 모든 사용자 노출 문구 한국어.
- 신규 백엔드 에러: USER_400_010 = "새 비밀번호는 현재 비밀번호와 동일할 수 없습니다."
- 프론트 인라인 한국어 또는 messages 키: 모달 타이틀/안내/취소 confirm. 성공 alert 는 기존 `MSG.MY_INFO_PW_CHANGED` 재사용 권장.

### 6.4 트랜잭션

- 기존 `updateMyPw` 단일 SQL 모델 그대로. 동일 PW 가드 추가는 영향 없음.

### 6.5 화면 UX (정책서 §13.3)

- 강제 변경 팝업 backdrop/ESC 닫힘 차단.
- 취소 confirm 으로 의도 재확인.
- 변경 성공 alert 1회 후 자동 라우팅.
- 인라인 `forced-notice` 로 변경 필요성 설명.

---

## 7. 미해결 / Follow-up 후보

| # | 항목 | 비고 |
|---|---|---|
| F1-FU1 | PW 만료 정책(90일 후 강제) | PWD_CHG_DTIME 활용. nextStep=PASSWORD_EXPIRED 패턴 확장. |
| F1-FU2 | 이전 PW 재사용 금지(N개 비교) | 신규 `tb_user_pwd_history` 또는 컬럼 필요. |
| F1-FU3 | 앱(F3 합류 후) 강제 PW 변경 | D-5. F3 완료 후 별도 PR. |
| F1-FU4 | PW 변경 감사 테이블 적재 | F5 와 통합. |
| F1-FU5 | PW 정책 강화(8자/특수문자 필수) | 기존 사용자 영향 큼. 정책 결정 필요. |
| F1-FU6 | 기존 사용자 PWD_CHG_DTIME NULL 운영 공지 | D-4. 운영 적용 전 사용자 사전 공지(이메일/공지) 권장. |
| F1-FU7 | SSO 도입 시 강제 변경 흐름 | 본 작업은 ID/PW 전제. SSO 도입 시 분기 재설계. |
| F1-FU8 | 변경 후 모든 디바이스 강제 로그아웃 옵션 | D-3 은 단일 디바이스 유지. PW 침해 우려 큰 경우 옵션 추가 가능. |

---

**최종 작성**: 2026-05-29 — planner 분해 완료. 본 문서는 후속 작업(developer/qa) 의 단일 출처(SSOT).
