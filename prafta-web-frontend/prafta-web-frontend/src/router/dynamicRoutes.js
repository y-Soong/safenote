import { resolveView } from "./viewResolver";

// 서버 응답(대문자 키)을 Vue Router children으로 변환
export function buildDynamicChildren(dbRoutes) {
  const list = Array.isArray(dbRoutes) ? dbRoutes : [];

  return list.map((r) => {
    // path=name 동일 권장 + 자식 경로는 앞 슬래시 금지
    const rawPath = r.PATH || r.NAME || "";
    const path = String(rawPath).replace(/^\/+/, "");
    const name = r.NAME || path;

    // 서버에서 오는 view 경로 키 후보들을 모두 시도
    const viewStr =
      r.VIEW || r.VIEW_FILE || r.VIEW_PATH || r.VFILE || r.FILE_PATH || "";

    // '@/views/' 또는 '/src/views/' 프리픽스가 섞여 오면 제거
    const normalizedView = String(viewStr)
      .replace(/^@\/views\//, "")
      .replace(/^\/?src\/views\//, "");

    const loader = resolveView(normalizedView); // 예: 'system/Syst_01.vue'

    return {
      path, // 최종 URL: /main/${path}
      name,
      component: loader ?? (() => import("@/views/_common/ComingSoon.vue")),
      meta: { requiresAuth: true, title: r.TITLE || r.MENU_NM || name || path },
      props: true,
    };
  });
}
