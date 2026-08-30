package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 반기 세션 1행 (시트1 "반기 교육실시 목록" + 건별 선택 리스트 공용).
 *
 * <p>공유 세션(sharedYn='Y')은 타사 개설 세션 중 자사 근로자 참석분이며,
 * 참여/이수 카운트는 자사 참석자 기준이다(타사 인원수 비노출 — Tbm02 원칙).
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceSessionResult(
    String sessionCd
    , String siteNm            // 개설 사업장명
    , String hostCmpnyNm       // 개설사명(자사 세션이면 null)
    , String title
    , String managerUserNm     // 주관자(개설자) 이름
    , String startedAt         // yyyy-MM-dd HH:mm (KST)
    , String endedAt
    , Integer eduMinutes       // 세션 인정시간(분)
    , Integer riskCount
    , Integer attendanceCount  // 참여(입실) 인원 — 공유 세션은 자사 기준
    , Integer completedCount   // 이수 인원 — 공유 세션은 자사 기준
    , String sharedYn          // 'Y'=타사 개설(연동 지정) / 'N'=자사 개설
){
}
