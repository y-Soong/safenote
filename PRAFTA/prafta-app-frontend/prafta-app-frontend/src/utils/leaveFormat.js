/**
 * leaveFormat.js — 연차 일수 표기 공용 유틸 (연차 시간차 환산 개편 LC-10·LC-11)
 *
 * 표기 규칙(2026-08-09 규약 반영 — 웹과 동일):
 *   - [2026-08-09 규약] 날짜 미정 문맥(잔여/부여/사용예정/한도)은 시간·분 환산 금지 —
 *     일 단위 단독 표기(formatLeaveDaysOnly/splitLeaveDaysOnly). 일 단위 소수 표기 허용.
 *   - "N일 H시간 M분" 환산 표기(formatLeaveDays/splitLeaveDays)는 날짜 확정 문맥(E1 당일분모
 *     conv) 전용으로 존치 — E4 참고 분모를 넘겨 호출하는 것 금지.
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
 * ⚠️ [2026-08-09 규약] E1(당일분모) 문맥 전용 — 날짜가 확정되어 그날 스케줄 conv 로 환산하는
 *   표기(preview 폴백 등)에만 사용한다. E4 참고 분모(요약 API convMinutes)를 넘겨 날짜 미정
 *   잔여/부여 표기에 호출하는 것 금지 → formatLeaveDaysOnly 사용.
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
 * ⚠️ [2026-08-09 규약] E1(당일분모) 문맥 전용 — E4 참고 분모로 호출 금지(formatLeaveDays 와 동일).
 *   날짜 미정 잔여/부여의 대형 숫자 표기는 splitLeaveDaysOnly 사용.
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
 * 일수 → 일 단위 단독 표기 "N일" (2026-08-09 규약 — E4 시간 환산 제거).
 *   소수 2자리 반올림 + 후행 0 제거 (attdFormat.formatLeaveDays 의 "앱 표시 자릿수 단일 출처"
 *   규칙과 통일 — 캘린더 마커 "0.19일"·홈 잔여 표기와 시각적 정합).
 *   날짜 미정 문맥(잔여/부여/사용예정/한도)의 유일한 표기 함수. 인라인 포맷 금지.
 *   TODO(단시간근로자 시간 단위 부여): 표기 분기는 반드시 이 함수에 추가한다
 *   (시그니처 확장 시 두 번째 인자는 옵션 객체로).
 * @param {number|string|null} days
 * @returns {string} 예: 0.5 → "0.5일", 13.4375 → "13.44일", -0.5 → "-0.5일", 무효 → "0일"
 */
export function formatLeaveDaysOnly(days) {
  const n = Number(days)
  if (!Number.isFinite(n)) return '0일'
  return `${String(Number(n.toFixed(2)))}일`
}

/**
 * 대형 숫자 레이아웃용 분리형 — splitLeaveDays 와 반환 형태 동일({ dayText, subText }).
 *   subText 는 항상 ''(시간·분 환산 없음 — 2026-08-09 규약). 단위("일")는 마크업 소유.
 *   자릿수 규칙은 formatLeaveDaysOnly 와 동일(2자리 반올림 + 후행 0 제거).
 */
export function splitLeaveDaysOnly(days) {
  const n = Number(days)
  return { dayText: Number.isFinite(n) ? String(Number(n.toFixed(2))) : '0', subText: '' }
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

// 반차 일수 합계 → 건수. 반차는 항상 정확히 0.5일이므로 1:1 환산이며,
//   짜투리 분할차감으로 한 반차가 여러 행이 되어도 일수 합은 0.5 로 보존된다(서버가 SUM 으로 내림).
//   부동소수 오차(0.49999…)를 흡수하기 위해 반올림한다. 무효/0 이하는 0.
const toHalfDayCount = (halfDayDays) => {
  const n = Number(halfDayDays)
  if (!Number.isFinite(n) || n <= 0) return 0
  return Math.round(n / 0.5)
}

// 시간차 실사용 분 정규화 — 무효/0 이하는 0.
const toHourlyMinutes = (minutes) => {
  const n = Math.trunc(Number(minutes))
  return Number.isFinite(n) && n > 0 ? n : 0
}

/**
 * HB-13 §20-2(B안) 공통 조립부 — 텍스트형/분리형 표기가 같은 규칙을 쓰도록 단일화.
 *   반환 null = 부가 항목(반차·시간차)이 전혀 없음 → 호출측은 기존 표기로 그대로 폴백(회귀 방지).
 *   dayPart 는 "days − 반차분" 의 정수부다(반차 이중 계상 방지 — 아래 formatLeaveDaysWithHourly 주석 참조).
 */
const buildHourlyParts = (days, convMinutes, hourlyMinutes, halfDayDays) => {
  const hm = toHourlyMinutes(hourlyMinutes)
  const halfCount = toHalfDayCount(halfDayDays)
  if (hm <= 0 && halfCount <= 0) return null

  const parts = []
  if (halfCount > 0) parts.push(`반차 ${halfCount}회`)
  if (hm > 0) parts.push(`시간차 ${formatMinutesToHm(hm)}`)

  // 반차분(건수 × 0.5 — 서버 원값 대신 환산 건수로 재구성해 부동소수 잔차를 제거)을 뺀 나머지의 정수부.
  //   ★ NEW-1: 서버가 반차 일수를 그룹 일수(groups.used/planned)와 "동일한 활성 부여 모수"로 집계하므로
  //   원값 기준으로는 반차 일수 ≤ 그룹 일수(부분집합)가 보장된다 → days − 반차일수 는 음수가 될 수 없다.
  //   남는 하한 필요성은 여기서 건수 환산(0.5 반올림)을 하기 때문이며, 편차는 최대 0.25일로 유한하다
  //   (반차 한 건이 분할차감으로 쪼개져 일부 행만 활성 부여에 물린 경우). 구 회계연도 축에서
  //   발생하던 "정수부 통째 소멸"(rest 가 크게 음수) 경로는 제거됐다.
  const rest = Number(days) - halfCount * 0.5
  const { negative, dayPart } = decompose(rest > 0 ? rest : 0, convMinutes)
  return { negative, dayPart, sub: parts.join(', ') }
}

/**
 * HB-13(F-3, §20 재확정): 사용/사용예정 표기 — 반차·시간차를 정확값 그대로 별도 항목화한다(B안).
 *
 * 배경: 당일분모 전환(E1) 이후 시간차 차감 분모가 날마다 달라져(420/450/480…),
 *   일수 → 시간 역환산을 단일 분모(convMinutes)로 하면 실제 3시간이 "2시간 48분"으로 표시된다.
 *   같은 화면의 "시간차 사용 3시간" 과 모순되므로(잔결함 F-3) 역환산 자체를 제거한다.
 *   1차 구현(A안)은 일수 정수부만 써서 반차 0.5일이 표기에서 증발했다(§20-1) → B안으로 재구현.
 *
 * 표기 규칙(§20-2, LC-11 소수점 금지 유지 — "0.5일" 이 아니라 "N회"):
 *   | 정수부 > 0, 부가 있음 | "2일 (반차 1회, 시간차 3시간)" |
 *   | 정수부 > 0, 부가 없음 | "2.5일"      (formatLeaveDaysOnly 폴백 — 2026-08-09 규약) |
 *   | 정수부 0,  부가 있음 | "반차 1회" / "시간차 3시간" / "반차 1회, 시간차 3시간" ("0일" 접두 생략) |
 *   | 전부 0               | "0일"        (formatLeaveDaysOnly 폴백) |
 * 역환산 0 — 종일=일수(정확) · 반차=0.5일 고정(정확) · 시간차=실분(정확). 셋 다 서버 원값이다.
 *
 * ⚠️ "정수부"는 days 전체가 아니라 <b>반차 일수를 뺀 나머지</b>의 정수부다. days 는 반차분을 포함한
 *   합계이므로 그대로 floor 하면 반차가 앞의 "N일"과 뒤의 "반차 N회"에 이중 계상된다
 *   (예: 종일 0일 + 반차 3회 = days 1.5 → floor 그대로면 "1일 (반차 3회)" = 2.5일로 읽힘).
 *   반차분을 먼저 제거하면 "반차 3회"(=1.5일)로 정확해진다. §20-2 예시(2.90119, 반차 1회)는
 *   두 방식의 결과가 "2일"로 동일하다.
 *   남는 오차는 시간차의 일수 환산분뿐이며(서버가 시간차 "일수"는 내려주지 않는다), 이는 그 합이
 *   1일 이상 쌓일 때만 정수부를 밀어 올린다 — 기존 A안이 이미 감수하던 범위와 동일하다.
 *
 * @param {number|string} days 해당 구간(사용/사용예정) 일수 합계(반차·시간차 차감분 포함)
 * @param {number} [convMinutes] 1일 환산시간(분)
 * @param {number} [hourlyMinutes] 그 구간의 시간차 실사용 분(서버 권위값)
 * @param {number} [halfDayDays] 그 구간의 반차 사용 "일수" 합계(서버 권위값 — 건수 아님)
 */
export function formatLeaveDaysWithHourly(days, convMinutes, hourlyMinutes, halfDayDays) {
  const p = buildHourlyParts(days, convMinutes, hourlyMinutes, halfDayDays)
  // 부가 항목이 전혀 없으면 일 단위 단독 표기로 폴백(2026-08-09 규약 — 구 formatLeaveDays 폴백은
  //   E4 분모 환산이 노출되던 경로라 교체. convMinutes 는 dayPart 캐리 방어(decompose)에만 잔존).
  if (!p) return formatLeaveDaysOnly(days)
  // 정수부 0 이면 "0일" 접두를 생략한다(부호도 무의미하므로 붙이지 않는다).
  if (p.dayPart <= 0) return p.sub
  return `${p.negative ? '-' : ''}${p.dayPart}일 (${p.sub})`
}

/**
 * HB-13 §20-2(B안)의 <b>대형 숫자 레이아웃용</b> 변형 — splitLeaveDays 와 반환 형태가 같다
 *   ({ dayText, subText }). 마이페이지 연차 요약 카드처럼 "큰 숫자 + 보조 텍스트" 마크업이
 *   이미 고정된 소비처에서, 레이아웃을 바꾸지 않고 B안 부가 항목을 실을 수 있게 한다.
 *
 * 규칙(텍스트형 formatLeaveDaysWithHourly 와 동일 — 조립부 buildHourlyParts 공유):
 *   - 반차·시간차가 모두 0 이면 splitLeaveDaysOnly 와 <b>완전히 동일</b>(2026-08-09 규약 —
 *     구 splitLeaveDays 폴백은 E4 분모 환산이 노출되던 경로).
 *   - 부가 항목이 있으면 dayText = "days − 반차분" 의 정수부, subText = "반차 1회, 시간차 3시간".
 *     이때 소수부의 시간·분 역환산은 표기하지 않는다(F-3 의 원인 자체를 제거).
 *   - 텍스트형은 정수부 0 일 때 "0일" 접두를 생략하지만, 여기서는 단위("일")를 마크업이 소유하므로
 *     dayText 를 '0' 으로 유지한다("0일" + 보조행 "반차 1회" = 텍스트형과 동일한 의미).
 *
 * @returns {{ dayText: string, subText: string }}
 */
export function splitLeaveDaysWithHourly(days, convMinutes, hourlyMinutes, halfDayDays) {
  const p = buildHourlyParts(days, convMinutes, hourlyMinutes, halfDayDays)
  // 2026-08-09 규약: 부가 항목 없음 폴백을 일 단위 단독(splitLeaveDaysOnly)으로 교체(위와 동일 사유).
  if (!p) return splitLeaveDaysOnly(days)
  return {
    dayText: (p.negative ? '-' : '') + String(p.dayPart),
    subText: p.sub,
  }
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
