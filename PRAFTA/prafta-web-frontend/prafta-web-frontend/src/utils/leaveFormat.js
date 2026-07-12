/**
 * 연차 일수 표기 공용 유틸 (연차 시간차 환산 개편 LC-09 — 표기 규칙 §5-B).
 *
 * 잔여/사용/부여 일수의 소수점 노출을 전면 금지하고 "N일 H시간 M분"으로 조립한다.
 *  - 정수부 = "N일", 소수부 = ×convMinutes(1일 환산시간, 분) 환산 → "H시간 M분"
 *  - 0인 시간/분 단위는 생략 (예: "13일" / "13일 3시간" / "13일 30분" / "13일 3시간 30분")
 *  - convMinutes 미제공/비정상 시 480분(8시간) 폴백
 *
 * ⚠️ 표시 전용 모듈이다. 정렬/필터/합산 등 내부 계산은 원 수치(days)를 그대로 유지한다.
 */

const DEFAULT_CONV_MINUTES = 480;

/**
 * 연차 일수(소수 포함) → "N일 H시간 M분" 표기.
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
