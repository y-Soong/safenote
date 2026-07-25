package com.prafta.web.attd.attd15.service;

import com.prafta.web.attd.attd15.application.param.Weekly52hListsParam;
import com.prafta.web.attd.attd15.dto.response.Weekly52hListsResponse;

/**
 * ATTD15-T1 - 주52시간 관리 서비스.
 *
 * <p>고객사(원청) 관리자가 소속 직원들의 주간 근무시간(등록된 스케줄 기준 / 실제 근무 기준)을
 * 조회해 주 52시간(법정근로 40h + 연장근로 12h) 초과 위험 직원을 사전 식별하는 조회 전용 API.
 */
public interface Attd15Service {

    /** 사업장/소속부서(+하위)/사용자명 필터 + 대상 주(월~일) 기준 주간 근로시간 목록 조회. */
    Weekly52hListsResponse getWeekly52hLists(Weekly52hListsParam param);
}
