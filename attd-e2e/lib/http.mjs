// 공용 HTTP 헬퍼 — PRAFTA 백엔드(localhost:8080/prafta) API 직접 호출용(시드 적재·스케줄러 유도).
// subcon-e2e/lib/http.mjs 패턴 계승. 실패 시 상태코드+본문 그대로 노출(디버깅 우선).
export const BASE = "http://localhost:8080/prafta";

export async function call(method, path, { token, body, clientType = "WEB" } = {}) {
  const headers = { "Content-Type": "application/json", "X-Client-Type": clientType };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { /* 비 JSON 응답 */ }
  return { status: res.status, ok: res.ok, json, text };
}

// 로그인 — { userId, userPw } → 토큰 포함 응답. clientType: WEB(웹관리자) | APP(앱).
export async function login(userId, userPw, clientType = "WEB") {
  return call("POST", "/comApi/login/login", { body: { userId, userPw }, clientType });
}

// 토큰 캐시 로그인 — 같은 계정 반복 로그인 방지(만료 시 재로그인은 호출부 책임).
const tokenCache = new Map();
export async function getToken(userId, userPw, clientType = "WEB") {
  const key = `${userId}:${clientType}`;
  if (tokenCache.has(key)) return tokenCache.get(key);
  const r = await login(userId, userPw, clientType);
  // 응답 토큰 필드는 `token`(access), `refreshToken` (2026-07-17 실측).
  if (r.status !== 200 || !r.json?.token) {
    throw new Error(`로그인 실패 ${userId}: ${r.status} ${r.text.slice(0, 200)}`);
  }
  tokenCache.set(key, r.json.token);
  return r.json.token;
}
export function evictToken(userId, clientType = "WEB") {
  tokenCache.delete(`${userId}:${clientType}`);
}
