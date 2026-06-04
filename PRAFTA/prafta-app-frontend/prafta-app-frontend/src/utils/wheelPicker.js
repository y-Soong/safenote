/*
  wheelPicker.js — 휠(드럼) 피커 공통 로직
  - 스크롤 스냅 기반 토스 스타일 휠을 DateStepperField / TimeStepperField / MonthPickerSheet 가 공유한다.
  - DOM 엘리먼트를 직접 다루므로, 각 컴포넌트는 휠 엘리먼트 ref 와 항목 배열만 보유하면 된다.
  - 아래 높이 상수는 .vue 의 CSS(.wp-item / .wp-pad)와 반드시 동일해야 한다.
*/

// 항목 1칸 높이(px). CSS .wp-item height 와 일치.
export const WHEEL_ITEM_H = 40
// 위/아래 패딩 높이(px). CSS .wp-pad height 와 일치(=2칸이라 중앙 밴드가 항목과 정렬됨).
export const WHEEL_PAD_H = 80

// 현재 스크롤 위치의 중앙 항목에 active 클래스를 표시한다.
export function markCenter(el) {
  if (!el) return
  const idx = Math.round(el.scrollTop / WHEEL_ITEM_H)
  const items = el.querySelectorAll('.wp-item')
  items.forEach((it, n) => it.classList.toggle('wp-item--on', n === idx))
}

// 지정한 인덱스가 중앙 밴드에 오도록 스크롤 위치를 정렬한다.
export function scrollToIndex(el, idx) {
  if (!el) return
  el.scrollTop = Math.max(0, idx) * WHEEL_ITEM_H
  markCenter(el)
}

// 현재 중앙 밴드에 위치한 항목의 인덱스(범위 클램프).
export function centerIndex(el, len) {
  if (!el) return 0
  const idx = Math.round(el.scrollTop / WHEEL_ITEM_H)
  return Math.max(0, Math.min(len - 1, idx))
}

// 스크롤이 멈추면(settle) 가장 가까운 항목으로 스냅하고 onSettle 을 호출한다.
// 반환값: 리스너 해제 함수(컴포넌트 unmount/시트 닫힘 시 호출).
export function attachWheelScroll(el, onSettle) {
  if (!el) return () => {}
  let timer = null
  const handler = () => {
    markCenter(el)
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      el.scrollTop = Math.round(el.scrollTop / WHEEL_ITEM_H) * WHEEL_ITEM_H
      markCenter(el)
      if (onSettle) onSettle()
    }, 110)
  }
  el.addEventListener('scroll', handler, { passive: true })
  return () => {
    el.removeEventListener('scroll', handler)
    if (timer) clearTimeout(timer)
  }
}
