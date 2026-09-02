/**
 * 위치정보 동의(005) 공통 유틸 — 위치정보 동의철회·중지 S4.
 *
 * <h3>왜 필요한가</h3>
 * 위치정보 동의를 철회/중지한 사용자는 출퇴근·TBM 입실이 서버에서 차단된다
 * (전용 오류코드 LOCATION_403_001). 그때 그냥 오류 메시지만 띄우면 사용자는
 * "왜 안 되는지"는 알아도 "어디서 푸는지"를 모른다. 안내 후 동의 설정으로 데려간다.
 *
 * ★로그인 자체는 막지 않는다(005 는 서버에서 LOGIN_GATE_YN='N').
 *   앱에 들어올 수 있어야 재동의를 할 수 있기 때문이다. 차단은 이벤트 시점에서만 한다.
 */

/** 서버 전용 오류코드 — 이 코드를 받으면 재동의로 유도한다. */
export const LOCATION_CONSENT_ERROR_CODE = 'LOCATION_403_001'

/** 위치정보 동의 설정 화면(마이페이지) 경로. */
export const LOCATION_CONSENT_ROUTE = '/MyPage'

/** 이벤트 차단 시 사용자에게 보여줄 안내(확인 시 동의 설정으로 이동). */
export const LOCATION_CONSENT_GUIDE =
  '위치정보 제공 및 이용에 동의해야 사용할 수 있는 기능이에요.\n\n' + '동의 설정으로 이동할까요?'

/** 서버가 위치정보 미동의로 차단했는가. */
export function isLocationConsentError(err) {
  return err?.response?.data?.errorCode === LOCATION_CONSENT_ERROR_CODE
}

/** 위치정보 동의 4-state 표시 문구. */
export const LOCATION_STATE_LABEL = {
  AGREED: '동의함',
  SUSPENDED: '일시 중지됨',
  PENDING_REAGREE: '재동의 필요',
  WITHDRAWN: '동의 철회됨',
}

/**
 * 동의 해제 시트(LocationConsentOffSheet)의 선택지 설명.
 *
 * ★상태 문구를 상시 노출하는 대신, 사용자가 해제를 시도하는 그 시점에 보여 준다
 *   (2026-09-02 UI 개선 — 마이페이지 상시 설명문 LOCATION_STATE_DESC 폐지).
 *   중지와 철회의 차이는 "과거 기록이 어떻게 되는가" 하나이고, 그 차이를 모른 채
 *   철회를 누르면 되돌릴 수 없다. 그래서 두 선택지에 각각 붙여 나란히 읽히게 한다.
 * ★아래 LOCATION_*_CONFIRM 과 같은 사실을 말해야 한다(문구가 갈리면 고지가 어긋난다).
 */
export const LOCATION_SUSPEND_POINTS = [
  '지금까지의 기록은 그대로 남습니다',
  '이후 위치는 수집하지 않습니다',
  '다시 동의하기 전까지 출퇴근·TBM 참석을 사용할 수 없습니다',
]

export const LOCATION_WITHDRAW_POINTS = [
  '지금까지 수집된 위치정보가 모두 삭제됩니다',
  '삭제된 기록은 복구할 수 없습니다',
  '출퇴근·TBM 참석 등 주요 기능을 사용할 수 없습니다',
]

/** 철회 전 반드시 보여줄 경고 — ★되돌릴 수 없다는 사실을 앞에 둔다. */
export const LOCATION_WITHDRAW_CONFIRM =
  '위치정보 동의를 철회할까요?\n\n' +
  '· 지금까지 수집된 위치정보가 모두 삭제됩니다\n' +
  '· 삭제된 기록은 복구할 수 없습니다\n' +
  '· 출퇴근·TBM 참석 등 주요 기능을 사용할 수 없습니다\n\n' +
  '기록을 남겨 두고 앞으로만 수집을 멈추려면 [일시 중지]를 이용해 주세요.'

/** 중지 전 확인 — 되돌릴 수 있으므로 철회보다 가볍게 안내한다. */
export const LOCATION_SUSPEND_CONFIRM =
  '위치정보 수집을 일시 중지할까요?\n\n' +
  '· 지금까지의 기록은 그대로 남습니다\n' +
  '· 다시 동의하기 전까지 출퇴근·TBM 참석을 사용할 수 없습니다'
