// src/api/axios.js
import axios from "axios";
import { useLoadingStore } from "@/stores/loadingStore";
import { useUserStore } from "@/stores/userStore";
// import router from "@/router";
import { $alert } from "@/utils/alertUtil";
import { resolveBaseURL } from "@/api/baseUrl";

/**
 * baseURL 결정 규칙 (Vue CLI 전용)
 * 1) window.__APP_CONFIG__가 있으면 최우선 (assets/vue_app/app-config.json 등에서 주입)
 * 2) file:// 로 열렸다면(Flutter WebView 로컬 자산) 절대 URL 사용
 *    - env: VUE_APP_FILE_API_BASE, VUE_APP_API_CONTEXT
 *    - 없으면 기본값 'http://172.30.1.4:8080' + '/prafta'
 * 3) 그 외(웹서버에서 서비스 중)는 '/prafta' (dev는 proxy, prod는 리버스프록시 전제)
 */
// const resolveBaseURL = () => {
//   // 1) 외부 설정 주입
//   const cfg = (typeof window !== "undefined" && window.__APP_CONFIG__) || {};
//   if (cfg.API_BASE) {
//     const context = cfg.CONTEXT ?? "/prafta";
//     return `${cfg.API_BASE}${context}`;
//   }

//   // 2) file:// 환경 (Flutter WebView 가 assets로 여는 경우)
//   if (typeof window !== "undefined" && window.location?.protocol === "file:") {
//     const env = (typeof process !== "undefined" && process.env) || {};
//     const apiBase = env.VUE_APP_FILE_API_BASE || "http://172.30.1.4:8080"; // ← 필요 시 https 로 변경
//     const context = env.VUE_APP_API_CONTEXT || "/prafta";
//     return `${apiBase}${context}`;
//   }

//   // 3) 웹서버로 서비스되는 경우
//   return "/prafta";
// };
let router;
const getRouter = async () => {
  if (!router) {
    const mod = await import("@/router");
    router = mod.default;
  }
  return router;
};

/**
 * 클라이언트 IP 주소를 가져옵니다 (외부 API 사용, sessionStorage 캐싱)
 */
async function getClientIP() {
  const cachedIP = sessionStorage.getItem("clientIP");
  if (cachedIP) return cachedIP;

  try {
    const response = await axios.get("https://api.ipify.org?format=json", {
      timeout: 3000,
    });
    const ip = response.data?.ip;
    if (ip) {
      sessionStorage.setItem("clientIP", ip);
      return ip;
    }
  } catch (error) {
    console.warn("[AXIOS] Failed to get client IP:", error.message);
  }

  return null;
}

// ✅ 인터셉터 없는 refresh 전용 인스턴스 (무한루프 방지)
const plain = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

// ------------------------------
// Refresh control (401 handling)
// ------------------------------
let isRefreshing = false;
let refreshQueue = [];

function resolveQueue(error, newToken = null) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(newToken);
  });
  refreshQueue = [];
}

async function requestRefresh() {
  const refreshToken =
    localStorage.getItem("refreshToken") ||
    localStorage.getItem("gv_refreshToken");

  console.log("[REFRESH] hasRT=", !!refreshToken);

  if (!refreshToken) throw new Error("NO_REFRESH_TOKEN");

  try {
    const res = await plain.post("/comApi/auth/refresh", { refreshToken });
    console.log("[REFRESH] status=", res.status, "data=", res.data);

    const newToken = res.data?.token;
    if (!newToken) throw new Error("NO_TOKEN_IN_REFRESH_RESPONSE");

    return newToken;
  } catch (e) {
    console.log(
      "[REFRESH][FAIL]",
      "status=",
      e?.response?.status,
      "data=",
      e?.response?.data,
      "msg=",
      e?.message
    );
    throw e;
  }
}

/**
 * 서버에 로그아웃 요청 (refresh 토큰 무효화용)
 * - 강제 로그아웃 시에도 서버에서 refresh 토큰을 invalidate할 수 있도록 호출
 * - plain 인스턴스 사용 (인터셉터 없음 → 무한루프 방지)
 * @param {string} [refreshToken] - 무효화할 refresh 토큰. 없으면 localStorage에서 조회
 */
async function performServerLogout(refreshToken) {
  const rt = refreshToken || localStorage.getItem("refreshToken");
  // localStorage.getItem("gv_refreshToken");
  if (!rt) return;

  try {
    await plain.post("/comApi/login/logout", { refreshToken: rt });
  } catch (e) {
    console.warn("[LOGOUT] Server logout failed:", e?.message);
  }
}

// 요청 인터셉터
api.interceptors.request.use(
  async (config) => {
    const loadingStore = useLoadingStore();
    loadingStore.startLoading();

    const clientIP = await getClientIP();

    const userInfo = {
      gv_cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      gv_userId: sessionStorage.getItem("gv_userId"),
      gv_userNm: sessionStorage.getItem("gv_userNm"),
      gv_authCd: sessionStorage.getItem("gv_authCd"),
      gv_mblNo: sessionStorage.getItem("gv_mblNo"),
      gv_email: sessionStorage.getItem("gv_email"),
      gv_clientIP: clientIP,
      gv_deviceId: "",
    };

    const method = (config.method || "get").toLowerCase();

    const isFormData = (v) =>
      typeof FormData !== "undefined" && v instanceof FormData;

    if (method === "get") {
      config.params = { ...(config.params || {}), ...userInfo };
    } else {
      if (isFormData(config.data)) {
        Object.entries(userInfo).forEach(([k, v]) => {
          if (v != null) config.data.append(k, v);
        });
        if (config.headers) {
          if (
            String(config.headers["Content-Type"] || "").includes(
              "application/json"
            )
          ) {
            delete config.headers["Content-Type"];
          }
        }
      } else if (!Array.isArray(config.data)) {
        config.data = { ...(config.data || {}), ...userInfo };
      }
    }

    // ✅ 토큰은 sessionStorage 기준
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // ✅ clientType 헤더는 "항상" 붙이는 편이 안전 (백엔드 정책 통일)
    config.headers = config.headers || {};
    config.headers["X-Client-Type"] = "WEB";

    if (
      typeof window !== "undefined" &&
      window.location?.protocol === "file:"
    ) {
      console.debug(
        "[AXIOS file://] =>",
        config.method?.toUpperCase(),
        config.baseURL,
        config.url
      );
    }

    return config;
  },
  (error) => {
    try {
      useLoadingStore().stopLoading();
    } catch {
      console.log(error);
    }
    return Promise.reject(error);
  }
);

// 응답 인터셉터
api.interceptors.response.use(
  (response) => {
    try {
      useLoadingStore().stopLoading();
    } catch {
      console.log(response);
    }
    return response;
  },
  async (error) => {
    try {
      useLoadingStore().stopLoading();
    } catch {
      console.log(error);
    }

    const userStore = useUserStore();
    const status = error?.response?.status;
    const originalRequest = error?.config;

    // ✅ 1) 401 -> refresh -> retry
    if (status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;

      // refresh 요청에서 다시 401나면 즉시 로그아웃 (루프 방지)
      if (
        String(originalRequest.url || "").includes("/auth/refresh") ||
        String(originalRequest.url || "").includes("/comApi/auth/refresh")
      ) {
        const rt = localStorage.getItem("refreshToken");
        // localStorage.getItem("gv_refreshToken");
        await performServerLogout(rt);
        sessionStorage.clear();
        localStorage.removeItem("refreshToken");
        userStore.logout();
        (await getRouter()).push("/");
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // refresh 진행 중이면 큐 대기
        return new Promise((resolve, reject) => {
          refreshQueue.push({
            resolve: (newToken) => {
              originalRequest.headers = originalRequest.headers || {};
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
              resolve(api(originalRequest));
            },
            reject,
          });
        });
      }

      isRefreshing = true;

      try {
        const newToken = await requestRefresh();

        // 새 access token 저장
        sessionStorage.setItem("token", newToken);

        // 기본 헤더 갱신(선택)
        api.defaults.headers.common.Authorization = `Bearer ${newToken}`;

        resolveQueue(null, newToken);

        // 원 요청 재시도
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (e) {
        resolveQueue(e, null);

        // refresh 실패 -> 서버에 로그아웃 요청 후 강제 로그아웃
        const rt =
          localStorage.getItem("refreshToken") ||
          localStorage.getItem("gv_refreshToken");
        await performServerLogout(rt);
        sessionStorage.clear();
        localStorage.removeItem("refreshToken");
        userStore.logout();
        (await getRouter()).push("/");
        return Promise.reject(e);
      } finally {
        isRefreshing = false;
      }
    }

    // ✅ 2) 기존 로직 유지 (404 + 유효하지 않은 토큰)
    if (
      status === 404 &&
      error?.response?.data?.message === "유효하지 않은 토큰입니다."
    ) {
      $alert(error.response.data.message);
      const rt =
        localStorage.getItem("refreshToken") ||
        localStorage.getItem("gv_refreshToken");
      await performServerLogout(rt);
      sessionStorage.clear();
      localStorage.removeItem("refreshToken");
      userStore.logout();
      (await getRouter()).push("/");
      return new Promise(() => {});
    }

    console.error(
      "[AXIOS][ERROR]",
      status,
      error?.message,
      error?.config?.method,
      error?.config?.url
    );
    return Promise.reject(error);
  }
);

export default api;
export { performServerLogout };
