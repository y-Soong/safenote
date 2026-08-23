// currentSite.js — 로그인 세션의 현재 사업장 코드 조회 헬퍼.
//   axios 인터셉터(api/axios.js)와 동일하게 sessionStorage('gv_siteCd')를 단일 출처로 읽는다.
//   작업지시서_소속이동-이력가시성-보정(근로자본인) T3: "당시 소속 사업장" 배지 판정(레코드 SITE_CD와 비교)에 사용.

const SESSION_KEY = 'gv_siteCd'

// 현재 로그인 사용자의 사업장 코드 반환(없으면 빈 문자열).
export function getCurrentSiteCd() {
  return sessionStorage.getItem(SESSION_KEY) || ''
}
