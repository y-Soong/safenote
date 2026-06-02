// src/composables/useAuth.js
// 세션·토큰 관련 단일 진입점.
// refresh 호출, refresh 잠금/대기 큐, refreshToken 저장/조회, 강제 로그아웃 정리를
// 본 파일 한 곳에서 관리한다. router/index.js, App.vue, api/axios.js는 모두 본 모듈만 호출한다.

import axios from "axios";
import { resolveBaseURL } from "@/api/baseUrl";

// 정책: refreshToken 단일 키 = "refreshToken" (localStorage)
const REFRESH_TOKEN_KEY = "refreshToken";

// 정책 §11.1에 따라 JWT/세션에 PII는 저장하지 않는다.
// sessionStorage에 보관 가능한 비-PII 식별·인가 키 집합. PII(mblNo/email)는 의도적으로 제외.
const SESSION_KEYS = [
  "token",
  "gv_cmpnyCd",
  "gv_userCd",
  "gv_userId",
  "gv_userNm",
  "gv_siteCd",
  "gv_siteNo",
  "gv_siteNm",
  "gv_nodeCd",
  "gv_nodeNm",
  "gv_authCd",
  "gv_authLevel",
];

// refresh 호출 전용 인스턴스. 응답 인터셉터를 두지 않아 401 → refresh 재귀 호출을 차단한다.
const plain = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 10000,
});

// 모듈 스코프 잠금 + 대기 큐 (라우터·인터셉터·App.vue가 동시에 호출해도 1회만 실제 요청)
let refreshing = null;
let waitQueue = [];

function drainQueue(error, newToken) {
  const queue = waitQueue;
  waitQueue = [];
  queue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(newToken);
  });
}

/** localStorage에서 refreshToken 조회 (단일 키). */
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/** refreshToken 저장. */
export function setRefreshToken(token) {
  if (!token) return;
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

/** refreshToken 제거. */
export function removeRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  // 과거 fallback 키 흔적도 함께 제거 (PRAFTA-009)
  localStorage.removeItem("gv_refreshToken");
}

/** sessionStorage 일괄 정리(비-PII 키 + 기타 모두). */
export function clearSession() {
  sessionStorage.clear();
}

/**
 * 서버에 refresh 토큰 무효화 요청.
 * - 실패해도 클라이언트 정리는 호출자가 진행하므로 throw 하지 않는다.
 */
export async function performServerLogout(refreshToken) {
  const rt = refreshToken || getRefreshToken();
  if (!rt) return;
  try {
    await plain.post("/comApi/login/logout", { refreshToken: rt });
  } catch (e) {
    // 서버 로그아웃 실패는 무시 (클라이언트 정리는 별도로 수행됨)
    // eslint-disable-next-line no-console
    console.warn("[AUTH] server logout failed:", e?.message);
  }
}

/**
 * refreshToken → 새 accessToken 교환.
 * - 동시에 호출되면 단일 요청으로 합치고 결과를 큐로 분배.
 * - 성공: sessionStorage.token 갱신 + 서버가 새 refreshToken을 주면 그것도 저장.
 * - 실패: 서버 로그아웃 + 클라이언트 정리 후 예외 전파.
 */
export async function refreshAccessToken() {
  if (refreshing) {
    return new Promise((resolve, reject) => {
      waitQueue.push({ resolve, reject });
    });
  }

  const rt = getRefreshToken();
  if (!rt) {
    throw new Error("NO_REFRESH_TOKEN");
  }

  refreshing = (async () => {
    try {
      const res = await plain.post("/comApi/auth/refresh", {
        refreshToken: rt,
      });
      const newToken = res.data?.token;
      if (!newToken) throw new Error("NO_TOKEN_IN_REFRESH_RESPONSE");

      sessionStorage.setItem("token", newToken);
      if (res.data?.refreshToken) {
        setRefreshToken(res.data.refreshToken);
      }
      drainQueue(null, newToken);
      return newToken;
    } catch (e) {
      drainQueue(e, null);
      throw e;
    } finally {
      refreshing = null;
    }
  })();

  return refreshing;
}

/**
 * 강제 로그아웃 정리 — 서버 로그아웃 호출 + 토큰/세션 일괄 정리.
 * 호출자는 정리 후 라우터 push("/")를 별도로 수행한다.
 */
export async function forceLogout() {
  const rt = getRefreshToken();
  await performServerLogout(rt);
  clearSession();
  removeRefreshToken();
}

/** 외부에서 세션 정리에 사용하는 키 목록 (테스트/디버깅 용도). */
export const __SESSION_KEYS__ = SESSION_KEYS;
