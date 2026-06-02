<template>
  <div class="phone-auth-page">
    <!-- 상단 헤더 (뒤로가기 + 임시 토큰 잔여시간) -->
    <div class="header">
      <button @click="fnCancel" class="back-btn" aria-label="뒤로가기">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <span class="ttl-timer" v-if="ttlSeconds > 0">
        남은 시간 {{ formattedTtl }}
      </span>
    </div>

    <!-- 본문 -->
    <div class="content">
      <h2 class="title">휴대폰 본인인증</h2>

      <p class="notice">
        관리자가 생성한 계정은 첫 로그인 시 본인인증이 필요합니다.<br />
        등록된 휴대폰번호로 인증을 진행해 주세요.
      </p>

      <!-- 휴대폰 번호 입력 -->
      <div class="field">
        <label class="field-label" for="mblNo">휴대폰 번호</label>
        <div class="input-with-action">
          <input
            id="mblNo"
            ref="mblNoFcs"
            v-model="mblNo"
            type="tel"
            placeholder="휴대폰번호 11자리"
            maxlength="13"
            :disabled="verified"
            @blur="focusKill"
            class="form-input"
          />
          <button
            class="btn btn-primary"
            @click="fnSendSms"
            :disabled="resendTimer > 0 || verified"
          >
            {{ resendTimer > 0 ? `${resendTimer}초 후 재요청` : '인증요청' }}
          </button>
        </div>
      </div>

      <!-- 인증번호 입력 -->
      <div class="field">
        <label class="field-label" for="certNo">인증번호</label>
        <div class="input-with-action">
          <input
            id="certNo"
            ref="certNoFcs"
            v-model="certNo"
            type="tel"
            placeholder="인증번호 6자리"
            maxlength="6"
            :disabled="!authReqSent || verified"
            class="form-input"
          />
          <button
            class="btn btn-primary"
            @click="fnVerify"
            :disabled="!authReqSent || verified"
          >
            확인
          </button>
          <span class="ok-mark" v-show="verified">✅</span>
        </div>
      </div>
    </div>

    <!-- 하단 -->
    <div class="footer">
      <button class="btn btn-secondary" @click="fnCancel">취소</button>
    </div>
  </div>
</template>

<script setup>
// PRAFTA-037-F3: 앱(webview) 인증대기 분기 화면.
// 라우트 진입 시 history.state.phoneAuthToken / cmpnyCd 를 받아 sessionStorage.token 에 일시 점유.
// 인증 성공 시 정식 LoginResponse 를 sessionStorage / userStore 에 적용 후 MainView 로 이동.
// 취소 / 만료 / 뒤로가기 시 임시 토큰 폐기 + 로그인 화면 복귀.

import { ref, computed, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import axios from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()

// ───────────── 상태 ─────────────
const cmpnyCd = ref('')
const mblNo = ref('')
const mblNoFcs = ref(null)
const certNo = ref('')
const certNoFcs = ref(null)
const authReqSent = ref(false)
const verified = ref(false)

// 인증번호 재발송 카운트다운(60초)
const resendTimer = ref(0)
let resendInterval = null

// 임시 토큰 잔여시간 (JWT exp 디코딩, 10분)
const ttlSeconds = ref(0)
let ttlInterval = null
const formattedTtl = computed(() => {
  const m = String(Math.floor(ttlSeconds.value / 60)).padStart(2, '0')
  const s = String(ttlSeconds.value % 60).padStart(2, '0')
  return `${m}:${s}`
})

// 화면 이탈 직전 정리 완료 표시 (중복 cleanup 방지)
let cleanedUp = false

// ───────────── 라이프사이클 ─────────────
onMounted(async () => {
  const state = window.history.state || {}
  const phoneAuthToken = state.phoneAuthToken
  const initCmpnyCd = state.cmpnyCd

  if (!phoneAuthToken || !initCmpnyCd) {
    // 새로고침 등으로 history.state 가 소실된 경우 — 의도된 보안 동작(임시 토큰 보호).
    await proxy.$alert('인증 정보가 없습니다.\n다시 로그인해 주세요.')
    cleanedUp = true
    router.replace('/')
    return
  }

  // 임시 토큰을 sessionStorage 에 일시 점유. axios 인터셉터가 자동 부착.
  sessionStorage.setItem('token', phoneAuthToken)
  cmpnyCd.value = initCmpnyCd

  // JWT exp 디코딩으로 잔여시간 산출
  const expSec = decodeJwtExp(phoneAuthToken)
  if (expSec > 0) {
    const nowSec = Math.floor(Date.now() / 1000)
    ttlSeconds.value = Math.max(0, expSec - nowSec)
    ttlInterval = setInterval(() => {
      ttlSeconds.value = ttlSeconds.value - 1
      if (ttlSeconds.value <= 0) {
        clearInterval(ttlInterval)
        ttlInterval = null
        fnExpire()
      }
    }, 1000)
  }

  if (mblNoFcs.value) mblNoFcs.value.focus()
})

onBeforeUnmount(() => {
  if (resendInterval) {
    clearInterval(resendInterval)
    resendInterval = null
  }
  if (ttlInterval) {
    clearInterval(ttlInterval)
    ttlInterval = null
  }
})

// 뒤로가기 / 라우트 이탈 가드
onBeforeRouteLeave(async (to, from, next) => {
  if (cleanedUp || verified.value) {
    next()
    return
  }
  const ok = await proxy.$confirm('본인인증을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?')
  if (ok) {
    fnCleanupToken()
    next()
  } else {
    next(false)
  }
})

// ───────────── 메서드 ─────────────
function decodeJwtExp(token) {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return 0
    // base64url → base64
    let payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    while (payload.length % 4 !== 0) payload += '='
    const json = atob(payload)
    const obj = JSON.parse(json)
    return typeof obj.exp === 'number' ? obj.exp : 0
  } catch (e) {
    return 0
  }
}

const fnCleanupToken = () => {
  sessionStorage.removeItem('token')
  if (axios.defaults && axios.defaults.headers && axios.defaults.headers.common) {
    delete axios.defaults.headers.common.Authorization
  }
  if (resendInterval) {
    clearInterval(resendInterval)
    resendInterval = null
  }
  if (ttlInterval) {
    clearInterval(ttlInterval)
    ttlInterval = null
  }
  cleanedUp = true
}

const fnExpire = async () => {
  await proxy.$alert('인증 시간이 만료되었습니다.\n다시 로그인해 주세요.')
  fnCleanupToken()
  router.replace('/')
}

const fnSendSms = async () => {
  const phone = (mblNo.value || '').replace(/-/g, '')
  let valid = false
  if (proxy.$util && typeof proxy.$util.validatePhoneNumber === 'function') {
    valid = proxy.$util.validatePhoneNumber(mblNo.value)
  } else {
    valid = /^010\d{7,8}$/.test(phone)
  }
  if (!valid) {
    await proxy.$alert('휴대폰번호를 올바르게 입력해 주세요.')
    if (mblNoFcs.value) mblNoFcs.value.focus()
    return
  }

  try {
    await axios.post('/comApi/baseinfo/sms-auth-sends', {
      cmpnyCd: cmpnyCd.value,
      mblNo: phone,
    })
    authReqSent.value = true
    await proxy.$alert('인증번호가 발송되었습니다.\n6자리 인증번호를 입력해 주세요.')
    if (certNoFcs.value) certNoFcs.value.focus()

    // 재발송 60초 카운트다운
    if (resendInterval) clearInterval(resendInterval)
    resendTimer.value = 60
    resendInterval = setInterval(() => {
      resendTimer.value = resendTimer.value - 1
      if (resendTimer.value <= 0) {
        clearInterval(resendInterval)
        resendInterval = null
      }
    }, 1000)
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, '인증번호 발송 중 오류가 발생했습니다.'))
  }
}

const fnVerify = async () => {
  const code = (certNo.value || '').trim()
  if (!code) {
    await proxy.$alert('인증번호를 입력해 주세요.')
    return
  }
  if (code.length !== 6) {
    await proxy.$alert('인증번호 6자리를 입력해 주세요.')
    return
  }

  try {
    const phone = (mblNo.value || '').replace(/-/g, '')
    const response = await axios.post('/comApi/login/verify-phone-auth', {
      mblNo: phone,
      certNo: code,
    })
    if (response.status === 200) {
      verified.value = true
      fnApplyLoginResponse(response.data)
    }
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, '인증에 실패했습니다.'))
  }
}

const fnApplyLoginResponse = (data) => {
  // 웹 LoginView.fnSubmitLogin 정상 분기와 동일 구조.
  // 정책 §11.1: 휴대폰/이메일은 응답에 없으며 sessionStorage/store 에 보관하지 않는다.
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

  // 라우트 이탈 가드가 차단하지 않도록.
  cleanedUp = true
  if (resendInterval) {
    clearInterval(resendInterval)
    resendInterval = null
  }
  if (ttlInterval) {
    clearInterval(ttlInterval)
    ttlInterval = null
  }

  router.replace('/MainView')
}

const fnCancel = async () => {
  const ok = await proxy.$confirm('본인인증을 취소하면 로그인 화면으로 돌아갑니다.\n계속하시겠습니까?')
  if (!ok) return
  fnCleanupToken()
  router.replace('/')
}

function focusKill(e) {
  if (e.target.id === 'mblNo') {
    if (proxy.$util && typeof proxy.$util.formatPhoneNumber === 'function') {
      mblNo.value = proxy.$util.formatPhoneNumber(mblNo.value)
    } else {
      // 폴백: 숫자만 추출 후 010-XXXX-XXXX / 010-XXX-XXXX 포맷.
      const digits = (mblNo.value || '').replace(/\D+/g, '')
      if (digits.length === 11) {
        mblNo.value = `${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}`
      } else if (digits.length === 10) {
        mblNo.value = `${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}`
      }
    }
  }
}
</script>

<style scoped>
.phone-auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-sizing: border-box;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  margin-top: 1rem;
}

.back-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #1f1f1f;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn svg {
  width: 1.75rem;
  height: 1.75rem;
}

.ttl-timer {
  font-size: 0.85rem;
  color: #ef4444;
  font-weight: 600;
}

.content {
  flex: 1;
  padding: 0 1.5rem;
}

.title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1f1f1f;
  margin: 1rem 0 1.25rem;
}

.notice {
  background: #eff6ff;
  border-radius: 10px;
  padding: 0.75rem 0.875rem;
  font-size: 0.8rem;
  color: #1d4ed8;
  line-height: 1.5;
  margin: 0 0 1.5rem;
}

.field {
  margin-bottom: 1rem;
}

.field-label {
  display: block;
  font-size: 0.85rem;
  color: #4b5563;
  margin-bottom: 0.4rem;
}

.input-with-action {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 0.95rem;
  color: #1f1f1f;
  background: #fff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s;
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-input:focus {
  border-color: #16a34a;
}

.form-input:disabled {
  background: #f9fafb;
  color: #6b7280;
}

.btn {
  padding: 0.7rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #16a34a;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #15803d;
}

.btn-secondary {
  background: #fff;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #f9fafb;
}

.ok-mark {
  font-size: 1rem;
  color: #16a34a;
}

.footer {
  padding: 1rem 1.5rem 1.5rem;
}

.footer .btn {
  width: 100%;
  padding: 0.85rem;
}
</style>
