/**
 * leaveFormat.js — 연차 일수 표기 공용 유틸 (연차 시간차 환산 개편 LC-10·LC-11)
 *
 * 표기 규칙(plan §5-B·§5-E — 웹과 동일):
 *   - 잔여/사용/부여 등 일수 수치의 소수점 노출 전면 금지.
 *   - 정수부 = "N일", 소수부 × convMinutes(1일 환산시간, 분) = "H시간 M분". 0시간 0분이면 "N일"만.
 *   - convMinutes 미제공/무효 시 480 폴백(8시간 사업장 기본).
 *   - 정렬/계산 내부값(days)은 그대로 유지하고 "표시만" 이 유틸로 교체한다.
 */

const DEFAULT_CONV_MINUTES = 480

// 1일 환산시간(분) 정규화 — 무효/미제공이면 480 폴백.
const resolveConv = (convMinutes) => {
  const conv = Number(convMinutes)
  return Number.isFinite(conv) && conv > 0 ? Math.trunc(conv) : DEFAULT_CONV_MINUTES
}

// 일수를 {일, 분} 으로 분해 — 부동소수 오차 방어(분 반올림 후 분모 도달 시 일 승급).
const decompose = (days, convMinutes) => {
  const n = Number(days)
  if (!Number.isFinite(n)) return { negative: false, dayPart: 0, minutes: 0 }
  const conv = resolveConv(convMinutes)
  const negative = n < 0
  const abs = Math.abs(n)
  let dayPart = Math.floor(abs)
  let minutes = Math.round((abs - dayPart) * conv)
  if (minutes >= conv) {
    dayPart += 1
    minutes = 0
  }
  return { negative, dayPart, minutes }
}

/**
 * 일수 → "N일 H시간 M분" 텍스트.
 * @param {number|string} days 일수(소수 가능, 음수는 '-' 부호 유지)
 * @param {number} [convMinutes] 1일 환산시간(분). 미제공 시 480 폴백
 * @returns {string} 예: formatLeaveDays(13.4375, 480) → "13일 3시간 30분" / 정수는 "13일"
 */
export function formatLeaveDays(days, convMinutes) {
  const { negative, dayPart, minutes } = decompose(days, convMinutes)
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  let text = `${dayPart}일`
  if (h > 0) text += ` ${h}시간`
  if (m > 0) text += ` ${m}분`
  return (negative ? '-' : '') + text
}

/**
 * 일수 표기를 큰 숫자(일)와 부가 텍스트(시간·분)로 분리 — 카드 대형 숫자 레이아웃용.
 * @returns {{ dayText: string, subText: string }} 예: { dayText: '13', subText: '3시간 30분' } (부가 없으면 '')
 */
export function splitLeaveDays(days, convMinutes) {
  const { negative, dayPart, minutes } = decompose(days, convMinutes)
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  const parts = []
  if (h > 0) parts.push(`${h}시간`)
  if (m > 0) parts.push(`${m}분`)
  return {
    dayText: (negative ? '-' : '') + String(dayPart),
    subText: parts.join(' '),
  }
}

/**
 * 분 원본 → "H시간 M분" (시간차 사용분 병기 — LC-11. 예: 90 → "1시간 30분").
 * 0 이하/무효는 "0분" (호출측이 0 이면 보통 미노출).
 */
export function formatMinutesToHm(minutes) {
  const n = Math.trunc(Number(minutes))
  if (!Number.isFinite(n) || n <= 0) return '0분'
  const h = Math.floor(n / 60)
  const m = n % 60
  if (h > 0 && m > 0) return `${h}시간 ${m}분`
  if (h > 0) return `${h}시간`
  return `${m}분`
}

/**
 * 원시 차감 일수(decimal(8,5) 직렬화 값)의 병기 표기 — 예상 차감 카드 "(0.5일)" 용.
 * 후행 0 제거(0.29170 → "0.2917"). 무효는 "0".
 */
export function trimRawDays(days) {
  const n = Number(days)
  if (!Number.isFinite(n)) return '0'
  // decimal(8,5) 범위 — 소수 5자리에서 반올림 후 후행 0 제거.
  return String(parseFloat(n.toFixed(5)))
}
