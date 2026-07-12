package com.prafta.web.user.user05.result;

/**
 * 일일사용자 관리(조회) 1행 결과 = 슬롯 점유 이력 1건(tb_daily_user_slot_his 기준).
 *
 * <p>관리자 운영 화면 요구로 이름(userNm)/휴대폰(mblNo)을 평문으로 응답한다(마스킹 해제).
 * 이름은 TB_DAILY_USER.USER_NM 평문, 휴대폰은 서비스에서 MBL_NO_ENC 를 복호화/포맷하여 채운다.
 * nodeNm 은 TB_USER.NODE_CD 의 현재값 기준 부서명(점유 시점 스냅샷 아님, D4).
 * his.USER_ID 가 tb_daily_user 에 없는 행도 LEFT JOIN 으로 노출(이름/전화 '-', Q1).
 */
public record DailyUserListResult(
    String hisId
    , String userNm
    , String mblNo
    , String siteNm
    , String nodeNm
    , String slotNo
    , String occupyDtime
    , String releaseDtime
){
}
