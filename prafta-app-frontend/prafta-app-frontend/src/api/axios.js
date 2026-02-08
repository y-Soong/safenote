// src/api/axios.js (APP)
import axios from 'axios'
import { useLoadingStore } from '@/stores/loadingStore'
import { useUserStore } from '@/stores/userStore'
import { $alert } from '@/utils/alertUtil'

/**
 * baseURL 결정 규칙
 */
function resolveBaseURL() {
  const cfg = (typeof window !== 'undefined' && window.__APP_CONFIG__) || {}
  if (cfg.API_BASE) {
    const context = cfg.CONTEXT ?? '/prafta'
    return `${cfg.API_BASE}${context}`
  }

  if (typeof window !== 'undefined' && window.location?.protocol === 'file:') {
    const env = (typeof process !== 'undefined' && process.env) || {}
    const apiBase = env.VUE_APP_FILE_API_BASE || 'http://172.30.1.4:8080'
    const context = env.VUE_APP_API_CONTEXT || '/prafta'
    return `${apiBase}${context}`
  }

  return '/prafta'
}

/**
 * 클라이언트 IP 주소를 가져옵니다 (외부 API 사용, sessionStorage 캐싱)
 */
async function getClientIP() {
  const cachedIP = sessionStorage.getItem('clientIP')
  if (cachedIP) return cachedIP

  try {
    const response = await axios.get('https://api.ipify.org?format=json', {
      timeout: 3000,
    })
    const ip = response.data?.ip
    if (ip) {
      sessionStorage.setItem('clientIP', ip)
      return ip
    }
  } catch (error) {
    console.warn('[AXIOS] Failed to get client IP:', error.message)
  }
  return null
}

/**
 * Device ID 가져오기
 */
function getDeviceId() {
  if (typeof window === 'undefined') return null

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

// ✅ refresh 전용(인터셉터 없는) 인스턴스
const plain = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
})

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
})

// ------------------------------
// Refresh control (401 handling)
// ------------------------------
let isRefreshing = false
let refreshQueue = []

function resolveQueue(error, newToken = null) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(newToken)
  })
  refreshQueue = []
}

async function requestRefresh() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) throw new Error('NO_REFRESH_TOKEN')

  console.log(refreshToken)

  // ⚠️ 실제 서버 경로에 맞춰 필요하면 변경
  const res = await plain.post('/comApi/auth/refresh', { refreshToken })

  const newToken = res.data?.token
  if (!newToken) throw new Error('NO_TOKEN_IN_REFRESH_RESPONSE')
  return newToken
}

function hardLogout(message) {
  try {
    if (message) $alert(message)
  } catch (err) {
    console.error('[AXIOS] error log :', err)
  }

  sessionStorage.clear()
  localStorage.removeItem('refreshToken')

  try {
    const userStore = useUserStore()
    userStore.logout()
  } catch (err) {
    console.error('[AXIOS] error log :', err)
  }

  // ✅ APP(해시 라우터)라 router import 없이도 강제 이동 가능
  // 로그인 경로가 "/" 이므로 "#/" 로 보냄
  if (typeof window !== 'undefined') {
    window.location.hash = '#/'
  }
}

// 요청 인터셉터
api.interceptors.request.use(
  async (config) => {
    const loadingStore = useLoadingStore()
    loadingStore.startLoading()

    const clientIP = await getClientIP()
    const deviceId = getDeviceId()

    const userInfo = {
      gv_cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      gv_userId: sessionStorage.getItem('gv_userId'),
      gv_userNm: sessionStorage.getItem('gv_userNm'),
      gv_siteCd: sessionStorage.getItem('gv_siteCd'),
      gv_siteNo: sessionStorage.getItem('gv_siteNo'),
      gv_siteNm: sessionStorage.getItem('gv_siteNm'),
      gv_authCd: sessionStorage.getItem('gv_authCd'),
      gv_mblNo: sessionStorage.getItem('gv_mblNo'),
      gv_email: sessionStorage.getItem('gv_email'),
      gv_clientType: 'APP', // ✅ APP로 고정
      gv_clientIP: clientIP,
      gv_deviceId: deviceId,
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

    // ✅ 헤더
    config.headers = config.headers || {}
    config.headers['X-Client-Type'] = 'APP' // ✅ 서버가 clientType 정책 쓰는 경우 중요

    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

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
  }
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

    const status = error?.response?.status
    const msg = error?.response?.data?.message
    const originalRequest = error?.config

    // ✅ 백엔드가 404로 "유효하지 않은 토큰"을 주는 경우도 인증오류로 취급
    const isAuthError =
      status === 401 ||
      (status === 404 && msg === '유효하지 않은 토큰입니다.') ||
      (status === 401 && msg === '유효하지 않은 토큰입니다.')

    if (isAuthError && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true

      // refresh 요청 자체에서 실패하면 루프 방지
      if (String(originalRequest.url || '').includes('/auth/refresh')) {
        hardLogout('세션이 만료되었습니다. 다시 로그인해주세요.')
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({
            resolve: (newToken) => {
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${newToken}`
              resolve(api(originalRequest))
            },
            reject,
          })
        })
      }

      isRefreshing = true

      try {
        const newToken = await requestRefresh()

        sessionStorage.setItem('token', newToken)
        api.defaults.headers.common.Authorization = `Bearer ${newToken}`

        resolveQueue(null, newToken)

        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (e) {
        resolveQueue(e, null)
        hardLogout('세션이 만료되었습니다. 다시 로그인해주세요.')
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }

    // 그 외 에러
    console.error(
      '[AXIOS][ERROR]',
      status,
      error?.message,
      error?.config?.method,
      error?.config?.url
    )
    return Promise.reject(error)
  }
)

export default api
