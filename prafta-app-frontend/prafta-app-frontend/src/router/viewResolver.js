export function resolveView(viewPath) {
  const raw = String(viewPath || '')
  const safe = raw.replace(/^\/+/, '')
  if (safe.includes('..')) return null // 경로 역참조 방지

  // '@/views' 하위만 허용
  return () => import(`@/views/${safe}`)
}
