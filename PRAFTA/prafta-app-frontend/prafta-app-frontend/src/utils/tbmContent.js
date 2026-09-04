/**
 * TBM 교육내용(contentBody) 표시용 HTML 변환.
 *
 * 교육내용은 CONTENT_FORMAT_CD=RICH_HTML 로 저장된다. 실제 값은 두 갈래다.
 *   1) 리치 HTML — web tbm02 QuillEditor 입력분, tbmai02 가 생성한 교육안(h4/p/ul/li/br).
 *   2) 순수 텍스트 — 앱 관리자 등록 화면(AdminTbmCreateForm)의 textarea 입력분(줄바꿈 포함).
 *
 * 1) 을 텍스트로 그리면 태그가 그대로 노출되고, 2) 를 HTML 로 그리면 줄바꿈이 사라진다.
 * 그래서 태그 유무로 갈라 처리한다. 반환값은 항상 v-html 로 렌더한다.
 *
 * ★서버(AppAdminTbmServiceImpl / AppTbm01ServiceImpl)가 응답 직전 Jsoup Safelist.relaxed 로
 *   저장형 XSS 를 정화한다(TbmContentSanitizer 단일 출처). 프론트는 별도 sanitize 를 하지 않는다
 *   (이중 방어 아님, 서버 단일 방어 — 근로자 화면과 동일 규약).
 */

// 태그로 볼 수 있는 패턴: <p>, <br/>, <ul >, </li> 등 여는·닫는 태그.
const HTML_TAG_PATTERN = /<\/?[a-z][a-z0-9]*(\s[^<>]*)?\/?>/i

/** 순수 텍스트를 HTML 로 안전하게 옮긴다(태그 오인 방지 + 줄바꿈 보존). */
function escapeToHtml(text) {
  const escaped = String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  return escaped.replace(/\r\n|\r|\n/g, '<br>')
}

/**
 * 교육내용을 v-html 에 넣을 문자열로 변환한다.
 *
 * @param {string} body 서버가 내려준 contentBody(정화 완료)
 * @returns {string} HTML 문자열. 값이 없으면 빈 문자열.
 */
export function toTbmContentHtml(body) {
  if (!body) return ''
  const text = String(body)
  return HTML_TAG_PATTERN.test(text) ? text : escapeToHtml(text)
}

/**
 * 교육내용에서 사람이 읽는 글자만 뽑는다(빈 입력 판정·글자수 검사용).
 *
 * Quill 은 내용을 지워도 `<p><br></p>` 같은 빈 껍데기를 남기므로, 문자열 길이로 빈 입력을
 * 판정하면 항상 "입력됨"이 된다. 태그·엔티티를 걷어낸 실제 글자로 판정해야 한다.
 *
 * @param {string} body contentBody(HTML 또는 순수 텍스트)
 * @returns {string} 태그를 제거하고 공백을 정리한 텍스트
 */
export function toTbmContentText(body) {
  if (!body) return ''
  return String(body)
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/\s+/g, ' ')
    .trim()
}

export default { toTbmContentHtml, toTbmContentText }
