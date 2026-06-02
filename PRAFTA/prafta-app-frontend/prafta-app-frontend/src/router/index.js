// src/router/index.js (APP)
import { createRouter, createWebHashHistory } from 'vue-router'
import { buildDynamicRoutes } from './dynamicRoutes'
import api from '@/api/axios'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/login/LoginView.vue') },
  { path: '/MainView', name: 'MainView', component: () => import('@/views/main/MainView.vue') },

  // prafta-app-002: 내 근태 조회 (오늘/이번주/이번달)
  {
    path: '/MyAttendance',
    name: 'MyAttendance',
    component: () => import('@/views/attd/MyAttendanceView.vue'),
  },

  // prafta-app-004-C4: TBM 입실/종료 (보호 — beforeEach 토큰 게이트). 진입: /TbmEntry?sessionCd=...
  {
    path: '/TbmEntry',
    name: 'TbmEntry',
    component: () => import('@/views/tbm/TbmEntryView.vue'),
  },

  // prafta-app-005: 연차 현황 (본인 잔여연차 상세)
  {
    path: '/MyLeaveSummaryView',
    name: 'MyLeaveSummaryView',
    component: () => import('@/views/leave/MyLeaveSummaryView.vue'),
  },

  // PRAFTA-APP-006: 내 승인 요청 목록 화면
  {
    path: '/MyRequests',
    name: 'MyRequests',
    component: () => import('@/views/req/MyRequestsView.vue'),
  },

  // PRAFTA-APP-007: 근태 요청 폼 (스케줄 수정 / 근태 보정 / 초과근무)
  {
    path: '/AttdRequest',
    name: 'AttdRequest',
    component: () => import('@/views/req/AttdRequestView.vue'),
  },

  // PRAFTA-APP-010: 마이페이지 (인증 필수 — publicPaths 미포함)
  {
    path: '/MyPage',
    name: 'MyPage',
    component: () => import('@/views/mypage/MyPageView.vue'),
  },
  {
    path: '/ProfileEdit',
    name: 'ProfileEdit',
    component: () => import('@/views/mypage/ProfileEditView.vue'),
  },
  {
    path: '/PasswordChange',
    name: 'PasswordChange',
    component: () => import('@/views/mypage/PasswordChangeView.vue'),
  },
  {
    path: '/ApprovalPresetList',
    name: 'ApprovalPresetList',
    component: () => import('@/views/mypage/ApprovalPresetListView.vue'),
  },
  {
    path: '/ApprovalPresetEdit',
    name: 'ApprovalPresetEdit',
    component: () => import('@/views/mypage/ApprovalPresetEditView.vue'),
  },

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
  // prafta-app-011 화면 C: 안전점검 저장 완료 (요약 query 전달)
  {
    path: '/SafetyInspectSaved',
    name: 'SafetyInspectSaved',
    component: () => import('@/views/chkLst/SafetyInspectSavedView.vue'),
  },
  { path: '/Risk_01', name: 'Risk_01', component: () => import('@/views/risk/Risk_01.vue') },

  // prafta-app-012: 아차사고/사건 보고 (인증 필수 — publicPaths 미포함, beforeEach 토큰 게이트)
  //   근로자 보고 / 관리자(안전직군) 목록·상세. 서버가 사업장 권한(assertSiteAccess) 최종 판정.
  {
    path: '/NearMissReport',
    name: 'NearMissReport',
    component: () => import('@/views/nearmiss/NearMissReport.vue'),
  },
  {
    path: '/NearMissManageList',
    name: 'NearMissManageList',
    component: () => import('@/views/nearmiss/NearMissManageList.vue'),
  },
  {
    path: '/NearMissManageDetail',
    name: 'NearMissManageDetail',
    component: () => import('@/views/nearmiss/NearMissManageDetail.vue'),
  },

  { path: '/JoinUser', name: 'JoinUser', component: () => import('@/views/login/JoinUser.vue') },

  // PRAFTA-037-F3: 인증대기(SYS013='04') 계정의 휴대폰 본인인증 화면.
  // 임시 scope=PHONE_AUTH 토큰만으로 SMS 발송/검증 호출. 정식 토큰 부재 → public.
  {
    path: '/PhoneAuth',
    name: 'PhoneAuth',
    component: () => import('@/views/login/PhoneAuthView.vue'),
  },

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
  '/PhoneAuth', // PRAFTA-037-F3: 인증대기 단계는 정식 토큰 미발급 → public 라우트로 취급
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
    // 백엔드 kebab 리팩터 정렬: GET /comApi/baseinfo/app-menu-lists (cmpnyCd/userCd 는 JWT 클레임에서 도출 → 파라미터 불필요)
    // 응답은 { appMenuResultList: [...] } 래핑(빈 목록이면 본문 null)
    const response = await api.get('/comApi/baseinfo/app-menu-lists')
    if (response.status === 200) return response.data?.appMenuResultList || []
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
    // 동적 라우트 주입은 토큰이 있을 때만 — 비로그인 상태에서 /app-menu-lists(@NoAuth 미적용)를
    // 호출하면 AuthAspect 가 토큰 부재로 500 을 던진다(가입하기·약관·인증대기 등 public 경로 진입 시 발생).
    if (!dynamicInjected && sessionStorage.getItem('token')) {
      await injectDynamicRoutes()
    }
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
