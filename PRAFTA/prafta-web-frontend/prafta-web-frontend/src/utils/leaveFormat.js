/**
 * 연차 일수 표기 공용 유틸 (연차 시간차 환산 개편 LC-09 → 2026-08-09 표기 규약 반영 — 앱과 동일).
 *
 *  - [2026-08-09 규약] 날짜 미정 문맥(잔여/부여/사용예정/한도)은 시간·분 환산 금지 —
 *    일 단위 단독 표기(formatLeaveDaysOnly/splitLeaveDaysOnly). 일 단위 소수 표기 허용.
 *  - "N일 H시간 M분" 환산 표기(formatLeaveDays)는 날짜 확정 문맥(E1 당일분모 conv) 전용으로
 *    존치 — E4 참고 분모(요약/대시보드 API convMinutes)를 넘겨 호출하는 것 금지.
 *  - convMinutes 미제공/비정상 시 480분(8시간) 폴백
 *
 * ⚠️ 표시 전용 모듈이다. 정렬/필터/합산 등 내부 계산은 원 수치(days)를 그대로 유지한다.
 */

const DEFAULT_CONV_MINUTES = 480;

/**
 * 연차 일수(소수 포함) → "N일 H시간 M분" 표기.
 * ⚠️ [2026-08-09 규약] E1(당일분모) 문맥 전용 — 날짜가 확정되어 그날 스케줄 conv 로 환산하는
 *   표기(preview·발동 당시 원장 conv 등)에만 사용한다. E4 참고 분모를 넘겨 날짜 미정
 *   잔여/부여 표기에 호출하는 것 금지 → formatLeaveDaysOnly 사용.
 * @param {number|string|null} days 일수 (예: 13.4375)
 * @param {number|string|null} convMinutes 1일 환산시간(분). 미제공 시 480 폴백
 * @returns {string} 예: 13.4375 · conv 480 → "13일 3시간 30분". 파싱 불가 → "0일"
 */
export function formatLeaveDays(days, convMinutes) {
  const n = Number(days);
  if (!Number.isFinite(n)) return "0일";

  const convNum = Number(convMinutes);
  const conv =
    Number.isFinite(convNum) && convNum > 0 ? convNum : DEFAULT_CONV_MINUTES;

  const sign = n < 0 ? "-" : "";
  const abs = Math.abs(n);
  let dayPart = Math.floor(abs);
  let minutes = Math.round((abs - dayPart) * conv);

  // 부동소수 반올림 캐리 방어 (예: 0.99999… × conv → conv 분 = 1일)
  if (minutes >= conv) {
    dayPart += 1;
    minutes = 0;
  }

  const h = Math.floor(minutes / 60);
  const m = minutes % 60;

  let out = `${dayPart}일`;
  if (h > 0) out += ` ${h}시간`;
  if (m > 0) out += ` ${m}분`;
  return sign + out;
}

/**
 * 일수 → 일 단위 단독 표기 "N일" (2026-08-09 규약 — E4 시간 환산 제거).
 *   소수 2자리 반올림 + 후행 0 제거 (앱 attdFormat.formatLeaveDays 의 "앱 표시 자릿수 단일 출처"
 *   규칙과 통일 — 앱·웹 화면 간 수치 표기 정합).
 *   날짜 미정 문맥(잔여/부여/사용예정/한도)의 유일한 표기 함수. 인라인 포맷 금지.
 *   TODO(단시간근로자 시간 단위 부여): 표기 분기는 반드시 이 함수에 추가한다
 *   (시그니처 확장 시 두 번째 인자는 옵션 객체로).
 * @param {number|string|null} days
 * @returns {string} 예: 0.5 → "0.5일", 13.4375 → "13.44일", -0.5 → "-0.5일", 무효 → "0일"
 */
export function formatLeaveDaysOnly(days) {
  const n = Number(days);
  if (!Number.isFinite(n)) return "0일";
  return `${String(Number(n.toFixed(2)))}일`;
}

/**
 * 대형 숫자 레이아웃용 분리형 — 앱 splitLeaveDays 와 반환 형태 동일({ dayText, subText }).
 *   subText 는 항상 ""(시간·분 환산 없음 — 2026-08-09 규약). 단위("일")는 마크업 소유.
 *   자릿수 규칙은 formatLeaveDaysOnly 와 동일(2자리 반올림 + 후행 0 제거).
 */
export function splitLeaveDaysOnly(days) {
  const n = Number(days);
  return { dayText: Number.isFinite(n) ? String(Number(n.toFixed(2))) : "0", subText: "" };
}

/**
 * 분 → "H시간 M분" 표기 (시간차 LEAVE_MINUTES 원본 병기용 — §5-B).
 * @param {number|string|null} minutes 분 (예: 90)
 * @returns {string} 예: 90 → "1시간 30분", 60 → "1시간", 30 → "30분". 0 이하/파싱 불가 → "0분"
 */
export function formatLeaveMinutes(minutes) {
  const n = Number(minutes);
  if (!Number.isFinite(n) || n <= 0) return "0분";
  const total = Math.round(n);
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h > 0 && m > 0) return `${h}시간 ${m}분`;
  if (h > 0) return `${h}시간`;
  return `${m}분`;
}

/**
 * 일수의 불필요한 끝자리 0 제거 표시 (예: 0.50000 → "0.5").
 * 예상 차감액 병기 "(0.5일)" 전용(§5-C) — 일반 잔여/사용/부여 표기에는 사용 금지.
 * @param {number|string|null} days
 * @returns {string}
 */
export function trimLeaveDays(days) {
  const n = Number(days);
  if (!Number.isFinite(n)) return "0";
  return String(parseFloat(n.toFixed(5)));
}

/**
 * v2(BW2-11) 휴게 넘김 요청 표기 3분기(P6, 앱 formatBrkWaiveText 와 동일 규칙).
 *   yn !== 'Y' → '' / min null(v1 전부 넘김) → "휴게 넘김 요청(전부)" / 0 → "휴게 없이 근무 요청(기록)" / N → "휴게 N분 넘김 요청".
 * @param {string|null} yn BRK_WAIVE_YN
 * @param {number|string|null} min BRK_WAIVE_MIN
 * @returns {string}
 */
export function formatBrkWaiveText(yn, min) {
  if (yn !== "Y") return "";
  if (min === null || min === undefined || min === "")
    return "휴게 넘김 요청(전부)";
  const n = Number(min);
  if (Number.isNaN(n)) return "휴게 넘김 요청(전부)";
  return n === 0 ? "휴게 없이 근무 요청(기록)" : `휴게 ${n}분 넘김 요청`;
}
