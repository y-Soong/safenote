// src/router/index.js
import { createRouter, createWebHistory } from "vue-router";
import { buildDynamicChildren } from "./dynamicRoutes";
import LoginView from "@/views/login/LoginView.vue";
import MainLayout from "@/components/layout/MainLayout.vue";
import Dashboard01 from "@/views/dashboard/Dashboard_01.vue";
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
// 서비스(로그인 이후) 영역의 루트 경로. 이 아래가 아니면 비로그인 공개 화면이다.
// App.vue / IntroHeader 등이 함께 쓰므로 export 한다(문자열 중복 선언 금지).
export const SERVICE_BASE = "/safenote";

// 초기 고정 라우트만 선언 (동적 화면은 나중에 주입)
const routes = [
  // 회사소개 랜딩 (루트 도메인, 비로그인 공개)
  // 2026-08-31: 종전 메인 화면(IntroMainView)은 폐기하고 루트를 회사소개로 교체.
  {
    path: "/",
    name: "Home",
    component: () => import("@/views/intro/IntroAboutView.vue"),
  },
  // 기존에 배포된 /about 링크 호환 — 루트로 보낸다
  { path: "/about", redirect: "/" },
  {
    path: "/attendance",
    name: "IntroAttendance",
    component: () => import("@/views/intro/IntroAttendanceView.vue"),
  },
  {
    path: "/safety",
    name: "IntroSafety",
    component: () => import("@/views/intro/IntroSafetyView.vue"),
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
  // 개인정보 처리방침 (비로그인 공개 — 스토어 제출용 공개 URL, 약관 002 문안)
  {
    path: "/privacy",
    name: "PrivacyPolicy",
    component: () => import("@/views/intro/PrivacyPolicyView.vue"),
  },
  // 계정 삭제 안내 (비로그인 공개 — 구글 플레이 계정삭제 URL 제출용)
  {
    path: "/account-deletion",
    name: "AccountDeletion",
    component: () => import("@/views/intro/AccountDeletionView.vue"),
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
        // PRAFTA-DASHBOARD-T1: 홈 = 관리자 대시보드 (구 DashboardView 더미 교체)
        component: Dashboard01,
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
// 동적 라우트를 주입한 세션 식별자(회사코드/사용자코드). USER_CD 는 회사별 채번이라 전역 유일이 아니므로
//   반드시 CMPNY_CD 와 함께 키로 사용한다. 같은 탭에서 다른 계정으로 재로그인(새로고침 없음) 시
//   이전 계정의 동적 라우트가 남아 메뉴가 어긋나는 것을 막기 위해 키가 바뀌면 재주입한다.
let injectedForKey = null;

/**
 * JWT 페이로드 디코드(서명 검증이 아니라 gv_scope 판별용). 실패 시 null.
 */
function decodeJwtPayload(token) {
  try {
    const part = token.split(".")[1];
    if (!part) return null;
    const json = decodeURIComponent(
      atob(part.replace(/-/g, "+").replace(/_/g, "/"))
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(json);
  } catch (e) {
    return null;
  }
}

/**
 * 임시 scope 토큰(gv_scope 보유) 여부. 휴대폰 본인인증/약관 동의/기본 근무타입 등 게이트 전용 토큰으로,
 * 정식 세션이 아니다. 게이트 미완료 상태에서 새로고침해도 메인 진입을 막기 위함.
 */
function isScopeToken(token) {
  const payload = decodeJwtPayload(token);
  return !!(payload && payload.gv_scope);
}

/**
 * JWT exp 클레임 기준 만료 여부. exp가 없으면 판단 보류(false) — 기존 동작 유지.
 * sessionStorage에 토큰이 "있기만" 하면 유효하다고 보던 가드의 허점을 막기 위함:
 * 장시간 방치 후 메뉴 이동 시, 실제로는 만료된 토큰인데도 가드를 그냥 통과해
 * 화면 마운트 후 API 호출에서야 401이 나던 지연 처리 구간을 없앤다.
 */
function isTokenExpired(token) {
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== "number") return false;
  return Date.now() >= payload.exp * 1000;
}

/**
 * sessionStorage.token이 없거나 만료됐으면 refreshToken으로 한 번 복구를 시도한다.
 * - 성공: sessionStorage.token 갱신 후 token 반환.
 * - 실패(또는 refreshToken 부재): 강제 로그아웃 정리 후 null 반환.
 */
async function ensureAccessToken() {
  const token = sessionStorage.getItem("token");
  if (token) {
    // 게이트 전용 임시 토큰이면 정식 세션으로 인정하지 않는다(서버도 gv_scope 토큰을 일반 API에서 거부).
    if (isScopeToken(token)) {
      sessionStorage.removeItem("token");
      return null;
    }
    if (isTokenExpired(token)) {
      // 만료된 액세스 토큰을 들고 있는 상태 — 정리 후 아래 refreshToken 경로로 넘어간다.
      sessionStorage.removeItem("token");
    } else {
      return token;
    }
  }

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
export async function injectDynamicRoutes(force = false) {
  const currentKey = `${sessionStorage.getItem("gv_cmpnyCd")}/${sessionStorage.getItem("gv_userCd")}`;

  // 다른 계정으로 재로그인(같은 탭, 새로고침 없음)했으면 이전 계정의 동적 라우트를 비우고 재주입한다.
  if (injectedForKey && injectedForKey !== currentKey) {
    resetDynamicRoutes();
  }

  if (dynamicInjected && !force) return;

  const token = sessionStorage.getItem("token");
  if (!token) return;

  const dbRoutes = await fetchRoutesFromServer();

  // 메뉴 조회가 실패하거나 빈 응답이면 latch 하지 않는다(다음 진입에서 재시도).
  //   최초 로그인 직후 일시적 실패가 dynamicInjected=true 로 고착되어, 새로고침(모듈 재초기화)
  //   전까지 동적 라우트가 비어 메뉴 클릭이 동작하지 않던 현상을 방지한다.
  if (!Array.isArray(dbRoutes) || dbRoutes.length === 0) {
    return;
  }

  sessionStorage.setItem("dynamicRoutes", JSON.stringify(dbRoutes));

  const children = buildDynamicChildren(dbRoutes);
  children.forEach((child) => {
    if (!router.hasRoute(child.name)) {
      router.addRoute("Main", child);
    }
  });

  dynamicInjected = true;
  injectedForKey = currentKey;
}

/**
 * 주입된 동적 라우트를 제거하고 주입 상태를 초기화한다.
 * - 로그아웃/계정 전환 시 호출하여 다음 로그인에서 새 메뉴로 재주입되게 한다.
 */
export function resetDynamicRoutes() {
  const raw = sessionStorage.getItem("dynamicRoutes");
  if (raw) {
    try {
      buildDynamicChildren(JSON.parse(raw)).forEach((child) => {
        if (child.name && router.hasRoute(child.name)) {
          router.removeRoute(child.name);
        }
      });
    } catch (e) {
      // 파싱 실패는 무시 — 상태 플래그만 초기화한다.
    }
  }
  sessionStorage.removeItem("dynamicRoutes");
  dynamicInjected = false;
  injectedForKey = null;
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
    // 재해석 후에도 동적 라우트에 매칭되지 않으면, 주입이 누락/지연되었을 수 있으므로
    //   한 번 더 강제 재주입(force)하여 재해석한다. 최초 로그인 직후 메뉴 클릭이 빈 화면으로
    //   떨어지고 새로고침해야 풀리던 현상을 보강(메뉴 라우트가 실제 존재하면 여기서 살아난다).
    let resolved = router.resolve(to.fullPath);
    let stillNotFound =
      resolved.name === "NotFound" ||
      resolved.matched.length === 0 ||
      resolved.matched[0]?.path === "/:pathMatch(.*)*";
    if (stillNotFound) {
      await injectDynamicRoutes(true);
      resolved = router.resolve(to.fullPath);
      stillNotFound =
        resolved.name === "NotFound" ||
        resolved.matched.length === 0 ||
        resolved.matched[0]?.path === "/:pathMatch(.*)*";
    }
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

  // 로그인 페이지인데 이미 세션이 있으면 메인으로 이동.
  //   단, 게이트 미완료 임시 scope 토큰은 세션이 아니므로 제거만 하고 로그인 페이지에 머문다(새로고침 우회 차단).
  const token = sessionStorage.getItem("token");
  if (token && isLoginPage) {
    if (isScopeToken(token)) {
      sessionStorage.removeItem("token");
    } else {
      return next(`${SERVICE_BASE}/main`);
    }
  }

  return next();
});

export default router;
