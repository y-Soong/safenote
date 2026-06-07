package com.prafta.app.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-tbm-A8: 시작전 퇴실(출결 취소) Command.
 *
 * <p>물리 DELETE 채택 사유: UK_TBM_ATTENDANCE_01(CMPNY/SESSION/USER_TYPE/USER)에 DEL_YN 미포함 →
 *   소프트삭제(DEL_YN='Y') 시 재입실 INSERT 가 UK 충돌로 막힌다. 시작전 퇴실은 출결 자체를
 *   취소(참석 인원 제거)하고 재입실을 멱등 허용해야 하므로 물리 삭제가 적합.
 * <p>WHERE: 본인(REGULAR+USER_CD) + 미종료(EXIT_AT IS NULL) + DEL_YN='N' 가드(IDOR/오삭제 차단).
 */
public record TbmLeaveBeforeCommand(
    String cmpnyCd
    , String sessionCd
    , String userCd
    , String userTypeCd
) {
    private static final String USER_TYPE_REGULAR = "REGULAR";

    public static TbmLeaveBeforeCommand of(String cmpnyCd, String sessionCd, String userCd) {

        if (cmpnyCd == null || sessionCd == null || userCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmLeaveBeforeCommand(cmpnyCd, sessionCd, userCd, USER_TYPE_REGULAR);
    }
}
