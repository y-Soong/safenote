/**
 * 웹 표시(presentation) 전용 날짜/시각 포맷 단일 출처 모듈 (prafta-com-014).
 *
 * 화면에 "사람이 읽는" 날짜/시각 텍스트를 점(.)·콜론(:)으로 통일한다.
 *  - 날짜: YYYY.MM.DD / MM.DD / YYYY.MM
 *  - 시각: HH:mm / HH:mm:ss
 *  - 날짜+시각: YYYY.MM.DD HH:mm (초 실재 시 HH:mm:ss)
 *
 * ⚠️ 위젯값(modelValue) 변환은 본 모듈의 책임이 아니다.
 *   - <input type=date> / CalendarSrch 등의 modelValue(대시 YYYY-MM-DD)는
 *     utils/common.js(formatDateString)·utils/noticeDate.js로 처리한다.
 *   - 본 모듈은 절대 점 텍스트를 위젯값으로 되돌려 보내지 않는다(표시 전용, D5).
 *
 * 모든 함수는 순수 함수이며, 파싱 불가/빈값 입력은 빈 문자열 또는 원본을 반환한다
 * (절대 "NaN"/"undefined" 출력 금지).
 */

// 숫자만 추출 (대시/슬래시/점/콜론/공백 등 구분자 제거)
function digitsOnly(value) {
  return String(value).replace(/\D/g, "");
}

/**
 * 날짜(YYYYMMDD / YYYY-MM-DD / YYYY.MM.DD / Date 등) → "YYYY.MM.DD".
 * 파싱 불가하면 원본 문자열을 그대로 반환한다.
 * @param {string|Date|null} value
 * @returns {string}
 */
export function formatYmdDot(value) {
  if (value == null || value === "") return "";
  // Date 객체는 로컬 기준 연월일로 변환
  if (value instanceof Date) {
    if (isNaN(value.getTime())) return "";
    const y = value.getFullYear();
    const m = String(value.getMonth() + 1).padStart(2, "0");
    const d = String(value.getDate()).padStart(2, "0");
    return `${y}.${m}.${d}`;
  }
  const d = digitsOnly(value);
  if (d.length >= 8) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)}`;
  }
  return String(value);
}

/**
 * 월-일(YYYYMMDD / MMDD / YYYY-MM-DD / Date 등) → "MM.DD".
 * 8자리 이상이면 월/일만, 4자리면 그대로 MM.DD 로 본다.
 * 파싱 불가하면 원본을 그대로 반환한다.
 * @param {string|Date|null} value
 * @returns {string}
 */
export function formatMdDot(value) {
  if (value == null || value === "") return "";
  if (value instanceof Date) {
    if (isNaN(value.getTime())) return "";
    const m = String(value.getMonth() + 1).padStart(2, "0");
    const d = String(value.getDate()).padStart(2, "0");
    return `${m}.${d}`;
  }
  const d = digitsOnly(value);
  if (d.length >= 8) {
    // YYYYMMDD... → MM.DD
    return `${d.slice(4, 6)}.${d.slice(6, 8)}`;
  }
  if (d.length === 4) {
    // MMDD → MM.DD
    return `${d.slice(0, 2)}.${d.slice(2, 4)}`;
  }
  return String(value);
}

/**
 * 연-월(YYYYMM / YYYY-MM / YYYY.MM 등) → "YYYY.MM".
 * 파싱 불가하면 원본을 그대로 반환한다.
 * @param {string|Date|null} value
 * @returns {string}
 */
export function formatYmDot(value) {
  if (value == null || value === "") return "";
  if (value instanceof Date) {
    if (isNaN(value.getTime())) return "";
    const y = value.getFullYear();
    const m = String(value.getMonth() + 1).padStart(2, "0");
    return `${y}.${m}`;
  }
  const d = digitsOnly(value);
  if (d.length >= 6) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}`;
  }
  return String(value);
}

/**
 * 시각(HHMM / HH:mm / HHMMSS / ISO / 타임스탬프 등) → "HH:mm".
 * - 4자리 이하 숫자(HHMM)는 시각으로 본다.
 * - 8자리 이상(YYYYMMDDHHmm...)은 날짜+시각으로 보고 시/분만 추출한다.
 * 파싱 불가하면 원본을 그대로 반환한다.
 * @param {string|null} value
 * @returns {string}
 */
export function formatHm(value) {
  if (value == null || value === "") return "";
  const str = String(value).trim();

  // "HH:mm" / "HH:mm:ss" 형태 (콜론 포함)
  if (/^\d{1,2}:\d{2}/.test(str)) {
    const parts = str.split(":");
    const hh = parts[0].padStart(2, "0");
    const mm = parts[1].slice(0, 2);
    return `${hh}:${mm}`;
  }

  const d = digitsOnly(str);
  if (d.length === 3) {
    // HMM → 0H:MM
    return `${d.slice(0, 1).padStart(2, "0")}:${d.slice(1, 3)}`;
  }
  if (d.length === 4) {
    // HHMM → HH:mm
    return `${d.slice(0, 2)}:${d.slice(2, 4)}`;
  }
  if (d.length === 6) {
    // 시각 컨텍스트에서만 호출되므로 HHMMSS 로 간주하여 시/분만 추출.
    return `${d.slice(0, 2)}:${d.slice(2, 4)}`;
  }
  if (d.length >= 12) {
    // YYYYMMDDHHmm... → HH:mm
    return `${d.slice(8, 10)}:${d.slice(10, 12)}`;
  }
  return String(value);
}

/**
 * 시각(HHMMSS / HH:mm:ss / ISO / 타임스탬프 등) → "HH:mm:ss".
 * - 초가 없으면 ":00" 을 붙이지 않고, 가능한 한 원본 정보를 보존하여 시/분만 채운다.
 * 파싱 불가하면 원본을 그대로 반환한다.
 * @param {string|null} value
 * @returns {string}
 */
export function formatHms(value) {
  if (value == null || value === "") return "";
  const str = String(value).trim();

  // "HH:mm:ss" / "HH:mm" 형태 (콜론 포함)
  if (/^\d{1,2}:\d{2}/.test(str)) {
    const parts = str.split(":");
    const hh = parts[0].padStart(2, "0");
    const mm = parts[1].slice(0, 2);
    const ss = parts[2] != null ? parts[2].slice(0, 2).padStart(2, "0") : "00";
    return `${hh}:${mm}:${ss}`;
  }

  const d = digitsOnly(str);
  if (d.length === 6) {
    // HHMMSS → HH:mm:ss
    return `${d.slice(0, 2)}:${d.slice(2, 4)}:${d.slice(4, 6)}`;
  }
  if (d.length === 4) {
    // HHMM → HH:mm:00
    return `${d.slice(0, 2)}:${d.slice(2, 4)}:00`;
  }
  if (d.length >= 14) {
    // YYYYMMDDHHmmss → HH:mm:ss
    return `${d.slice(8, 10)}:${d.slice(10, 12)}:${d.slice(12, 14)}`;
  }
  if (d.length >= 12) {
    // YYYYMMDDHHmm → HH:mm:00
    return `${d.slice(8, 10)}:${d.slice(10, 12)}:00`;
  }
  return String(value);
}

/**
 * 타임스탬프류(YYYYMMDDHHmm[ss] / YYYY-MM-DD HH:mm[:ss] / ISO 등) → "YYYY.MM.DD HH:mm".
 * 날짜만 있으면 날짜만 반환. 파싱 불가하면 원본을 그대로 반환한다.
 * @param {string|Date|null} value
 * @returns {string}
 */
export function formatDateTimeDot(value) {
  if (value == null || value === "") return "";
  if (value instanceof Date) {
    if (isNaN(value.getTime())) return "";
    const ymd = formatYmdDot(value);
    const hh = String(value.getHours()).padStart(2, "0");
    const mm = String(value.getMinutes()).padStart(2, "0");
    return `${ymd} ${hh}:${mm}`;
  }
  const d = digitsOnly(value);
  if (d.length >= 12) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)} ${d.slice(8, 10)}:${d.slice(10, 12)}`;
  }
  if (d.length >= 8) {
    return formatYmdDot(d);
  }
  return String(value);
}

/**
 * 타임스탬프류 → "YYYY.MM.DD HH:mm:ss" (초 실재 데이터 전용).
 * 초가 없으면 ":00" 을 채운다. 날짜만 있으면 날짜만 반환.
 * @param {string|Date|null} value
 * @returns {string}
 */
export function formatDateTimeDotWithSec(value) {
  if (value == null || value === "") return "";
  if (value instanceof Date) {
    if (isNaN(value.getTime())) return "";
    const ymd = formatYmdDot(value);
    const hh = String(value.getHours()).padStart(2, "0");
    const mm = String(value.getMinutes()).padStart(2, "0");
    const ss = String(value.getSeconds()).padStart(2, "0");
    return `${ymd} ${hh}:${mm}:${ss}`;
  }
  const d = digitsOnly(value);
  if (d.length >= 14) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)} ${d.slice(8, 10)}:${d.slice(10, 12)}:${d.slice(12, 14)}`;
  }
  if (d.length >= 12) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)} ${d.slice(8, 10)}:${d.slice(10, 12)}:00`;
  }
  if (d.length >= 8) {
    return formatYmdDot(d);
  }
  return String(value);
}

export default {
  formatYmdDot,
  formatMdDot,
  formatYmDot,
  formatHm,
  formatHms,
  formatDateTimeDot,
  formatDateTimeDotWithSec,
};
