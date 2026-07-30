package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 사용촉진 1차 현황 행 응답 View(raw + Java 파생 결과).
 *
 * <p>화면(LeavePromotion_01_1.vue)은 상태/기한/독촉가능 판정을 <b>재계산하지 않고</b> 본 View 의
 * 서버 산출값만 표기한다(프론트-서버 판정 분기 방지).
 *
 * <p>PII 최소화(공통 §11.3): 이름·부서·사업장까지만 싣는다. 입사일·연락처·이메일·생년월일 금지.
 * 지정 일수(STAGE1_DESIGNATED_DAYS)·지정 날짜도 싣지 않는다(확정 D1).
 */
@Getter
@Builder
public class PromotionFirstTargetView {

    /** 대상 근로자 코드(독촉 대상 식별자). */
    private String userCd;

    /** 근로자명(평문). */
    private String userNm;

    /** 소속 부서 코드. */
    private String nodeCd;

    /** 소속 부서명. */
    private String nodeNm;

    /** 사업장 코드. */
    private String siteCd;

    /** 사업장명. */
    private String siteNm;

    /** 1차 통지일 (YYYYMMDD). */
    private String noticedDate;

    /** 계획 제출 기한 (YYYYMMDD) = 통지일 + 10일(확정 D2, 근로기준법 제61조). */
    private String deadlineDate;

    /** 회차 기준 본연차 만료일 (YYYYMMDD). */
    private String baseAvailToDate;

    /** 2차 도래 예정일 (YYYYMMDD) = 만료일 - 3개월. */
    private String stage2DueDate;

    /** 제출 여부 Y/N (지정 일수 &gt; 0). 일수 자체는 노출하지 않는다(D1). */
    private String submittedYn;

    /** 해당 회차 촉진 연차 최초 등록일 (YYYYMMDD). 이전 회차분/미제출이면 null. */
    private String firstSubmitDate;

    /** 지연 통지 여부 Y/N (통지일이 만료-6개월+9일 초과 = 구 10일 창 종료일 초과). 회사 귀책 표식. */
    private String lateNoticeYn;

    /** 앱 로그인 안내 1회 노출 완료 여부 Y/N. */
    private String loginNotifiedYn;

    /** 해당 회차 독촉 발송 횟수. */
    private int remindCnt;

    /** 해당 회차 최종 독촉일 (YYYYMMDD). 없으면 null. */
    private String lastRemindDate;

    /** 독촉 가능 여부 Y/N (미제출자만 가능). 화면은 이 값만 보고 버튼을 활성한다. */
    private String remindableYn;

    /** 제출 기한까지 남은 일수(음수 = 기한 경과). 프론트 날짜 계산 제거용. */
    private long dDay;

    /** 상태 코드: NOT_SUBMITTED / OVERDUE_NOT_SUBMITTED / SUBMITTED / LATE_SUBMITTED. */
    private String statusCd;

    /** 상태 라벨(한국어). 화면은 이 값을 그대로 표기한다. */
    private String statusNm;
}
