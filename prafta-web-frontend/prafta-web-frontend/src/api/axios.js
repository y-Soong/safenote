// src/api/axios.js
import axios from "axios";
import { useLoadingStore } from "@/stores/loadingStore";
import { useUserStore } from "@/stores/userStore";
import { $alert } from "@/utils/alertUtil";
import { resolveBaseURL } from "@/api/baseUrl";
import {
  refreshAccessToken,
  forceLogout,
  performServerLogout,
  getRefreshToken,
} from "@/composables/useAuth";

let routerRef;
const getRouter = async () => {
  if (!routerRef) {
    const mod = await import("@/router");
    routerRef = mod.default;
  }
  return routerRef;
};

const api = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

/**
 * 401 응답이 토큰 자체의 문제로 발생한 것인지 판단.
 * - 서버가 errorCode를 반환했다면 AUTH_* 또는 COMMON_400_600만 토큰 에러로 본다.
 * - errorCode가 없으면 보수적으로 토큰 에러로 간주한다 (기존 동작 유지).
 */
function isTokenError(errorCode) {
  return (
    !errorCode ||
    errorCode === "COMMON_400_600" ||
    String(errorCode).startsWith("AUTH_")
  );
}

/** 강제 로그아웃 + 로그인 페이지 이동 (인터셉터 내부에서 일관 사용). */
async function forceLogoutAndRedirect(userStore) {
  await forceLogout();
  try {
    userStore.logout();
  } catch (e) {
    // store 미초기화 등은 무시
  }
  (await getRouter()).push("/");
}

// 요청 인터셉터
// - 정책 §11.1에 따라 휴대폰(gv_mblNo) / 이메일(gv_email)은 요청 파라미터에 포함하지 않는다.
// - 외부 IP 조회(ipify.org) 호출은 제거되었다 (PRAFTA-012).
//   클라이언트 IP가 필요한 경우 백엔드가 HttpServletRequest에서 추출한다.
api.interceptors.request.use(
  async (config) => {
    const loadingStore = useLoadingStore();
    loadingStore.startLoading();

    const userInfo = {
      gv_cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
      gv_userCd: sessionStorage.getItem("gv_userCd"),
      gv_userId: sessionStorage.getItem("gv_userId"),
      gv_userNm: sessionStorage.getItem("gv_userNm"),
      gv_siteCd: sessionStorage.getItem("gv_siteCd"),
      gv_siteNo: sessionStorage.getItem("gv_siteNo"),
      gv_siteNm: sessionStorage.getItem("gv_siteNm"),
      gv_nodeCd: sessionStorage.getItem("gv_nodeCd"),
      gv_nodeNm: sessionStorage.getItem("gv_nodeNm"),
      gv_authCd: sessionStorage.getItem("gv_authCd"),
      gv_authLevel: sessionStorage.getItem("gv_authLevel"),
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

    // 토큰은 sessionStorage 기준
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    // clientType 헤더는 "항상" 붙이는 편이 안전 (백엔드 정책 통일)
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

    const errorCode = error?.response?.data?.errorCode;
    console.log("errorCode :: " + errorCode);

    // COMMON_400_003 → 세션 만료 등 서버가 명시적으로 로그아웃 요구
    if (errorCode === "COMMON_400_003") {
      await forceLogoutAndRedirect(userStore);
      return Promise.reject(error);
    }

    // 1) 401 → refresh → retry
    const tokenError = isTokenError(errorCode);

    // 재시도(_retry) 후에도 401이면 토큰 자체가 무효 → 강제 로그아웃
    if (status === 401 && tokenError && originalRequest?._retry) {
      await forceLogoutAndRedirect(userStore);
      return Promise.reject(error);
    }

    if (
      status === 401 &&
      tokenError &&
      originalRequest &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;

      // refresh 자체에서 401이 다시 나면 즉시 로그아웃 (루프 방지)
      const reqUrl = String(originalRequest.url || "");
      if (
        reqUrl.includes("/auth/refresh") ||
        reqUrl.includes("/comApi/auth/refresh")
      ) {
        await forceLogoutAndRedirect(userStore);
        return Promise.reject(error);
      }

      try {
        // useAuth의 단일 잠금/큐를 통해 refresh
        const newToken = await refreshAccessToken();

        // 기본 헤더 갱신(선택)
        api.defaults.headers.common.Authorization = `Bearer ${newToken}`;

        // 원 요청 재시도
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (e) {
        await forceLogoutAndRedirect(userStore);
        return Promise.reject(e);
      }
    }

    // 2) 기존 로직 유지 (404 + 유효하지 않은 토큰 메시지)
    if (
      status === 404 &&
      error?.response?.data?.message === "유효하지 않은 토큰입니다."
    ) {
      $alert(error.response.data.message);
      await forceLogoutAndRedirect(userStore);
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
// 호환성: 기존에 api/axios에서 import 하던 코드를 위해 재-export.
// 신규 코드는 @/composables/useAuth의 performServerLogout / getRefreshToken을 직접 사용한다.
export { performServerLogout, getRefreshToken };
