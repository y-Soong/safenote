package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;

public record InsertUserAttdHistsCommand(
	String histId
	, String attdId
    , String siteCd
    , String histType
    , String processReason
    , String workYmd

    , String befCheckInDate
    , String befCheckInTime
    , String befCheckOutDate
    , String befCheckOutTime

    , String aftCheckInDate
    , String aftCheckInTime
    , String aftCheckOutDate
    , String aftCheckOutTime

    , String gvCmpnyCd
    , String gvUserCd
) {

    /** HIST_TYPE[SYS032] 01: 근태 생성 */
    public static final String HIST_TYPE_CREATE = "01";

    /** HIST_TYPE[SYS032] 07: 관리자 반려 (PRAFTA-008 / PRAFTA-009 마이그레이션으로 추가된 코드) */
    public static final String HIST_TYPE_REJECT = "07";

    public static InsertUserAttdHistsCommand from(String histId, String attdId, UpdateUserAttdInfosModel model) {

    	if (histId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
    	
    	if (attdId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
    	
        if (model == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertUserAttdHistsCommand(
    		histId
        	, attdId
            , model.siteCd()
            , HIST_TYPE_CREATE		// histType[SYS032] 01:생성
            , model.reason()
            , model.workYmd()

            , model.oriCheckInDate()
            , model.oriCheckInTime()
            , model.oriCheckOutDate()
            , model.oriCheckOutTime()

            , model.checkInDate()
            , model.checkInTime()
            , model.checkOutDate()
            , model.checkOutTime()

            , model.gvCmpnyCd()
            , model.gvUserCd()
        );
    }

    /**
     * PRAFTA-008 - 근태 요청 반려 이력 INSERT용 팩토리.
     *
     * 반려는 근태를 실제 반영하지 않으므로 BEF_* / AFT_* 출퇴근 값은 전부 NULL이며,
     * HIST_TYPE 은 '07'(관리자 반려), PROCESS_REASON 에는 반려사유를 기록한다.
     *
     * @param histId        채번된 HIST_ID
     * @param attdId        이력을 귀속시킬 ATTD_ID (수정요청: REQ.TARGET_ID, 생성요청: 신규 채번값)
     * @param cmpnyCd       회사 코드
     * @param siteCd        사업장 코드
     * @param workYmd       근무일자
     * @param rejectReason  반려사유
     * @param processUserCd 처리자(반려자) 사용자 코드
     */
    public static InsertUserAttdHistsCommand forReject(
            String histId, String attdId, String cmpnyCd, String siteCd,
            String workYmd, String rejectReason, String processUserCd) {

        if (histId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (attdId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertUserAttdHistsCommand(
            histId
            , attdId
            , siteCd
            , HIST_TYPE_REJECT		// histType[SYS032] 07:관리자 반려
            , rejectReason
            , workYmd

            , null					// BEF_* 전부 NULL (반려는 미반영)
            , null
            , null
            , null

            , null					// AFT_* 전부 NULL (반려는 미반영)
            , null
            , null
            , null

            , cmpnyCd
            , processUserCd
        );
    }
}
