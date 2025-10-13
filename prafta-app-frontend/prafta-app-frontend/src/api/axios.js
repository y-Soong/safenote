import axios from 'axios'
import { useLoadingStore } from '@/stores/loadingStore'
import { useUserStore } from '@/stores/userStore'
import router from '@/router'
import { $alert } from '@/utils/alertUtil'

// ✅ Vue CLI에서는 process.env.NODE_ENV 사용
const baseURL =
  process.env.NODE_ENV === 'production'
    ? '/prafta' // 운영 서버 (배포 시 nginx 등에서 프록시됨)
    : '/prafta' // 개발 서버 (vue.config.js proxy 사용)

const api = axios.create({
  baseURL, // ✅ 프록시를 통해 HTTP → HTTPS 자동 변환
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
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

    if (config.method === 'get') {
      config.params = { ...(config.params || {}), ...userInfo }
    } else if (!Array.isArray(config.data)) {
      config.data = { ...(config.data || {}), ...userInfo }
    }

    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    useLoadingStore().stopLoading()
    return Promise.reject(error)
  }
)

// 응답 인터셉터
api.interceptors.response.use(
  (response) => {
    useLoadingStore().stopLoading()
    return response
  },
  (error) => {
    useLoadingStore().stopLoading()
    const userStore = useUserStore()

    if (error.status === '404' && error.response?.data?.message === '유효하지 않은 토큰입니다.') {
      $alert(error.response.data.message)
      sessionStorage.clear()
      userStore.logout()
      router.push('/')
      return new Promise(() => {}) // 요청 중단
    }

    return Promise.reject(error)
  }
)

export default api
