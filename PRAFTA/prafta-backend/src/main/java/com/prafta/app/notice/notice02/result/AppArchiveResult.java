package com.prafta.app.notice.notice02.result;

/**
 * 앱 자료실 단건/목록 결과 VO (tb_notice 기준, NOTICE_TYPE='ARCHIVE').
 *
 * <p>웹 ArchiveResult 와 동형(앱 전용 분리). 팝업/고정/대상/확인이력 개념 없음.
 * archiveTypeNm 은 tb_baim_val_d LEFT JOIN 표시명(USE_YN 무관, 저장 타입명 보존).
 * fileCnt 는 첨부 개수(목록 아이콘 표시용).
 *
 * <p>⚠️ MyBatis record 매핑(feedback_mybatis_record_column_order): SELECT 컬럼 순서 =
 * 생성자 인자 순서(위치 기반). 보안상 EDIT_PWD(해시)는 결과 VO 에 절대 포함하지 않는다.
 */
public record AppArchiveResult(
    String cmpnyCd
    , String noticeId
    , String archiveTypeCd
    , String archiveTypeNm
    , String title
    , String content
    , String insertNo
    , String insertDate
    , String insertUserNm
    , String updateDate
    , Integer fileCnt
){
}
