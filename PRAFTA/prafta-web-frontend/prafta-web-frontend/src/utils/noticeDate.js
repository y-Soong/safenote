// 공지사항(Notice) 날짜 변환 유틸 (PRAFTA-047).
// 백엔드는 팝업 기간을 YYYYMMDD(VARCHAR8)로 저장/반환하고,
// 공통 달력 컴포넌트(CalendarSrch)는 YYYY-MM-DD 문자열을 사용한다.
// 두 표현을 상호 변환한다.

/**
 * YYYY-MM-DD(또는 YYYYMMDD) → YYYYMMDD(대시 제거).
 * 빈값/널은 빈 문자열로 반환한다.
 * @param {string} value
 * @returns {string}
 */
export function ymdDashToCompact(value) {
  if (!value) return "";
  return String(value).replace(/-/g, "").trim();
}

/**
 * YYYYMMDD → YYYY-MM-DD(달력 입력용). 이미 대시가 있으면 그대로 반환한다.
 * 길이가 8이 아니면 원본을 반환한다(방어적).
 * @param {string} value
 * @returns {string}
 */
export function ymdCompactToDash(value) {
  if (!value) return "";
  const v = String(value).trim();
  if (v.includes("-")) return v;
  if (v.length !== 8) return v;
  return `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`;
}
