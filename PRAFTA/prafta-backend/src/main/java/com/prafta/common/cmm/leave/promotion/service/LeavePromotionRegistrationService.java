package com.prafta.common.cmm.leave.promotion.service;

import com.prafta.common.cmm.leave.promotion.vo.PromotionRegisterResult;

/**
 * 촉진 연차 1일 등록 공용 헬퍼 (PRAFTA-COM-008-A-3/A-4).
 *
 * <p>1차(앱 자발)·2차(웹 회사직권)가 공유하는 "촉진 연차 1일 등록" 단일 로직이다. 단계(stage)·
 * 지정주체(designator)만 다르고 나머지(work_plan 폴백 + 교대 가드 + 마감월 가드 + 본연차 grant 차감
 * + leave_use INSERT + USED_DAYS 동기화 + DIRECT_USE_KEY 멱등)는 동일하다.
 *
 * <ul>
 *   <li>등록 연차 = LEAVE_CD='SYS_ANNUAL', USE_UNIT_TYPE='00'(종일), LEAVE_DAYS=1.0, CONFIRMED,
 *       GRANT_ID=차감 본연차(STATUTORY_ANNUAL ACTIVE) grant, START_DATE=END_DATE=대상일.</li>
 *   <li>ORIG_DESIGNATED_DATE = 최초 등록 시 START_DATE 와 동일(이동해도 보존).</li>
 *   <li>work_plan 행이 없으면 {@code DefaultSchGenService.ensureWorkPlanDay}(교대 비소속). 교대팀
 *       소속일이면 기존 근무일(work_plan 존재)에만 허용, 없으면 거부(NOT_SCHEDULED).</li>
 *   <li>마감월(isClosedForUser)이면 거부(CLOSED).</li>
 *   <li>차감 가능 본연차 grant 가 없으면 거부(INSUFFICIENT). 이미 등록(멱등키 충돌)이면 SKIPPED_DUP.</li>
 * </ul>
 *
 * <p>호출부(앱/웹 서비스)는 자신의 @Transactional 안에서 본 메서드를 호출하며, stage/designator 만
 * 다르게 넘긴다. 마스터(STAGE1/STAGE2) 갱신은 호출부가 등록 결과를 보고 일괄 수행한다.
 */
public interface LeavePromotionRegistrationService {

    /** 촉진 등록 연차 코드(법정 본연차 소비). */
    String SYS_ANNUAL = "SYS_ANNUAL";

    /** 사용 단위 [SYS025] 종일. */
    String UNIT_FULL = "00";

    /**
     * 촉진 연차 1일을 등록한다(work_plan 폴백 + 교대/마감 가드 + grant 차감 + leave_use INSERT + 동기화).
     *
     * @param cmpnyCd        회사 코드
     * @param siteCd         사업장 코드(사용자 소속)
     * @param userCd         대상 사용자 코드
     * @param workYmd        대상일 (YYYYMMDD)
     * @param promotionStage 촉진 단계 [SYS068] FIRST / SECOND
     * @param designatorType 지정 주체 [SYS069] VOLUNTARY / COMPANY
     * @param reason         사용 사유(로그/감사용 한국어)
     * @param operatorUserCd 등록 수행자(앱=본인, 웹=관리자)
     * @return 등록 결과(성공/멱등/거부 사유)
     */
    PromotionRegisterResult register(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                     String promotionStage, String designatorType,
                                     String reason, String operatorUserCd);
}
