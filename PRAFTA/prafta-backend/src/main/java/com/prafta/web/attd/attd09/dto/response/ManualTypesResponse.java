package com.prafta.web.attd.attd09.dto.response;

import java.util.List;

import com.prafta.common.cmm.leave.vo.LeaveTypeOptionVO;

import lombok.Builder;
import lombok.Value;

/**
 * 수동 부여 가능 휴가 종류 응답.
 * GET /attd09/leave-grant/manual-types.
 */
@Value
@Builder
public class ManualTypesResponse {

    /** 휴가 종류 옵션 목록 */
    List<LeaveTypeOptionVO> types;
}
