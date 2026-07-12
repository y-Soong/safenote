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

// 세션 만료/토큰 무효로 강제 로그아웃할 때 사용자에게 보여줄 기본 안내 메시지.
const SESSION_EXPIRED_MESSAGE = "세션이 만료되었습니다.\n다시 로그인해 주세요.";

// 동시에 여러 요청이 401 을 받아 forceLogoutAndRedirect 가 다발 호출될 때
// 안내 알림/서버 로그아웃/네비게이션이 중복 실행되지 않도록 하는 단일 플라이트 가드.
// 로그아웃 흐름이 끝나면 다시 null 로 풀어 다음 세션 만료에도 정상 동작하게 한다.
let loggingOut = null;

/**
 * 강제 로그아웃 + 로그인 페이지 이동 (인터셉터 내부에서 일관 사용).
 * @param userStore Pinia user store (null 가능)
 * @param message   강제 로그아웃 사유 안내 메시지. null/빈값이면 알림을 띄우지 않는다.
 */
function forceLogoutAndRedirect(userStore, message = SESSION_EXPIRED_MESSAGE) {
  // 이미 로그아웃 진행 중이면(동시 401 다발 등) 진행 중인 흐름을 그대로 공유한다.
  if (loggingOut) return loggingOut;

  loggingOut = (async () => {
    try {
      // 강제 로그아웃 사유를 사용자에게 1회 안내(메시지가 있을 때만).
      // 안내 후 사용자가 확인을 누르면 정리/이동을 진행한다.
      if (message) {
        try {
          await $alert(message);
        } catch (e) {
          // 알림 모달 미초기화 등은 무시하고 로그아웃은 계속 진행한다.
        }
      }

      await forceLogout();
      try {
        userStore?.logout();
      } catch (e) {
        // store 미초기화 등은 무시
      }
      // 로그인 화면 = SafeNote 서비스 진입('/safenote'). 루트('/')는 회사소개 랜딩이다.
      // router.push 가 (동일 라우트/네비게이션 취소 등으로) 무시되어 화면이 그대로 남는
      // 케이스가 있어, 라우팅이 적용되지 않으면 하드 리다이렉트로 확실히 로그인 화면으로 보낸다.
      try {
        const r = await getRouter();
        if (r?.currentRoute?.value?.path !== "/safenote") {
          await r.push("/safenote");
        }
      } catch (e) {
        // 네비게이션 실패는 아래 하드 폴백에서 처리
      }
      if (
        typeof window !== "undefined" &&
        window.location?.pathname !== "/safenote"
      ) {
        // SPA 라우팅이 적용되지 않은 경우 최종 폴백(전체 새로고침으로 세션 초기화)
        window.location.assign("/safenote");
      }
    } finally {
      // 하드 리다이렉트가 없을 때(SPA push 성공) 다음 세션 만료에도 동작하도록 가드 해제.
      loggingOut = null;
    }
  })();

  return loggingOut;
}

// ── 중복 변경요청(저장 연타) 차단용 in-flight 가드 ──────────────────────────
//   같은 변경요청(POST/PUT/PATCH/DELETE)이 "응답이 오기 전에" 다시 전송되면
//   동일 INSERT/UPDATE 가 서버에서 여러 번 실행되어 데이터가 중복된다(저장 버튼 연타).
//   전역 로딩 오버레이는 요청이 떠 있는 동안만 클릭을 막아, 이미 큐에 쌓인 빠른 더블클릭이나
//   요청 직전 구간(검증/confirm)을 못 막으므로, 화면/핸들러를 수정하지 않고 여기서 일괄 방어한다.
//   - 시그니처(method+url+body)가 동일한 요청이 진행 중이면 두 번째 요청은 전송하지 않는다.
//   - 응답(성공/실패) 시 시그니처를 해제하므로, 의도적인 동일 재저장은 직전 요청 완료 후 정상 동작한다.
//   - 조회(GET)는 중복돼도 무해하므로 대상에서 제외한다.
//   - FormData(첨부파일 동반 저장)도 보호한다. JSON 직렬화는 불가하므로 엔트리(필드값 + 파일 메타:
//     이름/크기/수정시각/타입)로 시그니처를 만든다. (앱쪽에서 FormData 경로가 미보호여서 첨부 저장
//     연타가 중복되던 것과 동일한 사각지대를 웹에서도 닫는다.)
const inFlightRequests = new Set();

// FormData → 결정적 시그니처 문자열. 동일 클릭은 같은 필드/같은 File(이름·크기·lastModified·type)이라
//   엔트리가 동일하므로 시그니처도 동일하다. 파일 본문은 읽지 않고 메타데이터만 사용(비용 無).
function serializeFormData(fd) {
  const parts = [];
  for (const [k, v] of fd.entries()) {
    if (typeof File !== "undefined" && v instanceof File) {
      parts.push(`${k}=@file:${v.name}:${v.size}:${v.lastModified}:${v.type}`);
    } else if (typeof Blob !== "undefined" && v instanceof Blob) {
      parts.push(`${k}=@blob:${v.size}:${v.type}`);
    } else {
      parts.push(`${k}=${String(v)}`);
    }
  }
  return parts.join("&");
}

function buildRequestSignature(config) {
  const method = (config.method || "get").toLowerCase();
  if (!["post", "put", "patch", "delete"].includes(method)) return null;
  const data = config.data;
  try {
    if (typeof FormData !== "undefined" && data instanceof FormData) {
      return `${method}::${config.url}::FD::${serializeFormData(data)}`;
    }
    const body = data == null ? "" : JSON.stringify(data);
    return `${method}::${config.url}::${body}`;
  } catch (e) {
    // 순환참조/엔트리 순회 실패 등 직렬화 불가 → 가드 제외(기존 동작 유지).
    return null;
  }
}

// 요청 인터셉터
// - 정책 §11.1에 따라 휴대폰(gv_mblNo) / 이메일(gv_email)은 요청 파라미터에 포함하지 않는다.
// - 외부 IP 조회(ipify.org) 호출은 제거되었다 (PRAFTA-012).
//   클라이언트 IP가 필요한 경우 백엔드가 HttpServletRequest에서 추출한다.
api.interceptors.request.use(
  async (config) => {
    // 중복 변경요청 차단 — startLoading 이전에 검사해 로딩 카운터 균형을 유지한다.
    //   (시그니처는 userInfo 병합 전 원본 body 기준이지만, 동일 클릭은 같은 payload·같은 세션값이라
    //    병합 후에도 동일하므로 더블클릭 식별에 충분하다.)
    const reqSignature = buildRequestSignature(config);
    if (reqSignature) {
      if (inFlightRequests.has(reqSignature)) {
        // 동일 변경요청이 진행 중 → 이 요청은 보내지 않고 조용히 무시(중복 클릭 no-op).
        const dupErr = new Error("DUPLICATE_REQUEST");
        dupErr.__duplicate = true;
        return Promise.reject(dupErr);
      }
      inFlightRequests.add(reqSignature);
      config.__reqSignature = reqSignature;
    }

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
    // 완료된 변경요청의 in-flight 시그니처 해제(이후 동일 재저장 허용).
    if (response?.config?.__reqSignature) {
      inFlightRequests.delete(response.config.__reqSignature);
    }
    return response;
  },
  async (error) => {
    // 중복으로 차단된 요청: 실제로 전송/로딩되지 않았으므로 stopLoading·시그니처 해제를 건드리지 않고
    //   영원히 보류되는 promise 를 반환해 호출자 then/catch 가 동작하지 않게 한다(중복 클릭 no-op).
    //   (기존 강제 로그아웃 흐름과 동일한 'new Promise(() => {})' 보류 패턴.)
    if (error?.__duplicate) {
      return new Promise(() => {});
    }

    try {
      useLoadingStore().stopLoading();
    } catch {
      console.log(error);
    }
    // 실패한 변경요청의 in-flight 시그니처 해제(재시도 허용).
    if (error?.config?.__reqSignature) {
      inFlightRequests.delete(error.config.__reqSignature);
    }

    // Pinia 미초기화 등으로 useUserStore() 가 throw 하면 이후 로그아웃 분기가 통째로
    // 건너뛰어져(에러만 노출·로그아웃 안 됨) 버리므로 방어적으로 감싼다.
    let userStore = null;
    try {
      userStore = useUserStore();
    } catch (e) {
      userStore = null;
    }
    const status = error?.response?.status;
    const originalRequest = error?.config;

    const errorCode = error?.response?.data?.errorCode;

    // COMMON_400_003 → 세션 만료 등 서버가 명시적으로 로그아웃 요구
    if (errorCode === "COMMON_400_003") {
      await forceLogoutAndRedirect(userStore);
      // 로그아웃/리다이렉트 중이므로 호출자 catch(예: "조회 중 오류") 가 뜨지 않도록 보류.
      return new Promise(() => {});
    }

    // prafta-057: AUTH_409_001 → 다른 환경(다른 브라우저/PC)에서 신규 로그인되어 현재 세션이 폐기됨.
    //   refresh 를 시도하지 않고(어차피 패밀리가 폐기되어 무효) 전용 안내 후 즉시 로그아웃한다.
    if (errorCode === "AUTH_409_001") {
      await forceLogoutAndRedirect(
        userStore,
        "다른 환경에서 로그인을 감지했습니다."
      );
      return new Promise(() => {});
    }

    // prafta-app-033: AUTH_403_001 → 로그인 게이트(이용약관 동의 / 강제 비밀번호 변경) 미해소.
    //   403 이므로 401 refresh 경로와 분리한다(refresh 미시도). 즉시 로그아웃 후 로그인 화면 복귀 →
    //   재로그인 시 기존 게이트 플로우(TermsPop / ForcedPasswordChangePop)가 다시 떠서 해소한다.
    //   서버 메시지를 그대로 안내하되, 없으면 forceLogoutAndRedirect 기본 안내로 폴백한다.
    if (errorCode === "AUTH_403_001") {
      await forceLogoutAndRedirect(userStore, error?.response?.data?.message);
      return new Promise(() => {});
    }

    // 1) 401 → refresh → retry
    const tokenError = isTokenError(errorCode);

    // 재시도(_retry) 후에도 401이면 토큰 자체가 무효 → 강제 로그아웃
    if (status === 401 && tokenError && originalRequest?._retry) {
      await forceLogoutAndRedirect(userStore);
      return new Promise(() => {});
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
        return new Promise(() => {});
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
        return new Promise(() => {});
      }
    }

    // 2) 기존 로직 유지 (404 + 유효하지 않은 토큰 메시지)
    if (
      status === 404 &&
      error?.response?.data?.message === "유효하지 않은 토큰입니다."
    ) {
      // 안내는 forceLogoutAndRedirect 가 단일 진입점에서 1회만 처리(중복 알림 방지).
      await forceLogoutAndRedirect(userStore, error.response.data.message);
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
