package com.prafta.app.admin.employeestatus.service;

import com.prafta.app.admin.employeestatus.application.param.EmployeeGpsTrailParam;
import com.prafta.app.admin.employeestatus.application.param.EmployeeStatusDailyParam;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeGpsTrailResponse;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeStatusDailyResponse;

/**
 * PRAFTA-002/003: 앱 관리자 "직원관리"(실시간 근태 현황 + 외근 위치) 서비스.
 */
public interface AppAdminEmployeeStatusService {

    /** PRAFTA-002: 일자 직원 현황(로스터 기반 근무중/미출근/휴무/퇴근 + 연차/외근 배지). */
    EmployeeStatusDailyResponse selectDaily(EmployeeStatusDailyParam param);

    /** PRAFTA-003: 외근 GPS 궤적(복호화 좌표) 단건 조회. */
    EmployeeGpsTrailResponse selectGpsTrail(EmployeeGpsTrailParam param);
}
