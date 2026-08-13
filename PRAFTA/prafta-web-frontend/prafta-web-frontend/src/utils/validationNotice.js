/**
 * 검증 결과 수집기 — "발견 즉시 팝업" 대신 "수집 후 1회 표시".
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ★표시 규약 (신규/수정 코드는 반드시 따른다)
 * ─────────────────────────────────────────────────────────────────────────────
 *  1) 검증은 발견 즉시 팝업하지 말고 수집한 뒤 1회만 표시한다.
 *  2) 차단(blocking)은 목록형 alert 1회로 표시하고 그 자리에서 흐름을 끝낸다.
 *     - 다만 "첫 위반에서 즉시 alert + return" 하는 기존 차단 코드는 그대로 두어도 된다
 *       (팝업이 1회만 뜨므로 연쇄가 아니다). 차단을 여러 건 모아 보여줄 때만 block() 을 쓴다.
 *  3) 비차단 안내(경고·확인 요청)는 절대 단독 팝업하지 않는다. 저장/삭제 컨펌 1회에 병합한다.
 *  4) 저장 완료/오류 알림(성공 alert, catch alert)은 수집 대상이 아니다. 종전대로 둔다.
 *  5) 수집한 문구는 기존 문자열을 문자 그대로 옮긴다. 요약·어미 통일·재작성 금지.
 *
 * 배경: 한 핸들러 안에서 `검사 → await alert → 검사 → await confirm` 순차 호출이 이어지면
 * 사용자는 팝업을 2~3번 닫아야 저장에 도달한다. 전역 Alert 컴포넌트를 고쳐도 병합할 수 없다
 * (뒤 팝업은 앞 팝업이 닫혀야 발화하므로). 해결은 호출 패턴 전환뿐이다.
 * 기준 사용처: `attd/popup/SchInfoPop.vue` fnSave (커밋 8c284235 의 인라인 구현을 본 유틸로 이관).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 왜 "주입형"인가 (전역 $alert/$confirm 을 감싸지 않는 이유)
 * ─────────────────────────────────────────────────────────────────────────────
 *  - `src/utils/` 모듈은 Vue 인스턴스에 접근할 수 없다. `getCurrentInstance()` 는 setup
 *    컨텍스트에서만 유효하므로 유틸이 스스로 `$alert` 을 잡을 수 없다.
 *  - 전역 싱글턴 등록 경로(`utils/alertUtil.js` / `plugins/confirm.js`)는 현재 사실상
 *    죽은 코드라 되살리는 것 자체가 별건 리스크다.
 *  - 앱(prafta-app-frontend)에는 전역 래퍼 관례가 없고 화면마다 `showAlert`/`showConfirm`/
 *    `askConfirm` 을 재선언한다(폴백 분기 포함). 주입형이면 그 래퍼를 그대로 넘기면 된다.
 *  - 표시 시점 제어권(컨펌 취소 시 이탈, `variant:'danger'`, 성공 알림 순서)은 핸들러 고유
 *    흐름이다. 유틸이 표시를 독점하면 화면별 재작성이 오히려 커진다.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 사용법 (웹)
 * ─────────────────────────────────────────────────────────────────────────────
 *   const { proxy } = getCurrentInstance();
 *   const notices = createNotices({ alert: proxy.$alert, confirm: proxy.$confirm });
 *   // ... 기존 검증 로직 그대로. 비차단 안내만 warn()/note() 로 수집 ...
 *   if (!(await notices.resolve(getMessage(MSG.SAVE_CONFIRM)))) return;
 *
 * `$alert/$confirm` 은 `app.config.globalProperties` 에 화살표 함수로 등록돼 있어
 * `this` 바인딩 없이 구조분해해도 안전하다(`plugins/alert.js` 확인).
 *
 * 사용법 (앱): 뷰의 기존 로컬 래퍼를 그대로 주입한다.
 *   const notices = createNotices({ alert: showAlert, confirm: askConfirm })
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 문구 규격
 * ─────────────────────────────────────────────────────────────────────────────
 *   [안내 통합 컨펌]                    [차단 목록형 alert]
 *   다음 사항을 확인해 주세요.           다음 항목을 먼저 확인해 주세요.
 *
 *   · {안내1}                           · {차단1}
 *   · {안내2}                           · {차단2}
 *
 *   {기존 저장/삭제 컨펌 문구 원문}
 *
 *  - 항목이 1건뿐이면 헤더/불릿 없이 원문 단건 문구만 출력한다(문구 보존 최우선).
 *    단 `alwaysList: true` 를 주면 1건이어도 헤더+불릿 형태를 유지한다
 *    (SchInfoPop 처럼 이미 목록형으로 노출 중인 화면의 표시 결과를 그대로 보존하기 위함).
 *  - `\n` 은 AlertModal/ConfirmModal 이 `white-space: pre-line` 으로 렌더한다.
 */

/** 비차단 안내 목록 헤더 */
const NOTICE_HEADER = "다음 사항을 확인해 주세요.";
/** 차단 항목 목록 헤더 */
const BLOCKING_HEADER = "다음 항목을 먼저 확인해 주세요.";
/** 목록 불릿 */
const BULLET = "· ";

/**
 * 수집 항목 정규화 — null/undefined/공백 문자열만 걸러낸다.
 * (문구 보존을 위해 내용 자체는 절대 가공하지 않는다. trim 은 판정용으로만 쓴다.)
 *
 * @param {Array<string>} items
 * @returns {Array<string>}
 */
function normalizeItems(items) {
  if (!Array.isArray(items)) return [];
  return items.filter(
    (item) => typeof item === "string" && item.trim() !== ""
  );
}

/**
 * 목록 본문 조립.
 *
 * @param {Array<string>} items 정규화된 항목
 * @param {string} header 목록 헤더
 * @param {boolean} alwaysList 1건일 때도 헤더/불릿을 유지할지
 * @returns {string}
 */
function renderSection(items, header, alwaysList) {
  if (items.length === 0) return "";
  if (items.length === 1 && !alwaysList) return items[0];
  return (
    header + "\n\n" + items.map((item) => BULLET + item).join("\n")
  );
}

/**
 * 안내 목록 + 기본 컨펌 문구를 합쳐 통합 컨펌 메시지를 만든다(표시 없음 — 순수 함수).
 *
 * @param {Array<string>} notices 비차단 안내 목록
 * @param {string} baseMessage 기존 저장/삭제 컨펌 문구 원문
 * @param {{ alwaysList?: boolean }} [options]
 * @returns {string}
 */
export function buildNoticeMessage(notices, baseMessage, options = {}) {
  const items = normalizeItems(notices);
  const base = typeof baseMessage === "string" ? baseMessage : "";
  if (items.length === 0) return base;

  const section = renderSection(items, NOTICE_HEADER, !!options.alwaysList);
  return base ? section + "\n\n" + base : section;
}

/**
 * 차단 항목 목록형 alert 메시지를 만든다(표시 없음 — 순수 함수).
 *
 * @param {Array<string>} blockings 차단 항목 목록
 * @param {{ alwaysList?: boolean }} [options]
 * @returns {string}
 */
export function buildBlockingMessage(blockings, options = {}) {
  const items = normalizeItems(blockings);
  if (items.length === 0) return "";
  return renderSection(items, BLOCKING_HEADER, !!options.alwaysList);
}

/**
 * 검증 결과 수집기 생성.
 *
 * @param {{
 *   alert: (message: string) => Promise<any>,
 *   confirm: (message: string, options?: object) => Promise<boolean>
 * }} io 표시 함수 주입(웹: proxy.$alert / proxy.$confirm, 앱: 뷰의 로컬 래퍼)
 * @returns {{
 *   block: (message: string) => any,
 *   warn: (message: string) => any,
 *   note: (message: string) => any,
 *   hasBlocking: () => boolean,
 *   hasNotice: () => boolean,
 *   list: () => { blocking: Array<string>, notices: Array<string> },
 *   resolve: (baseConfirmMessage: string, options?: object) => Promise<boolean>
 * }}
 */
export function createNotices(io = {}) {
  const blocking = [];
  const notices = [];

  const collect = (bucket, message) => {
    if (typeof message !== "string" || message.trim() === "") return api;
    bucket.push(message);
    return api;
  };

  const api = {
    /** 차단 — 하나라도 있으면 저장/전송 불가 */
    block: (message) => collect(blocking, message),
    /** 비차단 경고 — 통합 컨펌 목록에 포함 */
    warn: (message) => collect(notices, message),
    /** 비차단 확인 요청(자정 넘김 등) — 통합 컨펌 목록에 포함 */
    note: (message) => collect(notices, message),
    hasBlocking: () => blocking.length > 0,
    hasNotice: () => notices.length > 0,
    /** qa/테스트용 스냅샷(수집 순서 그대로) */
    list: () => ({ blocking: blocking.slice(), notices: notices.slice() }),

    /**
     * 수집 결과를 1회만 표시하고 진행 여부를 돌려준다.
     *
     *  - 차단 ≥ 1        : 목록형 alert 1회 → false 반환(호출부는 그대로 return)
     *  - 차단 0, 안내 ≥ 1 : 통합 컨펌 1회 → 사용자 응답 반환
     *  - 차단 0, 안내 0   : 기본 컨펌 1회 → 사용자 응답 반환
     *
     * @param {string} baseConfirmMessage 기존 저장/삭제 컨펌 문구 원문
     * @param {{ variant?: string, alwaysList?: boolean }} [options]
     *        confirm 에 그대로 전달된다(예: `{ variant: 'danger' }`).
     * @returns {Promise<boolean>} 진행 여부(false 면 호출부는 즉시 중단)
     */
    resolve: async (baseConfirmMessage, options = {}) => {
      if (blocking.length > 0) {
        await io.alert(buildBlockingMessage(blocking, options));
        return false;
      }
      const message = buildNoticeMessage(
        notices,
        baseConfirmMessage,
        options
      );
      return await io.confirm(message, options);
    },
  };

  return api;
}
