package com.prafta.web.attd.attd03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd03.application.command.LeaveTypeCommand;
import com.prafta.web.attd.attd03.application.query.LeaveNoDupCheckQuery;
import com.prafta.web.attd.attd03.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd03.result.LeaveTypeResult;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;

@Mapper
public interface Attd03Mapper {
	
	void updateLeaveType(LeaveTypeCommand command);

	String selectLeaveCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);

	List<LeaveTypeResult> selectLeaves(LeaveTypeListQuery query);

	LeaveNoDupChk selectLeaveNoDupChkResult(LeaveNoDupCheckQuery query);

	/**
	 * 연차 타입의 SYSTEM_YN 값을 조회한다 (PRAFTA-018 §8.5.5 시스템 시드 가드용).
	 *
	 * <p>대상 행이 없으면 {@code null} 을 반환한다. 'Y' 이면 시스템 시드로 편집/삭제 차단,
	 * 'N' 또는 NULL 이면 사용자 정의 행이므로 기존 로직 진행.
	 */
	String selectSystemYn(@Param(value = "gvCmpnyCd") String gvCmpnyCd,
	                      @Param(value = "leaveCd") String leaveCd);
}
