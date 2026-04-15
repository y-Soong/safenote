/**
 * code - message 형태 메시지 관리
 * Java enum처럼 코드값으로 메시지를 조회하여 alert/confirm에 사용
 */
import { COMMON_MESSAGES } from "./common.js";
import { BAIM_MESSAGES } from "./baim.js";
import { USER_MESSAGES } from "./user.js";
import { CHKLST_MESSAGES } from "./chkLst.js";
import { ATTD_MESSAGES } from "./attd.js";
import { RISK_MESSAGES } from "./risk.js";
import { TBM_MESSAGES } from "./tbm.js";
import { LOGIN_MESSAGES } from "./login.js";

/** 도메인별 메시지 통합 */
const MESSAGES = {
  ...COMMON_MESSAGES,
  ...BAIM_MESSAGES,
  ...USER_MESSAGES,
  ...CHKLST_MESSAGES,
  ...ATTD_MESSAGES,
  ...RISK_MESSAGES,
  ...TBM_MESSAGES,
  ...LOGIN_MESSAGES,
};

/**
 * 코드로 메시지 조회
 * @param {string} code - 메시지 코드 (예: ASSIGN_MANAGER_CONFIRM)
 * @param {Object} [params] - 치환할 값 { siteNm: "중곡사업장" } 등
 * @returns {string}
 */
export function getMessage(code, params = {}) {
  const msg = MESSAGES[code];
  if (msg == null) return String(code);

  if (Object.keys(params).length === 0) return msg;

  return Object.entries(params).reduce(
    (str, [key, val]) =>
      str.replace(new RegExp(`\\{${key}\\}`, "g"), String(val ?? "")),
    msg
  );
}

/** 메시지 코드 상수 (자동완성/오타 방지) */
export const MSG = Object.freeze(
  Object.fromEntries(Object.keys(MESSAGES).map((k) => [k, k]))
);
