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

// ── 중복 제출 가드 (앱 전체 공통) ──────────────────────────────
// 저장 버튼 연타 등으로 동일한 변경 요청(POST/PUT/PATCH/DELETE)이 동시에 중복 전송되어
// 같은 데이터가 여러 번 insert/update 되는 것을 막는다.
//   - 동일 (method+url+body) 요청이 "처리 중(in-flight)"이면 이후 중복 요청은 서버로 보내지 않고
//     즉시 취소한다(아래 응답 인터셉터에서 영구 보류 Promise 로 흡수 → 호출부에 에러가 노출되지 않음).
//   - 첫 요청이 끝나면(성공/실패/취소) 키를 해제하므로, 사용자가 의도적으로 다시 저장하는 것은 정상 동작한다.
//   - FormData(사진 첨부 저장 등 multipart)도 가드 대상에 포함한다. 파일은 파일명이 매 호출
//     생성될 수 있으므로 "크기"로만 식별해, 같은 본문(동일 폼 + 동일 사진) 연타를 중복 제거한다.
const inFlightMutations = new Set()
const MUTATING_METHODS = new Set(['post', 'put', 'patch', 'delete'])

function buildDedupKey(config) {
  const method = (config.method || 'get').toLowerCase()
  if (!MUTATING_METHODS.has(method)) return null
  const data = config.data
  let bodyKey = ''
  try {
    if (typeof FormData !== 'undefined' && data instanceof FormData) {
      // FormData: 텍스트 필드는 값 그대로, 파일(Blob/File)은 크기로만 식별(파일명 변동 무시).
      const parts = []
      for (const [k, v] of data.entries()) {
        if (typeof Blob !== 'undefined' && v instanceof Blob) {
          parts.push(`${k}=@${v.size}`)
        } else {
          parts.push(`${k}=${v}`)
        }
      }
      bodyKey = 'FD:' + parts.join('&')
    } else {
      bodyKey = data == null ? '' : JSON.stringify(data)
    }
  } catch (e) {
    // 직렬화 불가(순환참조/이터레이션 실패 등) → 본문 키 생략(method+url 로만 dedup)
    bodyKey = '[unserializable]'
  }
  return `${method}:${config.url || ''}:${bodyKey}`
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

// 보강 033-002: 병렬 요청이 동시에 403/토큰에러를 받아 forceLogoutAndRedirect 가 다발 호출될 때
//   알림 모달 중첩 / 서버 로그아웃 / 네비게이션이 중복 실행되지 않도록 하는 단일 플라이트 가드(웹 axios 동일 패턴).
//   로그아웃 흐름이 끝나면 다시 null 로 풀어 다음 세션 만료에도 정상 동작하게 한다.
let loggingOut = null

/** 강제 로그아웃 + 로그인 페이지 이동 (인터셉터 내부에서 일관 사용, 단일 플라이트). */
function forceLogoutAndRedirect(userStore) {
  // 이미 로그아웃 진행 중이면(동시 403/401 다발 등) 진행 중인 흐름을 그대로 공유한다.
  if (loggingOut) return loggingOut

  loggingOut = (async () => {
    try {
      await forceLogout()
      try {
        userStore?.logout()
      } catch (e) {
        // store 미초기화 등은 무시
        console.warn('[AXIOS] store logout skip:', e?.message)
      }
      // APP은 해시 라우터 → push('/') 는 '#/' 로 이동
      try {
        await (await getRouter()).push('/')
      } catch (e) {
        // 동일 라우트/네비게이션 취소 등은 무시
      }
    } finally {
      loggingOut = null
    }
  })()

  return loggingOut
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

    // 중복 제출 가드: 동일 변경요청이 처리 중이면 이 중복 요청은 보내지 않고 즉시 취소한다.
    //   (취소 에러는 응답 인터셉터에서 영구 보류 Promise 로 흡수되어 호출부 catch 가 동작하지 않는다.)
    const dedupKey = buildDedupKey(config)
    if (dedupKey) {
      if (inFlightMutations.has(dedupKey)) {
        // AbortController 미지원 구형 webview 등에서는 가드를 적용하지 않고 그대로 보낸다(안전 폴백).
        if (typeof AbortController !== 'undefined') {
          config.__dedupDuplicate = true
          const controller = new AbortController()
          config.signal = controller.signal
          controller.abort()
        }
      } else {
        inFlightMutations.add(dedupKey)
        config.__dedupKey = dedupKey
      }
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
    // 중복 제출 가드: 처리 완료된 변경요청의 in-flight 키를 해제한다.
    if (response?.config?.__dedupKey) {
      inFlightMutations.delete(response.config.__dedupKey)
    }
    return response
  },
  async (error) => {
    try {
      useLoadingStore().stopLoading()
    } catch (err) {
      console.error('[AXIOS] error log :', err)
    }

    // 중복 제출 가드:
    //   (1) 중복으로 취소된 요청은 호출부에 에러를 전파하지 않고 조용히 흡수한다(영구 보류 Promise).
    //   (2) 실제 변경요청이 실패/취소된 경우 in-flight 키를 먼저 해제해, 401 refresh-retry 및
    //       이후의 정상 재시도가 "중복"으로 오인되어 막히지 않도록 한다.
    if (error?.config?.__dedupDuplicate) {
      return new Promise(() => {})
    }
    if (error?.config?.__dedupKey) {
      inFlightMutations.delete(error.config.__dedupKey)
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

    // DAILYLOGIN_403_001 → 관리자가 일용직 계정을 비활성화함(로그인 이후 게이트 차단).
    //   안내 후 강제 로그아웃. 출퇴근 등 모든 후속 요청이 이 코드로 차단되므로 즉시 세션 종료된다.
    if (errorCode === 'DAILYLOGIN_403_001') {
      try {
        await $alert(
          error?.response?.data?.message ||
            '관리자가 계정을 비활성화했습니다. 관리자에게 문의해 주세요.',
        )
      } catch (_e) {
        // alert 실패는 무시하고 로그아웃 진행
      }
      await forceLogoutAndRedirect(userStore)
      return Promise.reject(error)
    }

    // prafta-app-033: AUTH_403_001 → 이용약관 동의 / 강제 비밀번호 변경 미완료.
    //   403 이라 아래 401 refresh 분기에 해당하지 않으므로 여기서 명시 분기한다(refresh 미시도).
    //   안내 후 강제 로그아웃 → 재로그인 시 약관 동의 / 비밀번호 변경 화면이 떠서 해소된다.
    //   사용자 안내는 서버가 내려준 정확한 사유 메시지를 그대로 1회만 노출한다(내부 용어 "게이트" 미노출).
    if (errorCode === 'AUTH_403_001') {
      // 보강 033-002: 이미 로그아웃이 진행 중이면(병렬 403 다발) 알림을 중복 표시하지 않는다.
      if (!loggingOut) {
        try {
          await $alert(
            error?.response?.data?.message ||
              '이용약관 동의 또는 비밀번호 변경이 필요합니다.\n다시 로그인해 주세요.',
          )
        } catch (_e) {
          // alert 실패는 무시하고 로그아웃 진행
        }
      }
      await forceLogoutAndRedirect(userStore)
      // 보강 033-003: 로그아웃/리다이렉트 중이므로 호출부 catch("조회 실패" 등)가 안내 위에 중복 토스트되지
      //   않도록 영구 보류 Promise 를 반환한다(웹 axios 동일). 단일 플라이트 가드와 함께 동작.
      return new Promise(() => {})
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
