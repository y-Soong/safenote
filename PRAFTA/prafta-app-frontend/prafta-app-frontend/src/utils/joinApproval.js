// joinApproval.js — 셀프가입(회원가입) 승인제 로그인 분기 판별 헬퍼.
//   작업 ID: 소정-12 (지시서 §셀프가입 승인/거부, plan §2)
//
//   배경: 셀프가입 계정은 가입 직후 바로 활성화되지 않고 관리자 승인을 기다린다.
//     ACCOUNT_STATUS[SYS013] '06 가입승인대기' / '07 가입거부' (마이그레이션 sojeong-1-5 적용 완료).
//     로그인 시도 시 서버가 이 상태를 알려 주면 앱은 로그인 대신 안내 화면(/JoinApprovalPending)으로 보낸다.
//
//   ★BE 응답 계약 가정 (소정-04 구현 중 — 확정 시 본 파일의 상수만 고치면 된다):
//     아래 세 형태를 모두 인식한다. 어느 하나라도 맞으면 안내 화면으로 분기한다.
//       (a) 200 응답 body 의 nextStep 값        — 기존 게이트 관례(PHONE_AUTH/DEFAULT_SCH/PASSWORD_CHANGE)
//       (b) 200/4xx body 의 accountStatus 값    — '06'/'07' (DDL 로 확정된 상수라 가장 안정적인 신호)
//       (c) 4xx body 의 errorCode 값            — 일용직 입장 승인대기(DAILYLOGIN_400_006/007) 관례 미러
//     (c) 의 코드값은 **가정**이다(LoginErrorCode 현재 최대 017 → 018/019 채번 예상).
//     실제 코드가 다르면 JOIN_PENDING_ERROR_CODES / JOIN_REJECTED_ERROR_CODES 만 교체한다.
//     어느 신호도 못 읽으면 기존 동작(서버 메시지 alert)으로 자연 폴백한다 — 오분기보다 안전.

/** 안내 화면 상태값. */
export const JOIN_APPROVAL_PENDING = 'PENDING'
export const JOIN_APPROVAL_REJECTED = 'REJECTED'

/** (a) 200 응답 nextStep 후보 — 서버 명명이 확정되면 실제 값만 남기면 된다. */
const PENDING_NEXT_STEPS = [
  'JOIN_APPROVAL',
  'JOIN_APPROVAL_PENDING',
  'JOIN_PENDING',
  'SIGNUP_PENDING',
]
const REJECTED_NEXT_STEPS = ['JOIN_APPROVAL_REJECTED', 'JOIN_REJECTED', 'SIGNUP_REJECTED']

/** (b) ACCOUNT_STATUS[SYS013] — DDL(sojeong-1-5)로 확정된 값. */
const PENDING_ACCOUNT_STATUS = '06'
const REJECTED_ACCOUNT_STATUS = '07'

/** (c) 로그인 거부 errorCode — ★가정값(BE 확정 시 여기만 교체). */
export const JOIN_PENDING_ERROR_CODES = ['LOGIN_400_018']
export const JOIN_REJECTED_ERROR_CODES = ['LOGIN_400_019']

/**
 * 로그인 응답(성공 body 또는 에러 body)에서 셀프가입 승인 상태를 판별한다.
 * @param {object|null|undefined} payload 응답 본문(response.data 또는 err.response.data)
 * @returns {'PENDING'|'REJECTED'|null} 해당 없으면 null(기존 흐름 유지)
 */
export function resolveJoinApprovalStatus(payload) {
  if (!payload || typeof payload !== 'object') return null

  const nextStep = typeof payload.nextStep === 'string' ? payload.nextStep : ''
  if (REJECTED_NEXT_STEPS.includes(nextStep)) return JOIN_APPROVAL_REJECTED
  if (PENDING_NEXT_STEPS.includes(nextStep)) return JOIN_APPROVAL_PENDING

  const accountStatus = payload.accountStatus == null ? '' : String(payload.accountStatus)
  if (accountStatus === REJECTED_ACCOUNT_STATUS) return JOIN_APPROVAL_REJECTED
  if (accountStatus === PENDING_ACCOUNT_STATUS) return JOIN_APPROVAL_PENDING

  const errorCode = typeof payload.errorCode === 'string' ? payload.errorCode : ''
  if (JOIN_REJECTED_ERROR_CODES.includes(errorCode)) return JOIN_APPROVAL_REJECTED
  if (JOIN_PENDING_ERROR_CODES.includes(errorCode)) return JOIN_APPROVAL_PENDING

  return null
}
