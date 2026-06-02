// overlayBack.js
let overlayDepth = 0

export function openOverlay(onClose) {
  overlayDepth++

  // overlay가 처음 열릴 때만 히스토리 push (중첩 대응 가능)
  history.pushState({ __overlay__: true }, '')

  const handler = (e) => {
    console.log(e)
    // 뒤로가기가 발생했을 때 overlay 닫기
    if (overlayDepth > 0) {
      overlayDepth--
      try {
        onClose?.()
      } catch (e) {
        console.error(e)
      }
    }

    // 중첩 overlay가 남아있으면 계속 state 유지
    if (overlayDepth > 0) {
      history.pushState({ __overlay__: true }, '')
    } else {
      window.removeEventListener('popstate', handler)
    }
  }

  window.addEventListener('popstate', handler, { once: true })
}

export function closeOverlay() {
  if (overlayDepth > 0) {
    overlayDepth--
  }
}
