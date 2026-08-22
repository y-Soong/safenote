<template>
  <div class="default-sch-gate">
    <header class="gate-header">
      <h1 class="gate-title">기본 근무타입 설정</h1>
      <!-- 닫기: 미설정 상태로 로그인 화면 복귀. 임시 토큰은 정리(웹 DefaultSchGatePop 패턴). -->
      <button type="button" class="gate-close" aria-label="닫기" @click="fnCancel">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </header>

    <section class="gate-body">
      <p class="gate-desc">
        서비스 이용을 위해 기본 근무타입을 먼저 설정해야 합니다.
        설정한 근무타입으로 당해 연말까지 평일 근무계획이 자동 생성됩니다.
      </p>

      <label class="gate-field-label" for="appDefaultSchCd">기본 근무타입</label>
      <select
        id="appDefaultSchCd"
        class="gate-select"
        v-model="defaultSchCd"
        :disabled="isLoading || isSaving"
      >
        <option :value="''">선택</option>
        <option v-for="opt in filteredSchTypeOptions" :key="opt.schCd" :value="opt.schCd">
          {{ opt.schNo }} ({{ fnFmtSchTime(opt.fstSchStrTime) }}~{{ fnFmtSchTime(opt.fstSchEndTime) }})
        </option>
      </select>

      <p class="gate-error" v-show="errorMsg">{{ errorMsg }}</p>
    </section>

    <footer class="gate-footer">
      <button
        class="gate-submit"
        :disabled="!defaultSchCd || isSaving || isLoading"
        @click="fnSave"
      >
        설정하고 시작하기
      </button>
    </footer>
  </div>
</template>

<script setup>
// PRAFTA-COM-008-E-8c: 앱(webview) 기본 근무타입 로그인 게이트.
// 라우트 진입 시 history.state.defaultSchToken 으로 scope=DEFAULT_SCH 임시 JWT 수신.
// 설정 저장 성공 시 정식 LoginResponse 를 sessionStorage / userStore 에 적용 후 MainView 로 이동.
// PhoneAuthView 패턴 미러(강제 게이트라 취소 경로는 로그인 화면 복귀).

import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { routeAfterLogin } from '@/utils/termsGate'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

const defaultSchToken = ref('')
const defaultSchCd = ref('')
const schTypeOptions = ref([])
// 반영 시점은 항상 명일(오늘+1, applyDefaultSchChange 규칙) — 적용일이 명일보다 미래인
//   근무타입은 노출하지 않는다(2026-08-22, 최종 판정은 서버 isValidDefaultSch).
const tomorrowYmd = (() => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
})()
const filteredSchTypeOptions = computed(() =>
  schTypeOptions.value.filter((o) => !o.earliestApplyDate || o.earliestApplyDate <= tomorrowYmd)
)
const isLoading = ref(false)
const isSaving = ref(false)
const errorMsg = ref('')

// 'HHmm' → 'HH:mm' 라벨 포맷.
const fnFmtSchTime = (t) => {
  if (!t || t.length < 4) return t || ''
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`
}

onMounted(async () => {
  const state = window.history.state || {}
  const token = state.defaultSchToken

  if (!token) {
    // 새로고침 등으로 history.state 소실 — 임시 토큰 보호. 로그인 복귀.
    await proxy.$alert('인증 정보가 없습니다.\n다시 로그인해 주세요.')
    router.replace('/')
    return
  }

  defaultSchToken.value = token
  // 임시 토큰 일시 점유 — axios 인터셉터가 자동 부착.
  sessionStorage.setItem('token', token)
  axios.defaults.headers.common.Authorization = `Bearer ${token}`

  await fnLoadOptions()
})

// 공통: confirm 폴백 (앱 전역 $confirm 우선, 없으면 window.confirm)
const askConfirm = async (message) => {
  if (proxy?.$confirm) {
    return await proxy.$confirm(message)
  }
  return window.confirm(message)
}

const fnCancel = async () => {
  const ok = await askConfirm(
    '기본 근무타입을 설정하지 않으면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?'
  )
  if (!ok) return
  // 미설정 취소 — 임시 토큰은 정식 토큰이 아니므로 잔존 금지.
  sessionStorage.removeItem('token')
  delete axios.defaults.headers.common.Authorization
  router.replace('/')
}

const fnLoadOptions = async () => {
  isLoading.value = true
  try {
    const response = await axios.get('/comApi/login/default-sch-options')
    schTypeOptions.value = response.data?.schedules ?? []
    if (schTypeOptions.value.length === 0) {
      errorMsg.value = '선택 가능한 근무타입이 없습니다.\n관리자에게 문의해 주세요.'
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(err, '근무타입 목록 조회 중 오류가 발생했습니다.')
  } finally {
    isLoading.value = false
  }
}

const fnSave = async () => {
  if (!defaultSchCd.value) {
    errorMsg.value = '기본 근무타입을 선택해 주세요.'
    return
  }
  isSaving.value = true
  errorMsg.value = ''
  try {
    const response = await axios.post('/comApi/login/set-default-sch', {
      defaultSchCd: defaultSchCd.value,
    })
    if (response.status === 200) {
      await fnApplyLoginResponse(response.data)
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(err, '기본 근무타입 설정 중 오류가 발생했습니다.')
  } finally {
    isSaving.value = false
  }
}

const fnApplyLoginResponse = async (data) => {
  // 앱 LoginView 정상 로그인 분기와 동일 구조(§11.1: 휴대폰/이메일 미보관).
  const {
    token,
    userCd,
    userId,
    userNm,
    cmpnyCd: respCmpnyCd,
    siteCd,
    siteNo,
    siteNm,
    nodeCd,
    nodeNm,
    authCd,
    authLevel,
    refreshToken,
  } = data

  sessionStorage.setItem('token', token)
  axios.defaults.headers.common.Authorization = `Bearer ${token}`

  sessionStorage.setItem('gv_cmpnyCd', respCmpnyCd)
  sessionStorage.setItem('gv_userCd', userCd)
  sessionStorage.setItem('gv_userId', userId)
  sessionStorage.setItem('gv_userNm', userNm)
  sessionStorage.setItem('gv_siteCd', siteCd)
  sessionStorage.setItem('gv_siteNo', siteNo)
  sessionStorage.setItem('gv_siteNm', siteNm)
  sessionStorage.setItem('gv_nodeCd', nodeCd)
  sessionStorage.setItem('gv_nodeNm', nodeNm)
  sessionStorage.setItem('gv_authCd', authCd)
  sessionStorage.setItem('gv_authLevel', authLevel)
  if (refreshToken) {
    localStorage.setItem('refreshToken', refreshToken)
  }

  userStore.setUser({
    cmpnyCd: respCmpnyCd,
    userCd,
    userId,
    userNm,
    siteCd,
    siteNo,
    siteNm,
    nodeCd,
    nodeNm,
    authCd,
    authLevel,
  })

  // APP-PRAFTA-001: 근무타입 설정 후 곧바로 MainView 로 직행하면 뒤따르는 게이트(강제 비번변경/필수약관)를
  //   건너뛰어, MainView 첫 API 에서 서버 게이트(AUTH_403_001)로 튕기거나 약관 동의가 우회된다.
  //   LoginView 정상 로그인과 동일하게, 정식 토큰 세팅 직후 남은 게이트 체인을 그대로 이어서 태운다.
  //   set-default-sch 응답은 login 과 동일한 LoginResponse.from 이라 nextStep/mustChangePassword 를 함께 내려준다.
  if (data?.nextStep === 'PASSWORD_CHANGE' || data?.mustChangePassword) {
    router.replace({
      path: '/ForcedPasswordChange',
      state: { redirect: '/MainView' },
    })
    return
  }

  // 필수약관 미동의 게이트 → 동의 화면, 없으면 /MainView.
  await routeAfterLogin(router)
}
</script>

<style scoped>
.default-sch-gate {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-bg, #f9fafb);
}
.gate-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px;
}
.gate-title {
  margin: 0;
  font-size: 18px;
  color: var(--color-text-strong, #111827);
}
.gate-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--color-text-muted, #4b5563);
  cursor: pointer;
}
.gate-body {
  flex: 1 1 auto;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.gate-desc {
  color: var(--color-text-muted, #4b5563);
  line-height: 1.5;
}
.gate-field-label {
  color: var(--color-text, #374151);
  font-weight: 600;
}
.gate-select {
  height: 44px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 10px);
  padding: 0 12px;
  background: var(--color-surface, #ffffff);
  color: var(--color-text, #374151);
}
.gate-error {
  color: var(--color-danger, #ef4444);
  white-space: pre-line;
}
.gate-footer {
  padding: 16px;
}
.gate-submit {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  font-size: 15px;
}
.gate-submit:disabled {
  opacity: 0.5;
}
</style>
