package com.prafta.platform.sms.application.query;

import com.prafta.platform.sms.application.param.SmsHistoryListParam;

/**
 * Platform_05: SMS 발송 이력 목록 조회 쿼리 (매퍼 입력).
 *
 * <p>발송기간은 <b>여기서 datetime 문자열로 정규화</b>한다(시작 00:00:00 / 종료 23:59:59).
 * 매퍼에서 {@code DATE(INSERT_DATE)} 같은 함수 술어를 쓰면 인덱스를 못 타기 때문이다.
 *
 * <p>★휴대폰은 <b>HMAC 만</b> 실린다. 평문/암호문은 매퍼로 내려가지 않는다.
 *    HMAC 은 {@code MBL_NO_HMAC = #{mblNoHmac}} 정확 일치 술어로만 쓰이며,
 *    <b>응답 DTO 로는 절대 옮기지 않는다</b>(상관·역추적 재료).
 *
 * <p>★{@code CMPNY_CD} 축이 없다 — {@code TB_SMS_AUTH_CODE} 에 회사 컬럼 자체가 없기 때문이다.
 *    따라서 이 쿼리는 <b>플랫폼 운영자 게이트({@code /platformApi}) 뒤에서만</b> 성립한다.
 */
public record SmsHistoryListQuery(
        String startDtime
        , String endDtime
        , String purposeCd
        , String sendStatus
        , String mblNoHmac
        , int offset
        , int pageSize
) {
    /**
     * @param param     검증 완료 파라미터
     * @param mblNoHmac 서비스가 {@code HmacSigner} 로 만든 검색 대상 HMAC. 검색 미사용이면 null.
     */
    public static SmsHistoryListQuery from(SmsHistoryListParam param, String mblNoHmac) {

        return new SmsHistoryListQuery(
                param.startDate() + " 00:00:00"
                , param.endDate() + " 23:59:59"
                , param.purposeCd()
                , param.sendStatus()
                , mblNoHmac
                , (param.page() - 1) * param.pageSize()
                , param.pageSize()
        );
    }
}
