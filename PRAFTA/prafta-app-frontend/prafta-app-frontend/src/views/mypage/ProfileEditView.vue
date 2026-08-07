<!--
  ProfileEditView.vue — 개인정보 수정 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-11 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A011
  - planner 라운드 스코프: 3그룹(소속 읽기전용/기본/연락처) + 푸터 (template/style)
  - developer 라운드 스코프(아래 TODO): 복호화 프리필(010-01b), 저장(010-02), 휴대폰 인증(010-03), 라우팅
  - ⚠️ D1 확정: PII 프리필(휴대폰/이메일/생년월일)은 메인 마스킹 응답이 아니라
    수정 진입 전용 복호화 엔드포인트 GET /appApi/mypage/profile/edit(010-01b)로 채운다.
    복호화 응답은 캐시 no-store, 폼 로컬 ref만 유지(store 영속화 금지 — webview 평문 잔존 방지).
  - ⚠️ D6 확정: 성별은 SYS004 시스템코드(100:남성/200:여성, NULL=선택안함). systCode 조회는
    앱 회원가입(JoinUser) 패턴 재사용 권장(하드코딩 옵션을 동적 목록으로 교체 가능).
  - 디자인 토큰: MyLeaveSummaryView 세트를 .profile-edit-view 루트에 1회 선언.
-->
<template>
  <div class="profile-edit-view">
    <!-- 헤더 -->
    <header class="pe-hd">
      <button type="button" class="pe-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-pe-chev-left" />
        </svg>
      </button>
      <h1 class="pe-hd__title">개인정보 수정</h1>
      <span class="pe-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="pe-body">
      <div v-if="isLoading" class="pe-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 그룹 1: 소속 정보 (읽기 전용) -->
        <section class="pe-section">
          <div class="pe-section__head">
            <p class="pe-section__title">소속 정보</p>
            <span class="pe-section__hint">수정은 관리자 문의</span>
          </div>
          <div class="pe-readonly">
            <div class="pe-readonly__row">
              <span class="pe-readonly__label">아이디</span>
              <span class="pe-readonly__value">{{ userId }}</span>
            </div>
            <div class="pe-readonly__row">
              <span class="pe-readonly__label">사업장</span>
              <span class="pe-readonly__value">{{ siteNm }}</span>
            </div>
            <div class="pe-readonly__row">
              <span class="pe-readonly__label">소속부서</span>
              <span class="pe-readonly__value">{{ nodeNm }}</span>
            </div>
            <div class="pe-readonly__row">
              <span class="pe-readonly__label">입사일</span>
              <span class="pe-readonly__value">{{ hireDateText }}</span>
            </div>
          </div>
        </section>

        <!-- 그룹 2: 기본 정보 (수정 가능) -->
        <section class="pe-section">
          <p class="pe-section__title">기본 정보</p>
          <div class="pe-field">
            <label class="pe-field__label" for="peUserNm">이름</label>
            <input
              id="peUserNm"
              v-model="userNm"
              type="text"
              class="pe-input"
              maxlength="50"
              placeholder="이름"
            />
          </div>
          <div class="pe-field">
            <label class="pe-field__label" for="peGender">성별</label>
            <!-- D6 확정: SYS004 코드 100:남성/200:여성, 빈값='' → 서버 저장 시 NULL.
                 developer: genderOptions 를 SYS004 systCode 동적 조회로 교체 가능(JoinUser 패턴). -->
            <select id="peGender" v-model="genderCode" class="pe-input">
              <option value="">선택 안 함</option>
              <option value="100">남성</option>
              <option value="200">여성</option>
            </select>
          </div>
          <div class="pe-field">
            <label class="pe-field__label" for="peBirth">생년월일</label>
            <DateStepperField v-model="birthDate" placeholder="생년월일 선택" />
          </div>
        </section>

        <!-- 그룹 3: 연락처 (수정 가능 + 휴대폰 인증) -->
        <section class="pe-section">
          <p class="pe-section__title">연락처</p>
          <!-- 휴대폰 입력 + 인증요청/확인 (010-20). 발송/검증은 앱 전용 /appApi/mypage/mobile/*(D4) -->
          <MobileVerificationField
            ref="mobileFieldRef"
            v-model="mblNo"
            :verified="mobileVerified"
            @request-code="onRequestMobileCode"
            @verify-code="onVerifyMobileCode"
          />
          <div class="pe-field">
            <label class="pe-field__label" for="peEmail">이메일</label>
            <input
              id="peEmail"
              v-model="email"
              type="email"
              class="pe-input"
              placeholder="이메일 (선택)"
            />
          </div>
        </section>

        <!-- 마지막 로그인 메타 -->
        <p class="pe-last-login">마지막 로그인 · {{ lastLoginText }}</p>
      </template>
    </main>

    <!-- 푸터 — F-10 규약: 왼쪽=진행/확정(저장), 오른쪽=이탈(취소), 폭 균등.
         저장은 파괴적이지 않으므로 primary(위험 액션만 danger). -->
    <footer class="pe-footer">
      <button
        type="button"
        class="pe-btn pe-btn--primary"
        :class="{ 'pe-btn--off': !canSave }"
        :disabled="!canSave"
        @click="onSave"
      >
        저장
      </button>
      <button type="button" class="pe-btn pe-btn--ghost" @click="onCancel">취소</button>
    </footer>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="pe-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-pe-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { useUserStore } from '@/stores/userStore'

import MobileVerificationField from './components/MobileVerificationField.vue'
import DateStepperField from '@/components/common/DateStepperField.vue'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance() || { proxy: null }

// 자식 컴포넌트(휴대폰 인증 필드) 참조 — startCountdown/setVerifyError 호출용(defineExpose)
const mobileFieldRef = ref(null)

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 상태 (developer: 응답 주입/검증/리셋 보완 필요)
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// 읽기 전용 (010-01b 진입 응답)
const userId = ref('')
const siteNm = ref('')
const nodeNm = ref('')
const hireDate = ref('') // YYYYMMDD
const lastLoginDtime = ref('')

// 수정 가능
const userNm = ref('')
const genderCode = ref('') // D6: SYS004 '100'/'200'/'' (빈값=NULL)
const birthDate = ref('') // date input: YYYY-MM-DD
const mblNo = ref('')
const email = ref('')

// 휴대폰 인증 상태 (010-03)
const mobileVerified = ref(false)
const mobileVerificationToken = ref('')
// 진입 시 원본 휴대폰값 (변경 감지용) — developer가 응답으로 주입
const originalMblNo = ref('')

// ───────────────────────────────────────────────────────────
// 파생 표시값 (단순 포맷 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
const hireDateText = computed(() => formatYmd(hireDate.value))
const lastLoginText = computed(() => lastLoginDtime.value || '-')

const formatYmd = (ymd) => {
  if (!ymd || ymd.length !== 8) return '-'
  return `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
}

// 휴대폰이 변경되었는지 (변경 시 인증 필요)
const mobileChanged = computed(() => mblNo.value !== originalMblNo.value)

// 저장 가능: 휴대폰 변경 시 인증 완료여야 함. 그 외 검증은 developer가 보완.
const canSave = computed(() => {
  if (mobileChanged.value && !mobileVerified.value) return false
  // 단순 필수: 이름 trim 1자 이상 (간단 validation — 허용 범위)
  if (!userNm.value || !userNm.value.trim()) return false
  return true
})

// ───────────────────────────────────────────────────────────
// 휴대폰 인증 이벤트 (010-20 → 010-03, 앱 전용 D4)
// ───────────────────────────────────────────────────────────
const onRequestMobileCode = async (phone) => {
  // D4: 앱 전용 발송 엔드포인트(comApi verify-phone-auth 호출 금지).
  if (!phone || phone.length < 10) {
    showAlert('휴대폰 번호를 정확히 입력해 주세요.')
    return
  }
  try {
    await api.post('/appApi/mypage/mobile/request-verification', { mblNo: phone })
    // 성공 시 자식 카운트다운 시작(인증번호 입력 활성화).
    mobileFieldRef.value?.startCountdown()
    // API 통신 성공 시에만 발송 안내(실패 분기에서는 띄우지 않음).
    showAlert('인증번호가 발송됐습니다.')
  } catch (e) {
    const code = e?.response?.data?.errorCode
    const map = {
      MOBILE_DUP: '이미 다른 계정에서 사용 중인 번호예요.',
      INVALID_MOBILE: '휴대폰 번호 형식이 올바르지 않아요.',
      TOO_MANY_ATTEMPTS: '요청이 너무 많아요. 잠시 후 다시 시도해 주세요.',
      // SMS-PPURIO-08: 문자 게이트웨이 발송 실패(뿌리오 연동).
      SMS_502_001: '인증번호 발송에 실패했어요. 잠시 후 다시 시도해 주세요.',
      SMS_502_002: '인증번호 발송이 거절됐어요. 잠시 후 다시 시도해 주세요.',
      SMS_502_003: '인증번호 발송 서버 인증에 실패했어요. 관리자에게 문의해 주세요.',
    }
    showAlert(
      map[code] ||
        e?.response?.data?.message ||
        '인증번호 발송에 실패했어요. 잠시 후 다시 시도해 주세요.',
    )
  }
}
const onVerifyMobileCode = async ({ phone, code }) => {
  // D4: 앱 전용 검증 엔드포인트. 성공 시 단발성 verificationToken 만 반환(로그인 토큰 미발급).
  try {
    const { data } = await api.post('/appApi/mypage/mobile/verify', {
      mblNo: phone,
      verificationCode: code,
    })
    if (data?.verified) {
      mobileVerified.value = true
      mobileVerificationToken.value = data?.verificationToken || ''
    } else {
      mobileFieldRef.value?.setVerifyError('인증번호가 올바르지 않아요.')
    }
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    const map = {
      INVALID_CODE: '인증번호가 올바르지 않아요.',
      EXPIRED: '인증번호가 만료되었어요. 다시 요청해 주세요.',
      TOO_MANY_ATTEMPTS: '시도 횟수를 초과했어요. 다시 요청해 주세요.',
    }
    mobileFieldRef.value?.setVerifyError(
      map[errorCode] || e?.response?.data?.message || '인증에 실패했어요.',
    )
  }
}

// ───────────────────────────────────────────────────────────
// 푸터 액션
// ───────────────────────────────────────────────────────────
const onCancel = () => {
  // 변경 사항 폐기 + 마이페이지 메인 복귀
  router.push('/MyPage')
}

// 저장 진행 중 (중복 제출 방지)
const isSaving = ref(false)

// date input(YYYY-MM-DD) → 서버 저장 형식(YYYYMMDD). 빈값은 빈 문자열.
const toYmd8 = (dateStr) => (dateStr ? String(dateStr).replace(/-/g, '') : '')

const onSave = async () => {
  if (!canSave.value || isSaving.value) return
  isSaving.value = true
  try {
    const body = {
      userNm: userNm.value.trim(),
      genderCode: genderCode.value, // '100'/'200'/'' (빈값 → 서버 NULL)
      birthDate: toYmd8(birthDate.value),
      email: email.value.trim(),
    }
    // 휴대폰은 변경된 경우에만 동봉(인증 토큰 포함). 미변경 시 서버가 기존값 유지.
    if (mobileChanged.value) {
      body.mblNo = mblNo.value
      body.mobileVerificationToken = mobileVerificationToken.value
    }

    await api.put('/appApi/mypage/profile', body)

    // 이름이 바뀌면 세션/스토어의 gv_userNm 을 갱신(아바타·헤더 즉시 반영).
    sessionStorage.setItem('gv_userNm', body.userNm)
    try {
      userStore.gv_userNm = body.userNm
    } catch (e) {
      console.warn('[ProfileEdit] userStore userNm 갱신 skip:', e?.message)
    }

    await showAlert('저장되었습니다.')
    router.push('/MyPage')
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    const fieldMsg = {
      INVALID_USER_NM: '이름을 정확히 입력해 주세요.',
      INVALID_GENDER: '성별 값이 올바르지 않아요.',
      INVALID_BIRTH_DATE: '생년월일이 올바르지 않아요.',
      INVALID_EMAIL: '이메일 형식이 올바르지 않아요.',
      INVALID_MOBILE: '휴대폰 번호 형식이 올바르지 않아요.',
      MOBILE_VERIFICATION_REQUIRED: '휴대폰 본인인증을 먼저 완료해 주세요.',
      MOBILE_VERIFICATION_INVALID: '휴대폰 인증 정보가 만료되었어요. 다시 인증해 주세요.',
      MOBILE_DUP: '이미 다른 계정에서 사용 중인 번호예요.',
    }
    // 휴대폰 인증 관련 오류는 인증 상태를 초기화해 재인증을 유도.
    if (
      errorCode === 'MOBILE_VERIFICATION_REQUIRED' ||
      errorCode === 'MOBILE_VERIFICATION_INVALID'
    ) {
      mobileVerified.value = false
      mobileVerificationToken.value = ''
    }
    showAlert(
      fieldMsg[errorCode] ||
        e?.response?.data?.message ||
        '저장에 실패했어요. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    isSaving.value = false
  }
}

const onBack = () => {
  router.push('/MyPage')
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회. D1 확정: 수정 진입 전용 복호화 엔드포인트 사용.
// ───────────────────────────────────────────────────────────
// 서버 birthDate(YYYYMMDD) → date input(YYYY-MM-DD). 빈값/형식불일치는 빈 문자열.
const toDateInput = (ymd) => {
  if (!ymd || String(ymd).length !== 8) return ''
  const s = String(ymd)
  return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`
}

onMounted(async () => {
  // D1: 메인 마스킹 응답이 아니라 수정 진입 전용 복호화 엔드포인트(010-01b)를 호출한다.
  //   복호화 응답은 store 영속화 금지 — 폼 로컬 ref 에만 유지(webview 평문 잔존 방지).
  try {
    const { data } = await api.get('/appApi/mypage/profile/edit')
    // 읽기 전용
    userId.value = data?.userId || ''
    siteNm.value = data?.siteNm || ''
    nodeNm.value = data?.nodeNm || ''
    hireDate.value = data?.hireDate || ''
    lastLoginDtime.value = data?.lastLoginDtime || ''
    // 수정 가능(복호화 전체값 프리필)
    userNm.value = data?.userNm || ''
    genderCode.value = data?.genderCode || '' // '100'/'200'/null → ''
    birthDate.value = toDateInput(data?.birthDate)
    email.value = data?.email || ''
    mblNo.value = data?.mblNo || ''
    // 변경 감지 기준값(휴대폰 변경 시에만 인증 필요)
    originalMblNo.value = data?.mblNo || ''
  } catch (e) {
    console.warn('[ProfileEdit] 프로필 조회 실패:', e?.message)
    await showAlert('정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
    router.push('/MyPage')
    return
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.profile-edit-view {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.pe-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.pe-hd__back {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.pe-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.pe-hd__spacer {
  width: 44px;
}

/* 본문 */
.pe-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 88px;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.pe-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 섹션 */
.pe-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.pe-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.pe-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.pe-section__hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 읽기 전용 그룹 (회색 bg) */
.pe-readonly {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.pe-readonly__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 0 var(--space-md);
  border-bottom: 1px solid var(--color-border-light);
}
.pe-readonly__row:last-child {
  border-bottom: 0;
}
.pe-readonly__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.pe-readonly__value {
  font-size: 14px;
  color: var(--color-text-primary);
}

/* 입력 필드 */
.pe-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.pe-field__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.pe-input {
  height: 46px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
}
.pe-input:focus {
  border-color: var(--color-primary);
}

/* 마지막 로그인 메타 */
.pe-last-login {
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 푸터 */
.pe-footer {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-sm);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-md) var(--space-lg);
}
.pe-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  border: 0;
}
/* F-9: flex 고정폭(0 0 96px) 제거 → .pe-btn 의 flex:1 상속으로 좌우 균등 */
.pe-btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.pe-btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.pe-btn--off {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.pe-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
