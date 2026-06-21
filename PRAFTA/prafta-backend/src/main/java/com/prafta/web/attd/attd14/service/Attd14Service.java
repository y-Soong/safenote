package com.prafta.web.attd.attd14.service;

import com.prafta.web.attd.attd14.application.param.AdminRequestHistoryListParam;
import com.prafta.web.attd.attd14.dto.response.AdminRequestHistoryListResponse;
import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;

/**
 * 관리자 발신 연차 변경 요청 이력(attd14) 조회 서비스 (prafta-com-016-H, 읽기 전용).
 *
 * <p>출처 = TB_LEAVE_CHANGE_REQUEST(INITIATOR_TYPE='ADMIN'). 역할 스코프/IDOR 는 attd13 정책 계승(safe 제외).
 */
public interface Attd14Service {

    /** 관리자 발신 요청 이력 목록 + 총 건수(페이징). */
    AdminRequestHistoryListResponse getAdminRequestHistory(AdminRequestHistoryListParam param);

    /** 관리자 발신 요청 이력 단건 상세(읽기 전용). 스코프 밖이면 404. */
    AdminRequestHistoryRowResult getAdminRequestHistoryDetail(String cmpnyCd, String authCd, String userCd, String changeReqId);
}
