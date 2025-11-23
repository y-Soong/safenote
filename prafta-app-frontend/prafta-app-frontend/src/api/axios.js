// src/api/axios.js
import axios from 'axios'
import { useLoadingStore } from '@/stores/loadingStore'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'
import { $alert } from '@/utils/alertUtil'

/**
 * baseURL 결정 규칙 (Vue CLI 전용)
 * 1) window.__APP_CONFIG__가 있으면 최우선 (assets/vue_app/app-config.json 등에서 주입)
 * 2) file:// 로 열렸다면(Flutter WebView 로컬 자산) 절대 URL 사용
 *    - env: VUE_APP_FILE_API_BASE, VUE_APP_API_CONTEXT
 *    - 없으면 기본값 'http://172.30.1.4:8080' + '/prafta'
 * 3) 그 외(웹서버에서 서비스 중)는 '/prafta' (dev는 proxy, prod는 리버스프록시 전제)
 */
function resolveBaseURL() {
  // 1) 외부 설정 주입
  const cfg = (typeof window !== 'undefined' && window.__APP_CONFIG__) || {}
  if (cfg.API_BASE) {
    const context = cfg.CONTEXT ?? '/prafta'
    return `${cfg.API_BASE}${context}`
  }

  // 2) file:// 환경 (Flutter WebView 가 assets로 여는 경우)
  if (typeof window !== 'undefined' && window.location?.protocol === 'file:') {
    const env = (typeof process !== 'undefined' && process.env) || {}
    const apiBase = env.VUE_APP_FILE_API_BASE || 'http://172.30.1.4:8080' // ← 필요 시 https 로 변경
    const context = env.VUE_APP_API_CONTEXT || '/prafta'
    return `${apiBase}${context}`
  }

  // 3) 웹서버로 서비스되는 경우
  return '/prafta'
}

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
  // headers: {
  //   'Content-Type': 'application/json',
  // },
})

// 요청 인터셉터
api.interceptors.request.use(
  async (config) => {
    const loadingStore = useLoadingStore()
    loadingStore.startLoading()

    const userInfo = {
      gv_cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
      gv_userId: sessionStorage.getItem('gv_userId'),
      gv_userNm: sessionStorage.getItem('gv_userNm'),
      gv_authCd: sessionStorage.getItem('gv_authCd'),
      gv_mblNo: sessionStorage.getItem('gv_mblNo'),
      gv_email: sessionStorage.getItem('gv_email'),
    }

    const method = (config.method || 'get').toLowerCase()

    // ✅ FormData 감지 유틸
    const isFormData = (v) => typeof FormData !== 'undefined' && v instanceof FormData

    if (method === 'get') {
      config.params = { ...(config.params || {}), ...userInfo }
    } else {
      if (isFormData(config.data)) {
        // ✅ FormData면 append로만 추가 (객체로 교체 금지)
        Object.entries(userInfo).forEach(([k, v]) => {
          if (v != null) config.data.append(k, v)
        })
        // 절대 Content-Type 강제로 세팅하지 말 것!
        if (config.headers) {
          // 혹시라도 이전 코드에서 JSON으로 덮어쓴 흔적 제거 (선택)
          if (String(config.headers['Content-Type'] || '').includes('application/json')) {
            delete config.headers['Content-Type']
          }
        }
      } else if (!Array.isArray(config.data)) {
        // ✅ 일반 JSON payload일 때만 merge
        config.data = { ...(config.data || {}), ...userInfo }
      }
    }

    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
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
    } catch {
      console.log(error)
    }
    return Promise.reject(error)
  }
)

// 응답 인터셉터
api.interceptors.response.use(
  (response) => {
    try {
      useLoadingStore().stopLoading()
    } catch {
      console.log(response)
    }
    return response
  },
  (error) => {
    try {
      useLoadingStore().stopLoading()
    } catch {
      console.log(error)
    }

    const userStore = useUserStore()
    const status = error?.response?.status

    // 기존 코드 보정: error.status가 아니라 error.response.status
    if (status === 404 && error?.response?.data?.message === '유효하지 않은 토큰입니다.') {
      $alert(error.response.data.message)
      sessionStorage.clear()
      userStore.logout()
      router.push('/')
      return new Promise(() => {}) // 체인 중단
    }

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
