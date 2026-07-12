package com.prafta.web.dashboard.dashboard01.dto.response;

import java.util.List;

import com.prafta.web.dashboard.dashboard01.result.DashRecentAcctResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 안전 탭 무사고 배너(S1) + 사고 summary(S5) 응답 (PRAFTA-DASHBOARD-T4).
 * PII 미포함: 재해자 관련 컬럼/사고 경위는 응답에 담지 않는다.
 */
@Getter
@Builder
public class DashSafetyAcctResponse {
    private Integer noAcctDays;   // 무사고 경과일 (기산 불가 시 null → FE '-')
    private String baselineYmd;   // 기산일 YYYYMMDD (기산 불가 시 null)
    private String baselineType;  // "ACCT"(최근 사고일) | "SITE_STR"(사업개시일) | "SITE_INSERT"(사업장 등록일) | null
    private int monthTotalCnt;    // 조회월 사고 총 건수
    private int grade100Cnt;      // 조회월 중대재해(SYS065=100) 건수
    private int grade200Cnt;      // 조회월 일반산재(200) 건수
    private int grade300Cnt;      // 조회월 신고제외(300) 건수
    private List<DashRecentAcctResult> recentAcctList; // 전체 기간 최근 3건
}
