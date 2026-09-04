// /src/utils/externalLink.js
//
// 외부 링크 열기 + 유튜브 썸네일 유틸 (2026-09-05).
//
// ★왜 필요한가 — `<a target="_blank">` 는 웹뷰에서 아무 일도 일어나지 않는다.
//   `target="_blank"` 는 "새 창 생성(createWindow)" 요청이라 셸의
//   `shouldOverrideUrlLoading` 을 타지 않는다. 셸(web_app.dart)에는 `onCreateWindow`
//   핸들러가 없어서 요청이 그대로 버려진다 = 버튼을 눌러도 무반응.
//
//   반면 같은 창 내비게이션(`location.href`)은 `shouldOverrideUrlLoading` 을 타고,
//   셸이 "화이트리스트 밖 http(s) 메인프레임" 으로 판정해
//   `_launchExternal()`(url_launcher, LaunchMode.externalApplication)로 외부 브라우저에
//   넘긴 뒤 `NavigationActionPolicy.CANCEL` 한다. 즉 **웹뷰는 현재 화면을 그대로 유지**한
//   채 외부 브라우저만 뜬다 — 우리가 원하는 동작이 이미 셸에 구현돼 있다.
//   따라서 셸 수정 없이(스토어 릴리즈 없이) 원격 배포만으로 해결된다.
//
// ★유튜브 인라인 재생(iframe)은 불가하다 — 셸이 화이트리스트 밖 서브프레임 문서 로드를
//   차단한다(H-1② iframe 브리지 우회 봉쇄). 그래서 "썸네일 + 외부 열기" 가 유일한 방법이다.
//   썸네일은 `<img>` 리소스 로드라 위 차단(문서 내비게이션/서브프레임) 대상이 아니다.

/** 웹뷰(셸) 안인지 판별. 기존 브리지 래퍼들의 isBridgeAvailable() 과 동일 기준. */
function isInWebView() {
  return (
    typeof window !== 'undefined' &&
    !!window.flutter_inappwebview &&
    typeof window.flutter_inappwebview.callHandler === 'function'
  )
}

/**
 * 유튜브 URL 에서 videoId 를 뽑는다. 유튜브가 아니면 null.
 *
 * 지원 형태: watch?v=ID / youtu.be/ID / shorts/ID / embed/ID / live/ID
 * (운영 실데이터에 `watch?v=ID&list=...&start_radio=1` 처럼 부가 파라미터가 붙은 건이 있어
 *  쿼리 전체를 파싱해 v 만 취한다. 타입 '03' 은 "외부링크" 라 유튜브가 아닌 URL 도 들어온다
 *  — 운영에 github.com 링크 실재 — 그 경우 null 을 돌려 기존 링크 버튼으로 폴백시킨다.)
 *
 * @param {string} url
 * @returns {string|null} 11자 videoId 또는 null
 */
export function extractYoutubeId(url) {
  if (!url || typeof url !== 'string') return null
  let u
  try {
    u = new URL(url.trim())
  } catch {
    return null
  }
  const host = u.hostname.replace(/^www\./, '').toLowerCase()
  const isYoutube =
    host === 'youtube.com' ||
    host === 'm.youtube.com' ||
    host === 'music.youtube.com' ||
    host === 'youtu.be'
  if (!isYoutube) return null

  // youtu.be/ID
  if (host === 'youtu.be') return normalizeId(u.pathname.slice(1))

  // youtube.com/watch?v=ID
  const v = u.searchParams.get('v')
  if (v) return normalizeId(v)

  // youtube.com/{shorts|embed|live}/ID
  const m = u.pathname.match(/^\/(?:shorts|embed|live)\/([^/?#]+)/)
  if (m) return normalizeId(m[1])

  return null
}

/** videoId 형식 검증(유튜브 11자 영숫자·-·_). 어긋나면 null. */
function normalizeId(raw) {
  const id = (raw || '').split(/[/?#]/)[0]
  return /^[A-Za-z0-9_-]{11}$/.test(id) ? id : null
}

/**
 * 유튜브 썸네일 URL. hqdefault 는 모든 영상에 존재한다(maxresdefault 는 없는 영상이 있어 쓰지 않는다).
 * @param {string} videoId
 * @returns {string}
 */
export function youtubeThumbUrl(videoId) {
  return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`
}

/**
 * 외부 URL 을 연다.
 *  - 웹뷰: location.href 로 같은 창 내비게이션 → 셸이 가로채 외부 브라우저로 위임 + 로드 취소
 *          (현재 화면 유지). window.open 은 무반응이라 쓰지 않는다.
 *  - 브라우저(개발/PC): window.open 새 탭. 팝업 차단되면 같은 탭으로 폴백.
 *
 * @param {string} url
 * @returns {boolean} 시도 여부(빈 URL 이면 false)
 */
export function openExternalUrl(url) {
  if (!url || typeof url !== 'string') return false
  const target = url.trim()
  if (!target) return false

  if (isInWebView()) {
    window.location.href = target
    return true
  }
  const win = window.open(target, '_blank', 'noopener,noreferrer')
  if (!win) window.location.href = target
  return true
}
