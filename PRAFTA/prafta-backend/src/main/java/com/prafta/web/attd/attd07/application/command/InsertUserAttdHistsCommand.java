package com.prafta.web.attd.attd07.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.UpdateUserAttdInfosModel;

/**
 * TB_USER_ATTD_HIST INSERT 용 커맨드.
 *
 * [PRAFTA-010-1-022] attdId 의미:
 *   attdId 는 "이 이력이 귀속될 ATTD_ID(= 요청이 귀결될 TB_USER_ATTD_MGMT.ATTD_ID)" 이다.
 *     - 수정요청(REQ_TYPE='02', 근태수정): 기존 근태의 ATTD_ID.
 *     - 생성요청(REQ_TYPE='01', 근태생성): 사용자측 요청 시점에 사전 채번된 ATTD_ID
 *       (REQ.TARGET_ID 에 저장된 값. prafta-010.md §2.1.1 참조).
 *   생성요청의 1차(요청 시점) 이력은 본 작업 범위에서 적재하지 않으며, 관리자 승인/반려
 *   시점의 2차 이력만 본 커맨드로 적재한다(승인 HIST_TYPE='01', 반려 HIST_TYPE='07').
 */
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

    /** HIST_TYPE[SYS032] 08: 초과근무 승인 (PRAFTA-027) */
    public static final String HIST_TYPE_OVERTIME_APPROVE = "08";

    /** HIST_TYPE[SYS032] 09: 초과근무 반려 (PRAFTA-027) */
    public static final String HIST_TYPE_OVERTIME_REJECT = "09";

    /**
     * 근태 승인(2차 이력) INSERT 용 팩토리. HIST_TYPE='01'.
     *
     * [PRAFTA-010-1-022] attdId 는 요청이 귀결될 ATTD_ID 이다(생성요청=사전 채번 ATTD_ID).
     * 생성요청 승인 시 model 의 oriCheckIn / oriCheckOut(BEF_ 계열) 값은 NULL 이며,
     * 그 NULL 을 그대로 BEF_ 계열 컬럼에 적재한다(승인 전 상태 없음 = 정상 동작).
     */
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

    /**
     * PRAFTA-027 - 초과근무 승인 이력 INSERT용 팩토리. HIST_TYPE='08'.
     *
     * 초과근무는 그날 출퇴근 기록이 있어야만 등록되므로, 그날 근태기록의 ATTD_ID 를
     * HIST 앵커로 사용한다(TB_USER_ATTD_HIST.ATTD_ID 는 NOT NULL).
     * AFT_* 출퇴근 컬럼에는 등록된 OT 구간(시작/종료)을 담아 일자 상세 "처리 이력"에
     * 노출한다. 생성 승인이므로 BEF_* 는 NULL 이다.
     *
     * @param histId        채번된 HIST_ID
     * @param attdId        이력을 귀속시킬 ATTD_ID (그날 근태기록)
     * @param cmpnyCd       회사 코드
     * @param siteCd        사업장 코드
     * @param workYmd       근무일자
     * @param otStartDate   OT 시작 일자 (AFT_CHECK_IN_DATE 에 저장)
     * @param otStartTime   OT 시작 시각 (AFT_CHECK_IN_TIME 에 저장)
     * @param otEndDate     OT 종료 일자 (AFT_CHECK_OUT_DATE 에 저장)
     * @param otEndTime     OT 종료 시각 (AFT_CHECK_OUT_TIME 에 저장)
     * @param reason        처리 사유 (요청 사유)
     * @param processUserCd 처리자(승인자) 사용자 코드
     */
    public static InsertUserAttdHistsCommand forOvertimeApprove(
            String histId, String attdId, String cmpnyCd, String siteCd, String workYmd,
            String otStartDate, String otStartTime, String otEndDate, String otEndTime,
            String reason, String processUserCd) {

        if (histId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (attdId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertUserAttdHistsCommand(
            histId
            , attdId
            , siteCd
            , HIST_TYPE_OVERTIME_APPROVE	// histType[SYS032] 08:초과근무 승인
            , reason
            , workYmd

            , null					// BEF_* 전부 NULL (생성 승인: 이전 상태 없음)
            , null
            , null
            , null

            , otStartDate			// AFT_* = 등록된 OT 시작/종료 구간
            , otStartTime
            , otEndDate
            , otEndTime

            , cmpnyCd
            , processUserCd
        );
    }

    /**
     * PRAFTA-027 - 초과근무 반려 이력 INSERT용 팩토리. HIST_TYPE='09'.
     *
     * 근태 요청 반려(forReject)와 동일하게 BEF_* / AFT_* 출퇴근 값은 전부 NULL 이며,
     * PROCESS_REASON 에 반려사유를 기록한다. ATTD_ID 앵커는 그날 근태기록을 사용한다.
     *
     * @param histId        채번된 HIST_ID
     * @param attdId        이력을 귀속시킬 ATTD_ID (그날 근태기록)
     * @param cmpnyCd       회사 코드
     * @param siteCd        사업장 코드
     * @param workYmd       근무일자
     * @param rejectReason  반려사유
     * @param processUserCd 처리자(반려자) 사용자 코드
     */
    public static InsertUserAttdHistsCommand forOvertimeReject(
            String histId, String attdId, String cmpnyCd, String siteCd, String workYmd,
            String rejectReason, String processUserCd) {

        if (histId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        if (attdId == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertUserAttdHistsCommand(
            histId
            , attdId
            , siteCd
            , HIST_TYPE_OVERTIME_REJECT		// histType[SYS032] 09:초과근무 반려
            , rejectReason
            , workYmd

            , null					// BEF_* 전부 NULL (반려는 미반영)
            , null
            , null
            , null

            , null					// AFT_* 전부 NULL (근태 반려와 동일)
            , null
            , null
            , null

            , cmpnyCd
            , processUserCd
        );
    }
}
