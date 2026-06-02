import { resolveView } from './viewResolver'

export function buildDynamicRoutes(dbRoutes) {
  if (!Array.isArray(dbRoutes)) return []

  return dbRoutes.map((r) => {
    // 백엔드 kebab 리팩터 정렬: AppMenuResult 가 camelCase(path/name/view/title) 로 응답
    const path = String(r.path || '').replace(/^\/+/, '')
    const name = r.name || path
    const viewPath = String(r.view || '')
      .replace(/^@\/views\//, '')
      .replace(/^\/?src\/views\//, '')

    const loader = resolveView(viewPath)

    return {
      path: `/${path}`, // ✅ 루트 기준 경로
      name,
      component: loader ?? (() => import('@/views/_common/ComingSoon.vue')),
      meta: { title: r.title || name },
      viewPath, // ← 구분용 필드 유지 (login/main 분리용)
      props: true,
    }
  })
}
