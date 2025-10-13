import { resolveView } from './viewResolver'

export function buildDynamicRoutes(dbRoutes) {
  if (!Array.isArray(dbRoutes)) return []

  return dbRoutes.map((r) => {
    const path = String(r.PATH || '').replace(/^\/+/, '')
    const name = r.NAME || path
    const viewPath = String(r.VIEW || '')
      .replace(/^@\/views\//, '')
      .replace(/^\/?src\/views\//, '')

    const loader = resolveView(viewPath)

    return {
      path: `/${path}`, // ✅ 루트 기준 경로
      name,
      component: loader ?? (() => import('@/views/_common/ComingSoon.vue')),
      meta: { title: r.TITLE || name },
      viewPath, // ← 구분용 필드 유지 (login/main 분리용)
      props: true,
    }
  })
}
