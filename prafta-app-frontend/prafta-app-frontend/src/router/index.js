// src/router/index.js
import { createRouter, createWebHashHistory } from 'vue-router'
import { buildDynamicRoutes } from './dynamicRoutes'
import axios from '@/api/axios'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/login/LoginView.vue') },
  { path: '/MainView', name: 'MainView', component: () => import('@/views/main/MainView.vue') },

  // 로그인 전 노출 화면들 (퍼블릭)
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
  {
    path: '/ChkLst',
    name: 'ChkLst',
    component: () => import('@/views/chkLst/ChkLst.vue'),
  },
  { path: '/JoinUser', name: 'JoinUser', component: () => import('@/views/login/JoinUser.vue') },

  // catch-all
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

/** 퍼블릭 허용 경로 목록 (정확 일치 또는 prefix 허용 필요 시 startsWith로 체크) */
const publicPaths = [
  '/',
  '/MainView',
  '/ActInfoSrch',
  '/TermsInfo',
  '/TermsDetail',
  'QrScanner',
  '/JoinUser',
]

/** DB에서 메뉴 조회 (예시 API) */
async function fetchAppRoutes() {
  try {
    const response = await axios.post('/comApi/baseinfo/getAppMenuList', { menuSrc: '002' })
    if (response.status === 200) return response.data || []
  } catch (err) {
    console.error('메뉴 조회 실패:', err)
  }
  return []
}

/** DB 결과를 라우트로 변환하여 등록 */
async function injectDynamicRoutes() {
  if (dynamicInjected) return
  const dbRoutes = await fetchAppRoutes()
  if (!dbRoutes || dbRoutes.length === 0) {
    dynamicInjected = true
    return
  }

  const dynamicRoutes = buildDynamicRoutes(dbRoutes)

  // 동적 라우트 등록: 개발 정책에 맞춰 분류(예: login/* 은 루트에, main/* 은 MainView 하위) 등
  dynamicRoutes.forEach((r) => {
    // 기본적으로 동적 라우트는 인증 필요(true) 로 표기
    if (!r.meta) r.meta = {}
    if (typeof r.meta.requiresAuth === 'undefined') r.meta.requiresAuth = true

    // 여기서는 route.path이 '/...' 기준으로 만들어졌다고 가정
    // 예: 로그인 전 노출(특정 경로 패턴)이 있다면 meta.requiresAuth = false로 변경 가능
    // 예시: if (r.viewPath?.startsWith('login/')) r.meta.requiresAuth = false

    // 라우터에 추가 (이 프로젝트는 루트 레벨에 추가)
    if (!router.hasRoute(r.name)) {
      router.addRoute(r) // 최상위로 등록
      console.log('동적라우트 추가:', r.path, r.name, 'requiresAuth:', r.meta.requiresAuth)
    }
  })

  dynamicInjected = true
}

/** beforeEach 가드 */
router.beforeEach(async (to, from, next) => {
  // 1) 동적 라우트 주입 보장
  if (!dynamicInjected) {
    await injectDynamicRoutes()
  }

  // 2) 퍼블릭 경로인지 우선 체크 (정확 일치 또는 prefix 체크 필요 시 변경)
  const isPublic = publicPaths.some((p) => {
    // 정확 일치 또는 p가 prefix인 경우 허용 (예: '/MainView' 는 '/MainView/...' 도 허용)
    if (p === to.path) return true
    if (to.path.startsWith(p + '/')) return true
    return false
  })

  if (isPublic) {
    // 퍼블릭은 로그인 여부와 무관하게 접근 허용
    return next()
  }

  // 3) 퍼블릭이 아닌 경우: 토큰 확인
  const token = sessionStorage.getItem('token')
  if (!token) {
    // 로그인 페이지로 리디렉트하되, 원래 가려던 경로를 쿼리로 보존해서 로그인 후 이동 가능하게 함
    return next({ path: '/', query: { redirect: to.fullPath } })
  }

  // 4) 라우트의 meta.requiresAuth 검사(동적 라우트에 따라 세밀히 분기 가능)
  if (to.meta?.requiresAuth === false) {
    return next()
  }

  // 5) 그 외 정상 접근 허용
  return next()
})

export default router
