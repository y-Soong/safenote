export default {
  onlyAlphaNumeric,
  isEmpty,
  isNotEmpty,
  getToday,
  getTodayAndNextMonth,
  getFirstAndLastDayOfThisMonth,
  formatNumberWithComma,
  validatePhoneNumber,
  formatPhoneNumber,
  validateBizNumber,
  deepClone,
  validateEmail,
  validatePasswordRule,
  toUpperCase,
  toLowerCase,
  resetRefs,
  isValidbirthDtdate,
  toCamelCase,
  toCamelCaseKeys,
  isInteger,
  formatDateString,
  ymToDateRange,
};

export {
  onlyAlphaNumeric,
  isEmpty,
  isNotEmpty,
  getToday,
  getTodayAndNextMonth,
  getFirstAndLastDayOfThisMonth,
  formatNumberWithComma,
  validatePhoneNumber,
  formatPhoneNumber,
  validateBizNumber,
  deepClone,
  validateEmail,
  validatePasswordRule,
  toUpperCase,
  toLowerCase,
  resetRefs,
  isValidbirthDtdate,
  toCamelCase,
  toCamelCaseKeys,
  isInteger,
  formatDateString,
  ymToDateRange,
};

import { ref } from "vue";

const value = ref("");

function onlyAlphaNumeric(e) {
  // 영문자(a-z, A-Z)와 숫자(0-9)만 허용
  e.target.value = e.target.value.replace(/[^a-zA-Z0-9]/g, "");
  value.value = e.target.value;
}

// 1. 비어있는 값 체크
function isEmpty(value) {
  if (value == null) return true;
  if (typeof value === "string" && value.trim() === "") return true;
  if (Array.isArray(value) && value.length === 0) return true;
  if (typeof value === "object" && Object.keys(value).length === 0) return true;
  return false;
}

// 1. 비어있지 않은 값 체크
function isNotEmpty(value) {
  return !isEmpty(value);
}

// 로컬 달력일 YYYY-MM-DD 포맷 (2026-08-30 결함 수정 공용 헬퍼)
//   ★toISOString()은 UTC 기준이라 KST 00~09시에 전날 날짜를 반환하던 결함이 있었다.
//   반드시 로컬 구성요소(getFullYear/getMonth/getDate)로 조립한다(재발 금지 — toISOString 사용 금지).
function toLocalYmd(d) {
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

// 2. 오늘 날짜 반환 (로컬 기준 — KST 00~09시 하루 밀림 결함 수정)
function getToday() {
  return toLocalYmd(new Date());
}

// 3. 오늘 + 한달 뒤 날짜 반환 (로컬 기준)
function getTodayAndNextMonth() {
  const today = new Date();
  const nextMonth = new Date();
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  return {
    today: toLocalYmd(today),
    nextMonth: toLocalYmd(nextMonth),
  };
}

// 4. 현재 월의 첫날 + 마지막 날 (로컬 기준)
function getFirstAndLastDayOfThisMonth() {
  const now = new Date();
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    firstDay: toLocalYmd(firstDay),
    lastDay: toLocalYmd(lastDay),
  };
}

// 5. 숫자 천 단위 콤마
function formatNumberWithComma(num) {
  if (isNaN(num)) return "";
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// 6. 전화번호 형식 체크 (하이픈 포함 또는 10~11자리 숫자)
function validatePhoneNumber(phone) {
  // 1) 휴대폰: 010으로 시작, 10~11자리
  const mobileRegex = /^(010)(-?\d{3,4})(-?\d{4})$/;

  // 2) 지역번호: 02 또는 0으로 시작하는 2~3자리 지역번호
  //    예: 02-123-4567, 031-123-4567
  const areaRegex = /^(0(?:2|[3-6][1-5]|70|80|50))(-?\d{3,4})(-?\d{4})$/;

  return mobileRegex.test(phone) || areaRegex.test(phone);
}

// 6.1 전화번호 형식 지정 함수
function formatPhoneNumber(value) {
  if (isNotEmpty(value)) {
    const cleaned = value.replace(/\D+/g, "");

    // 1. 서울 (02) 지역번호
    if (cleaned.startsWith("02")) {
      if (cleaned.length <= 2) return cleaned;
      if (cleaned.length <= 5)
        return `${cleaned.slice(0, 2)}-${cleaned.slice(2)}`;
      if (cleaned.length <= 9)
        return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 6)}-${cleaned.slice(
          6
        )}`;
      return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 6)}-${cleaned.slice(
        6,
        10
      )}`;
    }

    // 2. 나머지 지역번호 및 휴대폰 (3자리 지역번호)
    if (cleaned.length <= 3) return cleaned;
    if (cleaned.length <= 6)
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3)}`;
    if (cleaned.length <= 10)
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}-${cleaned.slice(
        6
      )}`;
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(
      7,
      11
    )}`;
  }
  return null;
}

// 7. 사업자등록번호 형식 체크 (10자리 숫자)
function validateBizNumber(bizNum) {
  const regex = /^\d{10}$/;
  return regex.test(bizNum);
}

// 8. 객체 깊은 복사
function deepClone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

// 9. 이메일 형식 체크
function validateEmail(email) {
  const regex = /^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/;
  return regex.test(email);
}

// 10. 비밀번호 규칙 체크
// 숫자, 영문자, 특수문자 중 2가지 이상 포함 + 6~15자
function validatePasswordRule(password) {
  const lengthValid = /^.{6,15}$/.test(password);
  const types = [
    /[0-9]/.test(password),
    /[a-zA-Z]/.test(password),
    /[^a-zA-Z0-9]/.test(password),
  ];
  const typeCount = types.filter(Boolean).length;
  return lengthValid && typeCount >= 2;
}

// 11. 영문자 → 대문자
function toUpperCase(str) {
  return typeof str === "string" ? str.toUpperCase() : "";
}

// 12. 영문자 → 소문자
function toLowerCase(str) {
  return typeof str === "string" ? str.toLowerCase() : "";
}

// 13. 주어진 ref 객체들을 자동으로 초기화함
// @param {Object} refs - key: ref 객체 (ex: { a: ref(), b: ref() })
function resetRefs(refs) {
  if (!refs || typeof refs !== "object") return;

  for (const key in refs) {
    const val = refs[key].value;
    if (Array.isArray(val)) refs[key].value = [];
    else if (typeof val === "boolean") refs[key].value = false;
    else if (typeof val === "number") refs[key].value = 0;
    else refs[key].value = "";
  }
}

function isValidbirthDtdate(dateStr) {
  if (!/^\d{8}$/.test(dateStr)) return false; // 숫자 8자리인지 확인

  const year = parseInt(dateStr.substring(0, 4));
  const month = parseInt(dateStr.substring(4, 6)) - 1; // JS에서는 0~11월
  const day = parseInt(dateStr.substring(6, 8));

  const date = new Date(year, month, day);

  // 날짜 객체로 변환한 값이 입력값과 일치하는지 검사
  return (
    date.getFullYear() === year &&
    date.getMonth() === month &&
    date.getDate() === day
  );
}

function toCamelCase(str) {
  return str.toLowerCase().replace(/_([a-z])/g, (g) => g[1].toUpperCase());
}

function toCamelCaseKeys(obj) {
  if (Array.isArray(obj)) {
    return obj.map(toCamelCaseKeys);
  } else if (obj !== null && typeof obj === "object") {
    return Object.keys(obj).reduce((acc, key) => {
      acc[toCamelCase(key)] = toCamelCaseKeys(obj[key]);
      return acc;
    }, {});
  }
  return obj;
}

function isInteger(value) {
  // 값이 비어 있으면 false
  if (value === null || value === undefined || value === "") return false;

  // 숫자로 변환
  const num = Number(value);

  // NaN 체크 + 정수 여부 반환
  return Number.isInteger(num);
}

/**
 * 날짜 문자열을 포맷팅.
 *  - 'YYYYMMDD' → 'YYYY-MM-DD'
 *  - 'YYYYMM'   → 'YYYY-MM'
 *  - 그 외 형식은 그대로 반환
 */
function formatDateString(dateStr) {
  if (!dateStr) return "";

  // 숫자만 추출 (혹시 '-'나 '.'이 포함된 경우 제거)
  const cleaned = dateStr.replace(/[^0-9]/g, "");

  if (cleaned.length === 8) {
    // YYYYMMDD → YYYY-MM-DD
    return `${cleaned.substring(0, 4)}-${cleaned.substring(
      4,
      6
    )}-${cleaned.substring(6, 8)}`;
  } else if (cleaned.length === 6) {
    // YYYYMM → YYYY-MM
    return `${cleaned.substring(0, 4)}-${cleaned.substring(4, 6)}`;
  } else {
    // 그 외는 원본 그대로
    return dateStr;
  }
}

/**
 * 월(YYYY-MM) → 해당 월의 시작일/말일 기간 (위젯값 YYYY-MM-DD).
 *   PRAFTA-DASHBOARD-T1: 대시보드 기준월을 일자 기간 조회 화면에 주입할 때 사용.
 *  - 'YYYY-MM' → { fromDate: 'YYYY-MM-01', toDate: 'YYYY-MM-말일' }
 *  - 파싱 불가하면 null 반환
 */
function ymToDateRange(ym) {
  if (!ym) return null;
  const cleaned = String(ym).replace(/[^0-9]/g, "");
  if (cleaned.length < 6) return null;

  const year = Number(cleaned.substring(0, 4));
  const month = Number(cleaned.substring(4, 6));
  if (!year || month < 1 || month > 12) return null;

  const mm = String(month).padStart(2, "0");
  // new Date(y, m, 0) = m월의 마지막 날 (m은 1-base 그대로 사용)
  const lastDay = new Date(year, month, 0).getDate();
  return {
    fromDate: `${year}-${mm}-01`,
    toDate: `${year}-${mm}-${String(lastDay).padStart(2, "0")}`,
  };
}
