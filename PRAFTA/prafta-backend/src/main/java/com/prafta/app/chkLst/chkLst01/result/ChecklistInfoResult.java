package com.prafta.app.chkLst.chkLst01.result;

/**
 * prafta-036-B1: 체크리스트 정보 조회 결과 (SELECT 결과셋 매핑 record).
 *
 * <p>매핑 대상: AppChkLst01Mapper.selectChkLstInfo SELECT 컬럼 별칭과 1:1 일치.
 * <pre>
 *   1. A.CMPNY_CD              AS cmpnyCd
 *   2. A.CHKLST_TYPE           AS chklstType
 *   3. A.CHKPT_DESC            AS chkptDesc
 *   4. A.CHKPT_NM              AS chkptNm    (prafta-app-011 추가)
 *   5. S.SITE_NM               AS siteNm     (prafta-app-011 추가, TB_SITE 조인)
 *   6. B.INSPECT_ITEM_CD       AS inspectItemCd
 *   7. B.INSPECT_ITEM_SUBJ     AS inspectItemSubj
 *   8. 'Y'                     AS inspectValue
 *   9. B.SORT_IDX              AS sortIdx
 * </pre>
 * SORT_IDX 는 DB 상 INT 이나 기존 vo 호환 및 FE 무변경을 위해 String 으로 매핑한다(MyBatis 자동 변환).
 */
public record ChecklistInfoResult(
    String cmpnyCd
    , String chklstType
    , String chkptDesc
    , String chkptNm
    , String siteNm
    , String inspectItemCd
    , String inspectItemSubj
    , String inspectValue
    , String sortIdx
    // PRAFTA-SUBCON-T6-07 / [정책변경 후행덮어쓰기]: 오늘 일자 기준 선행 점검 상태(앱 덮어쓰기 확인 팝업용).
    //   record 위치 매핑이라 신규 필드는 SELECT 말미와 같은 순서로 말미에 추가한다.
    , String alreadyAnswered    // 'Y' = 오늘 이미 점검됨(선행 데이터 존재), 'N' = 미수행
    , String performUserNm      // 선행 점검자 성명(저장 시점 스냅샷 — 확인 팝업 "성명(ID)" 표시)
    , String performUserCd      // 선행 점검자 USER_CD(확인 팝업 "성명(ID)" 표시, 조인 금지)
) {

}
