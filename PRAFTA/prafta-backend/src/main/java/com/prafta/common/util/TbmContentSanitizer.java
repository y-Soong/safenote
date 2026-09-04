package com.prafta.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.util.StringUtils;

/**
 * TBM 교육내용(CONTENT_BODY) 저장형 XSS 정화 + 텍스트 추출 단일 출처.
 *
 * <p>교육내용은 CONTENT_FORMAT_CD=RICH_HTML 로 저장된다(web tbm02 QuillEditor 입력분,
 * tbmai02 가 생성한 교육안 HTML, 앱 관리자 QuillEditor 입력분 모두 동일 포맷 코드). 이 값을
 * 화면에서 HTML 로 렌더(v-html)하는 경로는 응답 직전 반드시 {@link #sanitize(String)} 를 거쳐야 한다.
 *
 * <p>a/img/표/서식 등 본문 서식을 보존해야 하므로 {@link Safelist#relaxed()} 를 기준으로 한다.
 * relaxed() 는 본문 서식 태그와 a/img(및 표) 를 허용하되, {@code <script>}·
 * {@code onerror}/{@code onclick} 등 이벤트 핸들러 속성·{@code javascript:} 스킴 링크는
 * 자동으로 제거한다.
 *
 * <p>★{@code class} 속성 허용 사유: Quill 은 정렬·들여쓰기를 {@code ql-align-center} 같은
 * 클래스로 표현한다. 기본 relaxed() 는 class 를 지우므로, 정화된 값을 앱 관리자 편집기로 다시
 * 불러와 저장하면 웹에서 지정한 정렬이 영구 소실된다. class 는 스크립트를 실행시킬 수 없어
 * XSS 표면을 넓히지 않으므로 허용해 왕복 손실을 막는다.
 *
 * <p>★분리 금지: 근로자 앱(AppTbm01ServiceImpl)과 앱 관리자(AppAdminTbmServiceImpl) 가
 * 같은 컬럼을 같은 방식으로 렌더하므로, 정화 기준이 경로별로 갈라지면 한쪽만 뚫린다.
 * 새 렌더 경로를 추가할 때도 이 유틸을 재사용한다.
 */
public final class TbmContentSanitizer {

    /** 본문 서식 + Quill 서식 클래스 보존 기준. Safelist 는 불변이 아니므로 매 호출 새로 만든다. */
    private static Safelist safelist() {
        return Safelist.relaxed().addAttributes(":all", "class");
    }

    /**
     * ★pretty-print 해제(중요). Jsoup 은 기본으로 정화 결과를 들여쓰기·줄바꿈이 들어간 형태로
     * 출력하는데, 그러면 블록 태그 사이에 원문에 없던 공백 텍스트 노드가 생긴다. 이 값을
     * 편집기(Quill)에 그대로 넣으면 본문 최상위에 붙은 공백 텍스트 노드를 편집기가 정리하면서
     * 인접한 목록 블록까지 함께 지워, 교육 내용 일부가 사라진다. 원문 구조를 그대로 보존한다.
     */
    private static Document.OutputSettings compactOutput() {
        return new Document.OutputSettings().prettyPrint(false);
    }

    private TbmContentSanitizer() {
    }

    /**
     * 교육내용 리치 HTML 정화. null/blank 는 그대로 반환한다(불필요한 정화 호출 방지).
     *
     * @param body 원본 CONTENT_BODY
     * @return 정화된 HTML
     */
    public static String sanitize(String body) {
        if (!StringUtils.hasText(body)) {
            return body;
        }
        return Jsoup.clean(body, "", safelist(), compactOutput());
    }

    /**
     * 교육내용에서 사람이 읽는 글자만 뽑는다(빈 입력 판정·길이 검증용).
     *
     * <p>편집기는 내용을 지워도 {@code <p><br></p>} 같은 빈 껍데기를 남긴다. 문자열 길이로
     * 판정하면 이 껍데기(11자)가 최소 글자수를 통과시켜 빈 교육내용으로 개설이 된다.
     * 태그·엔티티를 걷어낸 실제 글자로 판정해야 한다. 순수 텍스트를 넣으면 그대로 돌려준다.
     *
     * @param body 원본 CONTENT_BODY(HTML 또는 순수 텍스트)
     * @return 태그를 제거한 텍스트. null/blank 는 빈 문자열.
     */
    public static String toText(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        return Jsoup.parse(body).text();
    }
}
