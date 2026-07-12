package com.prafta.web.dashboard.dashboard01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 근태 탭 A1 근무계획 등록율 응답 (PRAFTA-DASHBOARD-T2).
 * deptList 는 등록율 오름차순(저조 부서 상단 노출) — 서비스에서 정렬.
 */
@Getter
@Builder
public class DashAttdPlanRegRateResponse {
    private int totalUserCnt;         // 전체 대상 사용자 수 (Attd_05 selectUserList 술어 미러)
    private int regUserCnt;           // 전체 등록 사용자 수
    private double regRate;           // 전체 등록율 % (소수 1자리, 분모 0 이면 0.0)
    private List<DeptRate> deptList;  // 부서별 등록율 (regRate ASC, nodeNm ASC)

    /** 부서별 등록율 행 (FE 진행바 리스트 1행) */
    public record DeptRate(
        String nodeCd
        , String nodeNm
        , int totalUserCnt
        , int regUserCnt
        , double regRate
    ){
    }
}
