<!--
  ForcedPasswordChangeView.vue — 강제 비밀번호 변경 게이트 (모바일 앱)
  - 작업 ID: prafta-app-033-3
  - 웹 ForcedPasswordChangePop 대응. 첫 로그인(PWD_CHG_DTIME IS NULL, nextStep='PASSWORD_CHANGE') 또는
    게이트 강제 로그아웃 후 재로그인 시 진입한다(정식 토큰 보유 상태).
  - 비번 변경 EP 는 기존 PUT /appApi/mypage/password 재사용(성공 시 PWD_CHG_DTIME=NOW() 설정 → 게이트 해소).
  - 성공 → routeAfterLogin(약관 게이트 → MainView). 취소/뒤로가기 → 확인 후 로그아웃 → 로그인 복귀.
  - 디자인 토큰/폼은 PasswordChangeView(010-12) 세트를 재사용. 이탈 가드는 TermsAgreeView 패턴.
-->
<template>
  <div class="forced-pwd-view">
    <!-- 헤더 (뒤로 = 변경 거부 → 로그아웃) -->
    <header class="pw-hd">
      <button type="button" class="pw-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-fpw-chev-left" />
        </svg>
      </button>
      <h1 class="pw-hd__title">비밀번호 변경</h1>
      <span class="pw-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="pw-body">
      <!-- 안내 노트 -->
      <div class="pw-notice">
        안전한 서비스 이용을 위해 비밀번호를 변경해 주세요.<br />
        변경 후 서비스를 계속 이용할 수 있어요.
      </div>

      <!-- 현재 비밀번호 -->
      <div class="pw-field">
        <label class="pw-field__label" for="fpwCurrent">현재 비밀번호</label>
        <div class="pw-input-wrap">
          <input
            id="fpwCurrent"
            v-model="currentPassword"
            :type="showCurrent ? 'text' : 'password'"
            class="pw-input"
            autocomplete="current-password"
            placeholder="현재 비밀번호"
          />
          <button
            type="button"
            class="pw-eye"
            aria-label="비밀번호 표시"
            @click="showCurrent = !showCurrent"
          >
            <svg class="icon" width="20" height="20" aria-hidden="true">
              <use href="#i-fpw-eye" />
            </svg>
          </button>
        </div>
        <p v-if="currentError" class="pw-helper pw-helper--danger">{{ currentError }}</p>
      </div>

      <!-- 새 비밀번호 -->
      <div class="pw-field">
        <label class="pw-field__label" for="fpwNew">새 비밀번호</label>
        <div class="pw-input-wrap">
          <input
            id="fpwNew"
            v-model="newPassword"
            :type="showNew ? 'text' : 'password'"
            class="pw-input"
            autocomplete="new-password"
            maxlength="15"
            placeholder="새 비밀번호 (6~15자)"
          />
          <button
            type="button"
            class="pw-eye"
            aria-label="비밀번호 표시"
            @click="showNew = !showNew"
          >
            <svg class="icon" width="20" height="20" aria-hidden="true">
              <use href="#i-fpw-eye" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 새 비밀번호 확인 -->
      <div class="pw-field">
        <label class="pw-field__label" for="fpwConfirm">새 비밀번호 확인</label>
        <div class="pw-input-wrap">
          <input
            id="fpwConfirm"
            v-model="confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            class="pw-input"
            autocomplete="new-password"
            maxlength="15"
            placeholder="새 비밀번호 확인"
          />
          <button
            type="button"
            class="pw-eye"
            aria-label="비밀번호 표시"
            @click="showConfirm = !showConfirm"
          >
            <svg class="icon" width="20" height="20" aria-hidden="true">
              <use href="#i-fpw-eye" />
            </svg>
          </button>
        </div>
        <p v-if="confirmMismatch" class="pw-helper pw-helper--danger">비밀번호가 일치하지 않아요</p>
      </div>

      <!-- 규칙 가이드 (010-21 재사용) -->
      <PasswordRuleGuide :rules="passwordRules" />
    </main>

    <!-- 푸터 -->
    <footer class="pw-footer">
      <button
        type="button"
        class="pw-submit"
        :class="{ 'pw-submit--off': !canSubmit }"
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        변경하기
      </button>
    </footer>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="pw-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-fpw-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-fpw-eye"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" />
          <circle cx="12" cy="12" r="3" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'

import api from '@/api/axios'
import { forceLogout } from '@/composables/useAuth'
import { useUserStore } from '@/stores/userStore'
import { routeAfterLogin } from '@/utils/termsGate'

import PasswordRuleGuide from '@/views/mypage/components/PasswordRuleGuide.vue'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(TermsAgreeView 패턴 동일).
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = (message) => {
  if (proxy?.$confirm) return proxy.$confirm(message)
  return Promise.resolve(window.confirm(message))
}

// ───────────────────────────────────────────────────────────
// 입력 상태
// ───────────────────────────────────────────────────────────
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

const currentError = ref('')

// 변경 완료/로그아웃 후 라우트 이탈 가드 우회 플래그.
let bypassGuard = false
// 변경 성공 후 진행할 목적지(history state 로 전달; 없으면 /MainView).
const redirect = ref('/MainView')

onMounted(() => {
  const state = window.history.state || {}
  if (state.redirect) redirect.value = state.redirect
})

// ───────────────────────────────────────────────────────────
// 규칙 충족 판정 (PasswordChangeView 와 동일: 6~15자 + 숫자/영문/특수 중 2종 이상)
// ───────────────────────────────────────────────────────────
const passwordRules = computed(() => {
  const v = newPassword.value || ''
  const lengthMet = v.length >= 6 && v.length <= 15
  const typeCount = [/[0-9]/.test(v), /[a-zA-Z]/.test(v), /[^a-zA-Z0-9]/.test(v)].filter(
    Boolean,
  ).length
  return [
    { key: 'length', label: '길이 6~15자', met: lengthMet },
    { key: 'mix', label: '숫자·영문·특수 중 2종 이상', met: typeCount >= 2 },
  ]
})

// 전체 게이트는 회원가입과 동일한 공통 유틸로 판정(중복 구현 방지).
const allRulesMet = computed(() => {
  const v = newPassword.value || ''
  if (!v) return false
  if (proxy?.$util?.validatePasswordRule) return proxy.$util.validatePasswordRule(v)
  return passwordRules.value.every((r) => r.met)
})
const confirmMismatch = computed(
  () => confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value,
)
const sameAsCurrent = computed(
  () => newPassword.value.length > 0 && newPassword.value === currentPassword.value,
)

const canSubmit = computed(
  () =>
    currentPassword.value.length > 0 &&
    allRulesMet.value &&
    !confirmMismatch.value &&
    confirmPassword.value.length > 0 &&
    !sameAsCurrent.value,
)

// ───────────────────────────────────────────────────────────
// 액션
// ───────────────────────────────────────────────────────────
const isSubmitting = ref(false)

// 변경하기: PUT /appApi/mypage/password 재사용(성공 시 서버가 PWD_CHG_DTIME=NOW() 설정 → 게이트 해소).
//   성공 후 routeAfterLogin 으로 약관 게이트 → MainView 후속 진행.
const onSubmit = async () => {
  if (!canSubmit.value || isSubmitting.value) return
  isSubmitting.value = true
  currentError.value = ''
  try {
    await api.put('/appApi/mypage/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    bypassGuard = true
    await routeAfterLogin(router, redirect.value)
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    if (errorCode === 'INVALID_CURRENT_PASSWORD') {
      currentError.value = '현재 비밀번호가 일치하지 않아요.'
    } else if (errorCode === 'SAME_AS_CURRENT') {
      currentError.value = '현재 비밀번호와 다른 비밀번호를 사용해 주세요.'
    } else if (errorCode === 'PASSWORD_RULE_VIOLATION') {
      await showAlert('비밀번호 규칙을 다시 확인해 주세요.')
    } else {
      await showAlert(
        e?.response?.data?.message || '비밀번호 변경에 실패했어요. 잠시 후 다시 시도해 주세요.',
      )
    }
  } finally {
    isSubmitting.value = false
  }
}

// 변경 거부/취소 → 로그아웃 후 로그인 화면 복귀(미변경 상태로 진입 불가).
const fnLogout = async () => {
  await forceLogout()
  try {
    userStore.logout()
  } catch (e) {
    console.warn('[ForcedPwd] userStore logout skip:', e?.message)
  }
}

const onBack = async () => {
  const ok = await askConfirm(
    '비밀번호를 변경하지 않으면 서비스를 이용할 수 없어요.\n로그아웃하고 로그인 화면으로 돌아갈까요?',
  )
  if (!ok) return
  await fnLogout()
  bypassGuard = true
  router.replace('/')
}

// 뒤로가기/라우트 이탈 가드 — 변경/통과/로그아웃이 아닌 이탈은 로그아웃 후 허용.
onBeforeRouteLeave(async (to, from, next) => {
  if (bypassGuard) {
    next()
    return
  }
  const ok = await askConfirm(
    '비밀번호를 변경하지 않으면 서비스를 이용할 수 없어요.\n로그아웃하고 로그인 화면으로 돌아갈까요?',
  )
  if (ok) {
    await fnLogout()
    bypassGuard = true
    next({ path: '/' })
  } else {
    next(false)
  }
})
</script>

<style scoped>
.forced-pwd-view {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-info: #3b82f6;
  --color-info-strong: #1d4ed8;
  --color-info-tint: #eff6ff;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-md: 10px;
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
.pw-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.pw-hd__back {
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
.pw-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.pw-hd__spacer {
  width: 44px;
}

/* 본문 */
.pw-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 88px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 안내 노트 */
.pw-notice {
  padding: var(--space-md);
  background: var(--color-info-tint);
  color: var(--color-info-strong);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
}

/* 입력 필드 */
.pw-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.pw-field__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.pw-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.pw-input {
  flex: 1;
  height: 46px;
  padding: 0 44px 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
}
.pw-input:focus {
  border-color: var(--color-primary);
}
.pw-eye {
  position: absolute;
  right: 4px;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-tertiary);
}
.pw-helper {
  margin: 0;
  font-size: 12px;
}
.pw-helper--danger {
  color: var(--color-danger);
}

/* 푸터 */
.pw-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-md) var(--space-lg);
}
.pw-submit {
  width: 100%;
  height: 48px;
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.pw-submit--off {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.pw-sprite {
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
