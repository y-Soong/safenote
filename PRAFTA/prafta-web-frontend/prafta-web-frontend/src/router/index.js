// src/router/index.js
import { createRouter, createWebHistory } from "vue-router";
import { buildDynamicChildren } from "./dynamicRoutes";
import LoginView from "@/views/login/LoginView.vue";
import MainLayout from "@/components/layout/MainLayout.vue";
import DashboardView from "@/views/DashboardView.vue";
import api from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import {
  refreshAccessToken,
  forceLogout,
  getRefreshToken,
} from "@/composables/useAuth";

// URL 구조 (도메인 연결 시 prafta.com 기준):
//   '/'          → 회사소개 랜딩(공개)            = prafta.com/
//   '/safenote'  → SafeNote 서비스(로그인/관리자) = prafta.com/safenote/...
// SafeNote 서비스의 모든 경로는 '/safenote' 프리픽스 아래에 둔다.
const SERVICE_BASE = "/safenote";

// 초기 고정 라우트만 선언 (동적 화면은 나중에 주입)
const routes = [
  // 회사소개 랜딩 (루트 도메인, 비로그인 공개)
  // 기존 CompanyIntroView.vue는 보존(미라우팅). 신규 홈페이지 메인으로 교체.
  {
    path: "/",
    name: "Home",
    component: () => import("@/views/intro/IntroMainView.vue"),
  },
  // 홈페이지 intro 공개 페이지 (비로그인, requiresAuth 없음)
  {
    path: "/about",
    name: "IntroAbout",
    component: () => import("@/views/intro/IntroAboutView.vue"),
  },
  {
    path: "/service",
    name: "IntroService",
    component: () => import("@/views/intro/IntroServiceView.vue"),
  },
  {
    path: "/pricing",
    name: "IntroPricing",
    component: () => import("@/views/intro/IntroPricingView.vue"),
  },
  {
    path: "/contact",
    name: "IntroContact",
    component: () => import("@/views/intro/IntroContactView.vue"),
  },
  // SafeNote 서비스 진입 = 로그인
  { path: SERVICE_BASE, name: "Login", component: LoginView },
  // 일일계정 ID 생성용 별도 경로 (PRAFTA-007) — path 중복 해소
  {
    path: `${SERVICE_BASE}/daily-user-create`,
    name: "dailyUserIdCreate",
    component: LoginView,
  },
  {
    path: `${SERVICE_BASE}/main`,
    name: "Main",
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: "",
        name: "Dashboard",
        component: DashboardView,
        meta: { requiresAuth: true },
        props: true,
      },
    ],
  },
  // 일일사용자 회원가입 (비로그인 외부 화면, PRAFTA-013)
  // joinCd = {회사코드}-{사업장코드5자리}. requiresAuth 미부착 → 인증 강제 없음.
  // 기존에 배포된 '/dailyUserJoin/...' 링크/QR 호환을 위해 alias 유지.
  {
    path: `${SERVICE_BASE}/dailyUserJoin/:joinCd`,
    name: "DailyUserJoin",
    alias: "/dailyUserJoin/:joinCd",
    component: () => import("@/views/dailyJoin/DailyUserJoin.vue"),
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("@/views/_common/NotFound.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// --- 동적 라우트 주입 로직 ---
let dynamicInjected = false;

/**
 * sessionStorage.token이 없으면 refreshToken으로 한 번 복구를 시도한다.
 * - 성공: sessionStorage.token 갱신 후 token 반환.
 * - 실패(또는 refreshToken 부재): 강제 로그아웃 정리 후 null 반환.
 */
async function ensureAccessToken() {
  const token = sessionStorage.getItem("token");
  if (token) return token;

  const rt = getRefreshToken();
  if (!rt) return null;

  try {
    const newToken = await refreshAccessToken();
    if (newToken) {
      api.defaults.headers.common.Authorization = `Bearer ${newToken}`;
    }
    return newToken || null;
  } catch (e) {
    // refresh 실패 → 정리만 수행. 라우팅은 가드가 결정한다.
    await forceLogout();
    return null;
  }
}

async function fetchRoutesFromServer() {
  let data = [];
  try {
    const response = await api.get("/comApi/baseinfo/web-menu-lists", {
      params: {
        userCd: sessionStorage.getItem("gv_userCd"),
        menuSrc: "001",
      },
    });

    if (response.status === 200) {
      data = response.data?.webMenuResultList || [];
    }
  } catch (err) {
    alert(resolveApiErrorMessage(err, "메뉴 조회 중 오류가 발생했습니다."));
  }
  return data;
}

/**
 * 동적 자식 라우트를 Main 라우트 하위에 주입.
 * - 이미 주입되어 있으면 즉시 반환 (한 세션 내 1회 실행).
 * - 외부(MainLayout 등)에서도 호출 가능하도록 export.
 */
export async function injectDynamicRoutes() {
  if (dynamicInjected) return;

  const token = sessionStorage.getItem("token");
  if (!token) return;

  const dbRoutes = await fetchRoutesFromServer();
  sessionStorage.setItem("dynamicRoutes", JSON.stringify(dbRoutes));

  const children = buildDynamicChildren(dbRoutes);
  children.forEach((child) => {
    if (!router.hasRoute(child.name)) {
      router.addRoute("Main", child);
    }
  });

  dynamicInjected = true;
}

/** 동적 라우트가 주입되었는지 외부에 노출 (MainLayout이 탭 자동 추가에 사용). */
export function isDynamicInjected() {
  return dynamicInjected;
}

router.beforeEach(async (to, from, next) => {
  const requiresAuth = to.matched.some((r) => r.meta?.requiresAuth);
  const isLoginPage = to.path === SERVICE_BASE;

  // catch-all/NotFound 진입 여부 (현재 시점 기준)
  const isCatchAllNow =
    to.name === "NotFound" ||
    to.matched.length === 0 ||
    to.matched[0]?.path === "/:pathMatch(.*)*";

  // /safenote/main 하위 경로에 직진입했지만 NotFound로 잡힌 경우:
  // 1) 동적 라우트 미주입 상태 → 토큰 확보 후 주입 → 재평가
  // 2) 주입 완료 후에도 매칭 실패 → /safenote/main으로 라우팅 (대시보드)
  const isMainSubPath = to.path.startsWith(`${SERVICE_BASE}/main`);

  if (isCatchAllNow && isMainSubPath) {
    // PRAFTA-005: 새 탭에서 /safenote/main/{path} 직진입 시 처리
    const ensured = await ensureAccessToken();
    if (!ensured) {
      // 세션 복구 실패 → 로그인
      return next(SERVICE_BASE);
    }
    if (!dynamicInjected) {
      await injectDynamicRoutes();
    }
    // 재해석 후에도 동적 라우트에 매칭되지 않으면 대시보드로 보낸다.
    const resolved = router.resolve(to.fullPath);
    const stillNotFound =
      resolved.name === "NotFound" ||
      resolved.matched.length === 0 ||
      resolved.matched[0]?.path === "/:pathMatch(.*)*";
    if (stillNotFound) {
      return next({ path: `${SERVICE_BASE}/main`, replace: true });
    }
    // 매칭되도록 재평가
    return next({ path: to.fullPath, replace: true });
  }

  // 인증 필요 라우트면 토큰을 먼저 확보(새 탭/새로고침 복구)
  if (requiresAuth) {
    const ensured = await ensureAccessToken();
    if (!ensured) return next(SERVICE_BASE);
    // 토큰 확보 후 동적 라우트 주입 (한 번만)
    if (!dynamicInjected) {
      await injectDynamicRoutes();
    }
  }

  // 로그인 페이지인데 이미 세션이 있으면 메인으로 이동
  const token = sessionStorage.getItem("token");
  if (token && isLoginPage) return next(`${SERVICE_BASE}/main`);

  return next();
});

export default router;
