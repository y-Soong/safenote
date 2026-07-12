// 빌드 타임에 '@/views' 하위 모든 .vue 를 지연 로더로 수집한다.
// 완전 동적 import(`@/views/${safe}`)는 Vite 가 정적 분석을 못 해 경고가 나고,
// 운영 빌드 시 해당 청크가 번들에서 누락되어 DB 메뉴 기반 동적 라우트가 로드 실패한다.
// import.meta.glob 로 정적 수집하면 경고 제거 + 청크 번들이 동시에 보장된다.
// (키는 이 파일 기준 상대경로: '../views/<sub>/<File>.vue')
const viewModules = import.meta.glob('../views/**/*.vue')

export function resolveView(viewPath) {
  const raw = String(viewPath || '')
  const safe = raw.replace(/^\/+/, '')
  if (!safe || safe.includes('..')) return null // 경로 역참조 방지 / 빈 값 가드

  // 확장자 누락 시 .vue 부착 (DB view 값이 확장자 없이 올 수 있음)
  const withExt = safe.endsWith('.vue') ? safe : `${safe}.vue`

  const key = `../views/${withExt}`
  const loader = viewModules[key]
  return loader || null
}
