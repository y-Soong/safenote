package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 수동 부여 가능 휴가 종류 옵션(attd09 수동 부여 모달).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1
 *
 * <p>필터: {@code tb_leave_type_mgmt} WHERE CMPNY_CD AND LEAVE_TYPE='02'(관리자 부여)
 * AND GRANT_TYPE='02'(수동) AND USE_YN='Y'.
 */
@Getter
@Setter
public class LeaveTypeOptionVO {

    /** 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) */
    private String leaveCd;

    /** 연차명 */
    private String leaveNm;
}
