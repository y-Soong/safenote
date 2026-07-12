package com.prafta.web.tbm.tbmai01.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * TBM AI 분석 워크리스트 조회 요청(GET /webApi/tbmai01/analysis-worklist).
 *
 * <p>GET @ModelAttribute 바인딩. 식별자(cmpnyCd/userCd/authCd/siteCd)는 바디에 두지 않고
 *    JWT 클레임에서만 도출한다(IDOR 차단). 아래 필드는 모두 선택 필터/페이징이다.
 * <ul>
 *   <li>{@code keyword} — 자료명(TITLE)/항목설명(MTRL_DESC) LIKE(선택).</li>
 *   <li>{@code fileNm} — 첨부파일명(TB_FILE_INFO.FILE_NM) LIKE(선택).</li>
 *   <li>{@code mtrlItemType} — 항목 타입(01~04, 선택).</li>
 *   <li>{@code aiStatus} — AI 상태[SYS056](선택). 단 {@code PENDING_ANALYZE}(01·04 분석대기),
 *       {@code PENDING_CONFIRM}(02·03 관리자 확정 대기)은 SYS056 미등록 의사코드로,
 *       매퍼가 {@code AI_STATUS='NONE'} + 항목타입 조합으로 해석한다.</li>
 *   <li>{@code siteCd} — 특정 사업장 / 'COMMON'=회사공통(선택).</li>
 *   <li>{@code page} — 1-base 페이지(기본 1).</li>
 *   <li>{@code size} — 페이지 크기(기본 20, 상한 100 클램프).</li>
 * </ul>
 */
@Getter
@Setter
public class TbmAiWorklistRequest {
    private String keyword;
    private String fileNm;
    private String mtrlItemType;
    private String aiStatus;
    private String siteCd;
    private Integer page;
    private Integer size;
}
