// Vite: import.meta.glob으로 뷰 모듈 사전 로드 (동적 import 경로는 Vite에서 정적 분석 불가)
// glob 키 형식: /src/views/attd/Attd_01_2.vue
const viewModules = import.meta.glob("/src/views/**/*.vue");

export function resolveView(viewPath) {
  const raw = String(viewPath || "").trim();
  let safe = raw.replace(/^\/+/, "").replace(/\\/g, "/");
  if (safe.includes("..")) return null; // 경로 역참조 방지
  if (!safe) return null;

  // .vue 확장자 보정
  if (!safe.endsWith(".vue")) safe = safe + ".vue";

  // import.meta.glob 키 형식 (프로젝트 루트 기준): src/views/attd/Attd_01_2.vue 또는 /src/views/...
  const candidates = [
    `/src/views/${safe}`,
    `src/views/${safe}`,
    `/${safe}`,
    safe,
    `/src/views/${safe.replace(/^views\//, "")}`,
  ];
  for (const key of candidates) {
    const loader = viewModules[key];
    if (loader) return loader;
  }
  // 키가 정확히 일치하지 않으면 부분 매칭 시도 (glob 키 형식 확인용)
  const safeLower = safe.toLowerCase();
  for (const key of Object.keys(viewModules)) {
    if (key.replace(/\\/g, "/").toLowerCase().endsWith(safeLower)) {
      return viewModules[key];
    }
  }
  return () => import("@/views/_common/ComingSoon.vue");
}
