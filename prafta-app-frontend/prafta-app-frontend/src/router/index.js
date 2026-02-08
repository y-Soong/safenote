// src/router/index.js (APP)
import { createRouter, createWebHashHistory } from 'vue-router'
import { buildDynamicRoutes } from './dynamicRoutes'
import api from '@/api/axios'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/login/LoginView.vue') },
  { path: '/MainView', name: 'MainView', component: () => import('@/views/main/MainView.vue') },

  // 퍼블릭
  {
    path: '/ActInfoSrch',
    name: 'ActInfoSrch',
    component: () => import('@/views/login/ActInfoSrch.vue'),
  },
  { path: '/TermsInfo', name: 'TermsInfo', component: () => import('@/views/login/TermsInfo.vue') },
  {
    path: '/TermsDetail',
    name: 'TermsDetail',
    component: () => import('@/views/login/TermsDetail.vue'),
  },
  {
    path: '/QrScanner',
    name: 'QrScanner',
    component: () => import('@/views/_common/QrScanner.vue'),
  },
  { path: '/ChkLst', name: 'ChkLst', component: () => import('@/views/chkLst/ChkLst.vue') },
  { path: '/Risk_01', name: 'Risk_01', component: () => import('@/views/risk/Risk_01.vue') },
  { path: '/JoinUser', name: 'JoinUser', component: () => import('@/views/login/JoinUser.vue') },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/_common/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

let dynamicInjected = false

/** 퍼블릭 허용 경로 목록 */
const publicPaths = [
  '/',
  // '/MainView', // 너 정책상 public로 열어둔 상태 유지 (원하면 제거 가능)
  '/ActInfoSrch',
  '/TermsInfo',
  '/TermsDetail',
  '/QrScanner',
  '/JoinUser',
]

// ✅ refresh 동시 호출 방지
let bootstrapping = null

async function ensureAccessToken() {
  const token = sessionStorage.getItem('token')
  if (token) return token

  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  if (!bootstrapping) {
    bootstrapping = (async () => {
      try {
        const res = await api.post('/comApi/auth/refresh', { refreshToken })
        const newToken = res.data?.token
        if (newToken) {
          sessionStorage.setItem('token', newToken)
          api.defaults.headers.common.Authorization = `Bearer ${newToken}`
        }
        return newToken || null
      } catch (e) {
        sessionStorage.clear()
        localStorage.removeItem('refreshToken')
        return null
      } finally {
        bootstrapping = null
      }
    })()
  }

  return bootstrapping
}

/** DB에서 메뉴 조회 */
async function fetchAppRoutes() {
  try {
    const response = await api.post('/comApi/baseinfo/getAppMenuList', { menuSrc: '002' })
    if (response.status === 200) return response.data || []
  } catch (err) {
    console.error('메뉴 조회 실패:', err)
  }
  return []
}

/** 동적 라우트 주입 */
async function injectDynamicRoutes() {
  if (dynamicInjected) return

  const dbRoutes = await fetchAppRoutes()
  if (!dbRoutes || dbRoutes.length === 0) {
    dynamicInjected = true
    return
  }

  const dynamicRoutes = buildDynamicRoutes(dbRoutes)

  dynamicRoutes.forEach((r) => {
    if (!r.meta) r.meta = {}
    if (typeof r.meta.requiresAuth === 'undefined') r.meta.requiresAuth = true

    if (!router.hasRoute(r.name)) {
      router.addRoute(r)
      console.log('동적라우트 추가:', r.path, r.name, 'requiresAuth:', r.meta.requiresAuth)
    }
  })

  dynamicInjected = true
}

/** beforeEach */
router.beforeEach(async (to, from, next) => {
  // ✅ 이미 로그인 상태인데 로그인 화면(/)으로 가려 하면 메인으로 보내기 (replace)
  if (to.path === '/') {
    const ensured = await ensureAccessToken() // 세션 토큰 없으면 refresh로 확보 시도
    if (ensured) {
      // redirect 쿼리가 있으면 그쪽으로, 없으면 메인
      const target = to.query?.redirect || '/MainView'
      return next({ path: target, replace: true })
    }
    // 토큰 없으면 그냥 로그인 화면 허용
    return next()
  }

  // 1) 퍼블릭 경로인지 체크
  const isPublic = publicPaths.some((p) => {
    if (p === to.path) return true
    if (to.path.startsWith(p + '/')) return true
    return false
  })

  if (isPublic) {
    // 퍼블릭 접근 허용
    if (!dynamicInjected) await injectDynamicRoutes()
    return next()
  }

  // 2) 퍼블릭이 아니면 토큰 확보 시도(새로고침/웹뷰 재로딩 대응)
  const ensured = await ensureAccessToken()
  if (!ensured) {
    return next({ path: '/', query: { redirect: to.fullPath } })
  }

  // 3) 토큰 확보 후 동적 라우트 주입
  if (!dynamicInjected) {
    await injectDynamicRoutes()
  }

  // 4) meta.requiresAuth 세밀 분기(필요시)
  if (to.meta?.requiresAuth === false) return next()

  return next()
})

export default router
