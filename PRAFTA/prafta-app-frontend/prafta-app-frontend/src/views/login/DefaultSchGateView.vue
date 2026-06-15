<template>
  <div class="default-sch-gate">
    <header class="gate-header">
      <h1 class="gate-title">기본 근무타입 설정</h1>
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
        <option v-for="opt in schTypeOptions" :key="opt.schCd" :value="opt.schCd">
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

import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

const defaultSchToken = ref('')
const defaultSchCd = ref('')
const schTypeOptions = ref([])
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
      fnApplyLoginResponse(response.data)
    }
  } catch (err) {
    errorMsg.value = resolveApiErrorMessage(err, '기본 근무타입 설정 중 오류가 발생했습니다.')
  } finally {
    isSaving.value = false
  }
}

const fnApplyLoginResponse = (data) => {
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

  router.replace('/MainView')
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
  padding: 20px 16px;
}
.gate-title {
  margin: 0;
  font-size: 18px;
  color: var(--color-text-strong, #111827);
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
