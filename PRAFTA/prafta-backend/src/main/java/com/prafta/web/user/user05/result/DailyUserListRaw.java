package com.prafta.web.user.user05.result;

/**
 * 일일사용자 관리(조회) 매퍼 원시 결과 1행.
 *
 * <p>이름(userNm)은 TB_DAILY_USER.USER_NM 평문, 휴대폰(mblNoEnc)은 MBL_NO_ENC AES-GCM 암호문이다.
 * SQL 로는 복호화가 불가하므로 서비스단에서 휴대폰을 복호화/포맷하여 평문 응답
 * ({@link DailyUserListResult})으로 변환한다. 암호문(mblNoEnc)은 프론트로 노출하지 않는다.
 */
public record DailyUserListRaw(
    String hisId
    , String userNm
    , String mblNoEnc
    , String siteNm
    , String nodeNm
    , String slotNo
    , String occupyDtime
    , String releaseDtime
){
}
