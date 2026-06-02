<!--
  PasswordChangeView.vue — 비밀번호 변경 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-12 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A012
  - planner 라운드 스코프: 안내노트 + 3필드 + 규칙가이드 + 푸터 (template/style)
  - developer 라운드 스코프(아래 TODO): 변경 호출(010-04), 라우팅, 서버 에러 매핑
  - 디자인 토큰: MyLeaveSummaryView 세트를 .pwd-change-view 루트에 1회 선언.
-->
<template>
  <div class="pwd-change-view">
    <!-- 헤더 -->
    <header class="pw-hd">
      <button type="button" class="pw-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-pw-chev-left" />
        </svg>
      </button>
      <h1 class="pw-hd__title">비밀번호 변경</h1>
      <span class="pw-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="pw-body">
      <!-- 안내 노트 -->
      <div class="pw-notice">안전한 비밀번호 사용을 위해 3개월마다 변경을 권장합니다</div>

      <!-- 현재 비밀번호 -->
      <div class="pw-field">
        <label class="pw-field__label" for="pwCurrent">현재 비밀번호</label>
        <div class="pw-input-wrap">
          <input
            id="pwCurrent"
            v-model="currentPassword"
            :type="showCurrent ? 'text' : 'password'"
            class="pw-input"
            autocomplete="current-password"
            placeholder="현재 비밀번호"
          />
          <button type="button" class="pw-eye" aria-label="비밀번호 표시" @click="showCurrent = !showCurrent">
            <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-pw-eye" /></svg>
          </button>
        </div>
        <p v-if="currentError" class="pw-helper pw-helper--danger">{{ currentError }}</p>
      </div>

      <!-- 새 비밀번호 -->
      <div class="pw-field">
        <label class="pw-field__label" for="pwNew">새 비밀번호</label>
        <div class="pw-input-wrap">
          <input
            id="pwNew"
            v-model="newPassword"
            :type="showNew ? 'text' : 'password'"
            class="pw-input"
            autocomplete="new-password"
            maxlength="15"
            placeholder="새 비밀번호 (6~15자)"
          />
          <button type="button" class="pw-eye" aria-label="비밀번호 표시" @click="showNew = !showNew">
            <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-pw-eye" /></svg>
          </button>
        </div>
      </div>

      <!-- 새 비밀번호 확인 -->
      <div class="pw-field">
        <label class="pw-field__label" for="pwConfirm">새 비밀번호 확인</label>
        <div class="pw-input-wrap">
          <input
            id="pwConfirm"
            v-model="confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            class="pw-input"
            autocomplete="new-password"
            maxlength="15"
            placeholder="새 비밀번호 확인"
          />
          <button type="button" class="pw-eye" aria-label="비밀번호 표시" @click="showConfirm = !showConfirm">
            <svg class="icon" width="20" height="20" aria-hidden="true"><use href="#i-pw-eye" /></svg>
          </button>
        </div>
        <p v-if="confirmMismatch" class="pw-helper pw-helper--danger">비밀번호가 일치하지 않아요</p>
      </div>

      <!-- 규칙 가이드 (010-21) -->
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
        <symbol id="i-pw-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-pw-eye" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z" />
          <circle cx="12" cy="12" r="3" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'

import PasswordRuleGuide from './components/PasswordRuleGuide.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 입력 상태
// ───────────────────────────────────────────────────────────
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

// 표시 토글 (UI 토글 — 허용)
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)

// 서버 에러 (developer가 주입)
const currentError = ref('')

// ───────────────────────────────────────────────────────────
// 규칙 충족 판정 (단순 form validation — 허용 범위)
// 기존 프로젝트 비밀번호 정책과 정렬: 6~15자 + 숫자/영문/특수 중 2종 이상.
// 판정 기준은 회원가입(JoinUser)과 동일한 $util.validatePasswordRule 을 재사용.
// (common.js: /^.{6,15}$/ + [숫자, 영문, 특수] 중 2종 이상)
// ───────────────────────────────────────────────────────────
const passwordRules = computed(() => {
  const v = newPassword.value || ''
  // 표시용 항목별 판정 (validatePasswordRule 과 동일한 1차 기준 사용)
  const lengthMet = v.length >= 6 && v.length <= 15
  const typeCount = [/[0-9]/.test(v), /[a-zA-Z]/.test(v), /[^a-zA-Z0-9]/.test(v)].filter(
    Boolean
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
  // 폴백: 유틸 미주입 시에도 동일 규칙으로 판정
  return passwordRules.value.every((r) => r.met)
})
const confirmMismatch = computed(
  () => confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value
)
const sameAsCurrent = computed(
  () => newPassword.value.length > 0 && newPassword.value === currentPassword.value
)

// 변경하기 활성: 전 규칙 충족 + 일치 + 현재≠새 + 현재 입력
const canSubmit = computed(
  () =>
    currentPassword.value.length > 0 &&
    allRulesMet.value &&
    !confirmMismatch.value &&
    confirmPassword.value.length > 0 &&
    !sameAsCurrent.value
)

// ───────────────────────────────────────────────────────────
// 액션
// ───────────────────────────────────────────────────────────
// 제출 진행 중 (중복 제출 방지)
const isSubmitting = ref(false)

const onSubmit = async () => {
  if (!canSubmit.value || isSubmitting.value) return
  isSubmitting.value = true
  currentError.value = ''
  try {
    await api.put('/appApi/mypage/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    // 세션은 유지(Q10). 토스트 후 메인 복귀.
    await showAlert('비밀번호가 변경되었습니다.')
    router.push('/MyPage')
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    if (errorCode === 'INVALID_CURRENT_PASSWORD') {
      currentError.value = '현재 비밀번호가 일치하지 않아요.'
    } else if (errorCode === 'SAME_AS_CURRENT') {
      currentError.value = '현재 비밀번호와 다른 비밀번호를 사용해 주세요.'
    } else if (errorCode === 'PASSWORD_RULE_VIOLATION') {
      showAlert('비밀번호 규칙을 다시 확인해 주세요.')
    } else {
      showAlert(e?.response?.data?.message || '비밀번호 변경에 실패했어요. 잠시 후 다시 시도해 주세요.')
    }
  } finally {
    isSubmitting.value = false
  }
}

const onBack = () => {
  router.push('/MyPage')
}
</script>

<style scoped>
.pwd-change-view {
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
