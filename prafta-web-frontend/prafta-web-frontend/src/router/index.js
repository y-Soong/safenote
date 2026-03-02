// src/router/index.js
import { createRouter, createWebHistory } from "vue-router";
import { buildDynamicChildren } from "./dynamicRoutes";
import LoginView from "@/views/login/LoginView.vue";
import MainLayout from "@/components/layout/MainLayout.vue";
import DashboardView from "@/views/DashboardView.vue";
import api, { performServerLogout } from "@/api/axios";
import { resolveBaseURL } from "@/api/baseUrl";
import axios from "axios";

// refresh는 인터셉터 없는 plain axios로 호출(루프 방지)
const plain = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

// 초기 고정 라우트만 선언 (동적 화면은 나중에 주입)
const routes = [
  { path: "/", name: "Login", component: LoginView },
  { path: "/", name: "dailyUserIdCreate", component: LoginView },
  {
    path: "/main",
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

// ✅ 동시 refresh 중복 방지 (라우터 가드용)
let bootstrapping = null;

async function ensureAccessToken() {
  const token = sessionStorage.getItem("token");
  if (token) return token;

  const refreshToken = localStorage.getItem("refreshToken");
  if (!refreshToken) return null;

  if (!bootstrapping) {
    bootstrapping = (async () => {
      try {
        // ⚠️ 실제 서버 경로에 맞춰 필요 시 수정
        const res = await plain.post("/comApi/auth/refresh", { refreshToken });
        const newToken = res.data?.token;
        if (newToken) {
          sessionStorage.setItem("token", newToken);

          // ✅ 새 refreshToken이 있으면 저장 (서버가 새 refreshToken을 반환하는 경우)
          if (res.data?.refreshToken) {
            localStorage.setItem("refreshToken", res.data.refreshToken);
          }

          // api 기본헤더도 갱신(선택)
          api.defaults.headers.common.Authorization = `Bearer ${newToken}`;
        }
        return newToken || null;
      } catch (e) {
        // refresh 실패 -> 서버에 로그아웃 요청 후 정리
        await performServerLogout(refreshToken);
        sessionStorage.clear();
        localStorage.removeItem("refreshToken");
        return null;
      } finally {
        bootstrapping = null;
      }
    })();
  }

  return bootstrapping;
}

async function fetchRoutesFromServer() {
  let data = [];
  try {
    const response = await api.post("/comApi/baseinfo/getWebMenuList", {
      cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      userId: sessionStorage.getItem("gv_userId"),
      menuSrc: "001",
    });

    if (response.status === 200) data = response.data;
  } catch (err) {
    alert(err?.response?.data?.message || err.message);
  }
  return data;
}

async function injectDynamicRoutes() {
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

router.beforeEach(async (to, from, next) => {
  const requiresAuth = to.matched.some((r) => r.meta?.requiresAuth);
  const isLoginPage = to.path === "/";

  // ✅ 1) 인증 필요 라우트면 토큰을 먼저 확보(새 탭/새로고침 복구)
  if (requiresAuth) {
    const ensured = await ensureAccessToken();
    if (!ensured) return next("/");
  }

  // ✅ 2) 토큰 확보 후 동적 라우트 주입
  const injectedBefore = dynamicInjected;
  await injectDynamicRoutes();

  // ✅ 3) 주입 직후 /main 하위가 NotFound로 매칭된 경우 재평가
  const isCatchAll =
    to.name === "NotFound" ||
    to.matched.length === 0 ||
    to.matched[0]?.path === "/:pathMatch(.*)*";

  if (
    !injectedBefore &&
    dynamicInjected &&
    isCatchAll &&
    to.path.startsWith("/main")
  ) {
    return next({ path: to.fullPath, replace: true });
  }

  // ✅ 4) 로그인페이지 처리
  const token = sessionStorage.getItem("token");
  if (token && isLoginPage) return next("/main");

  return next();
});

export default router;
