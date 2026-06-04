// src/api/axios.js (APP)
// web 프론트(@/api/axios)와 동일한 통신/세션 정책을 사용한다.
// 차이점(APP 고유): X-Client-Type=APP, gv_deviceId 전송(디바이스 바인딩), 해시 라우터 이동.
import axios from 'axios'
import { useLoadingStore } from '@/stores/loadingStore'
import { useUserStore } from '@/stores/userStore'
import { $alert } from '@/utils/alertUtil'
import { resolveBaseURL } from '@/api/baseUrl'
import { refreshAccessToken, forceLogout } from '@/composables/useAuth'

// 순환참조 방지: router는 지연 import (router/index.js가 본 모듈을 import 하므로)
let routerRef
const getRouter = async () => {
  if (!routerRef) {
    const mod = await import('@/router/index.js')
    routerRef = mod.default
  }
  return routerRef
}

/**
 * Device ID 가져오기 (APP 고유 — localStorage에 영속, 로그아웃에도 유지).
 * 백엔드 인증/리프레시/로그아웃 흐름이 gv_deviceId를 사용한다.
 *
 * prafta-com-003 C4: 네이티브 deviceId 우선 — utils/deviceBridge.requestDeviceInfo() 가
 * 네이티브(Flutter) ANDROID_ID/IDFV 를 받으면 localStorage('gv_deviceId')를 그 값으로
 * 덮어쓴다. 따라서 여기서 localStorage 값을 그대로 읽으면 네이티브값이 우선되고,
 * 브리지 미동작(웹 디버그)/취득 실패 시에는 기존 클라 생성 UUID 폴백이 유지된다.
 */
function getDeviceId() {
  if (typeof window === 'undefined') return ''

  const STORAGE_KEY = 'gv_deviceId'
  let deviceId = localStorage.getItem(STORAGE_KEY)

  if (!deviceId) {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      deviceId = crypto.randomUUID()
    } else {
      deviceId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0
        const v = c === 'x' ? r : (r & 0x3) | 0x8
        return v.toString(16)
      })
    }
    localStorage.setItem(STORAGE_KEY, deviceId)
  }
  return deviceId
}

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
})

/**
 * 401 응답이 토큰 자체의 문제로 발생한 것인지 판단.
 * - 서버가 errorCode를 반환했다면 AUTH_* 또는 COMMON_400_600만 토큰 에러로 본다.
 * - errorCode가 없으면 보수적으로 토큰 에러로 간주한다 (기존 동작 유지).
 */
function isTokenError(errorCode) {
  return !errorCode || errorCode === 'COMMON_400_600' || String(errorCode).startsWith('AUTH_')
}

/** 강제 로그아웃 + 로그인 페이지 이동 (인터셉터 내부에서 일관 사용). */
async function forceLogoutAndRedirect(userStore) {
  await forceLogout()
  try {
    userStore.logout()
  } catch (e) {
    // store 미초기화 등은 무시
    console.warn('[AXIOS] store logout skip:', e?.message)
  }
  // APP은 해시 라우터 → push('/') 는 '#/' 로 이동
  ;(await getRouter()).push('/')
}

// 요청 인터셉터
// - 정책 §11.1에 따라 휴대폰(gv_mblNo) / 이메일(gv_email)은 요청 파라미터에 포함하지 않는다.
// - 외부 IP 조회(ipify.org) 호출은 제거되었다. 클라이언트 IP는 백엔드가 HttpServletRequest에서 추출한다.
api.interceptors.request.use(
  async (config) => {
    const loadingStore = useLoadingStore()
    loadingStore.startLoading()

    const userInfo = {
      gv_cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      gv_userCd: sessionStorage.getItem('gv_userCd'),
      gv_userId: sessionStorage.getItem('gv_userId'),
      gv_userNm: sessionStorage.getItem('gv_userNm'),
      gv_siteCd: sessionStorage.getItem('gv_siteCd'),
      gv_siteNo: sessionStorage.getItem('gv_siteNo'),
      gv_siteNm: sessionStorage.getItem('gv_siteNm'),
      gv_nodeCd: sessionStorage.getItem('gv_nodeCd'),
      gv_nodeNm: sessionStorage.getItem('gv_nodeNm'),
      gv_authCd: sessionStorage.getItem('gv_authCd'),
      gv_authLevel: sessionStorage.getItem('gv_authLevel'),
      gv_deviceId: getDeviceId(), // APP 고유
    }

    const method = (config.method || 'get').toLowerCase()
    const isFormData = (v) => typeof FormData !== 'undefined' && v instanceof FormData

    if (method === 'get') {
      config.params = { ...(config.params || {}), ...userInfo }
    } else {
      if (isFormData(config.data)) {
        Object.entries(userInfo).forEach(([k, v]) => {
          if (v != null) config.data.append(k, v)
        })
        if (config.headers) {
          if (String(config.headers['Content-Type'] || '').includes('application/json')) {
            delete config.headers['Content-Type']
          }
        }
      } else if (!Array.isArray(config.data)) {
        config.data = { ...(config.data || {}), ...userInfo }
      }
    }

    // 토큰은 sessionStorage 기준
    config.headers = config.headers || {}
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // clientType 헤더는 "항상" 붙인다 (백엔드 정책 통일). APP은 APP 고정.
    config.headers['X-Client-Type'] = 'APP'

    if (typeof window !== 'undefined' && window.location?.protocol === 'file:') {
      console.debug('[AXIOS file://] =>', config.method?.toUpperCase(), config.baseURL, config.url)
    }

    return config
  },
  (error) => {
    try {
      useLoadingStore().stopLoading()
    } catch (err) {
      console.error('[AXIOS] error log :', err)
    }
    return Promise.reject(error)
  },
)

// 응답 인터셉터
api.interceptors.response.use(
  (response) => {
    try {
      useLoadingStore().stopLoading()
    } catch (err) {
      console.error('[AXIOS] error log :', err)
    }
    return response
  },
  async (error) => {
    try {
      useLoadingStore().stopLoading()
    } catch (err) {
      console.error('[AXIOS] error log :', err)
    }

    const userStore = useUserStore()
    const status = error?.response?.status
    const originalRequest = error?.config
    const errorCode = error?.response?.data?.errorCode

    // COMMON_400_003 → 세션 만료 등 서버가 명시적으로 로그아웃 요구
    if (errorCode === 'COMMON_400_003') {
      await forceLogoutAndRedirect(userStore)
      return Promise.reject(error)
    }

    const tokenError = isTokenError(errorCode)

    // 재시도(_retry) 후에도 401이면 토큰 자체가 무효 → 강제 로그아웃
    if (status === 401 && tokenError && originalRequest?._retry) {
      await forceLogoutAndRedirect(userStore)
      return Promise.reject(error)
    }

    // 401 → refresh → retry
    if (status === 401 && tokenError && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true

      // refresh 자체에서 401이 다시 나면 즉시 로그아웃 (루프 방지)
      const reqUrl = String(originalRequest.url || '')
      if (reqUrl.includes('/auth/refresh') || reqUrl.includes('/comApi/auth/refresh')) {
        await forceLogoutAndRedirect(userStore)
        return Promise.reject(error)
      }

      try {
        // useAuth의 단일 잠금/큐를 통해 refresh
        const newToken = await refreshAccessToken()

        api.defaults.headers.common.Authorization = `Bearer ${newToken}`

        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (e) {
        await forceLogoutAndRedirect(userStore)
        return Promise.reject(e)
      }
    }

    // 기존 호환: 404 + "유효하지 않은 토큰입니다." 메시지
    if (status === 404 && error?.response?.data?.message === '유효하지 않은 토큰입니다.') {
      $alert(error.response.data.message)
      await forceLogoutAndRedirect(userStore)
      return new Promise(() => {})
    }

    console.error(
      '[AXIOS][ERROR]',
      status,
      error?.message,
      error?.config?.method,
      error?.config?.url,
    )
    return Promise.reject(error)
  },
)

export default api
