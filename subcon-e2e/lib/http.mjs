// 공용 HTTP 헬퍼 — PRAFTA 백엔드(localhost:8080/prafta) 호출용.
// 토큰/응답을 그대로 반환하고, 실패 시 상태코드+본문을 노출한다(디버깅 우선).
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

// 로그인 — { userId, userPw } → LoginResponse(정식/임시 토큰 포함).
export async function login(userId, userPw, clientType = "WEB") {
  return call("POST", "/comApi/login/login", { body: { userId, userPw }, clientType });
}
