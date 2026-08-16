package com.prafta.app.approval.admin.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 001-P2-B2: 승인 대기 리스트(A-1) 응답. plan §3-B 계약.
 */
@Getter
@Builder
public class ApprovalPendingResponse {

    private final List<PendingItem> items;
    /** 그룹별 대기 건수: ALL/CORRECTION/OVERTIME/LEAVE. */
    private final Map<String, Integer> counts;
    private final int totalCount;
    private final boolean hasMore;

    @Getter
    @Builder
    public static class PendingItem {
        private final String reqId;
        private final String group;
        private final String reqType;
        private final String reqTypeNm;
        private final String requesterUserNm;
        private final String requesterUserCd;
        private final String nodeNm;
        private final String targetYmd;
        private final List<String> summaryLines;
        private final String reqDate;
        /** A1(마감 기준일 소스) 미확정 — v1 보류(null). */
        private final Integer deadlineDday;
        private final String deadlineLevel;
        private final String selfYn;
        /** 선점 잠금 미구현(A-4 제외) — 항상 false. */
        private final boolean lockedYn;
        private final String lockedByNm;
        /** 연차만 결재 단계(근태보정/초과는 null). */
        private final Integer approvalStep;
        /**
         * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-02, additive).
         * 연차(LEAVE)만 세팅(0 이상) — CorrOt/스케줄 경로는 null 직렬화(구버전 앱 무영향).
         */
        private final BigDecimal borrowDays;

        // ── prafta-leavemulti: 연차 기간(From-To) 신청 묶음 접기(additive, 전부 nullable) ──────────
        // groupLeave=Y 로 조회했을 때만 세팅된다. 단일일 신청·근태보정/초과/스케줄은 전부 null 이라
        // 구버전 앱 번들은 이 필드들을 무시하고 종전과 동일하게 동작한다.

        /** 묶음 키(TB_USER_ATTD_REQ.LEAVE_GROUP_ID). null 이면 단건 카드. */
        private final String leaveGroupId;
        /** 묶음에 포함된 대기 건수(= 이 결재자가 처리해야 할 날짜 수). */
        private final Integer groupCount;
        /** 묶음 시작일(YYYYMMDD, 대기 건 기준 최소 WORK_YMD). */
        private final String groupFromYmd;
        /** 묶음 종료일(YYYYMMDD, 대기 건 기준 최대 WORK_YMD). */
        private final String groupToYmd;
        /**
         * 묶음 총 일수(LEAVE_DAYS 합).
         * ★LEAVE_MINUTES 는 분할차감 시 첫 행에만 총량을 싣는 불변식이라 합산하지 않는다.
         */
        private final BigDecimal groupDays;
        /** 묶음 내 개별 건(날짜 오름차순). 개별 처리·일괄 전송 items 산출에 사용. */
        private final List<GroupItem> groupItems;

        /** prafta-leavemulti: 묶음 내 개별 연차 요청 1건(날짜별). */
        @Getter
        @Builder
        public static class GroupItem {
            private final String reqId;
            private final Integer approvalStep;
            /** 대상 근무일(YYYYMMDD). */
            private final String targetYmd;
            private final BigDecimal leaveDays;
            /** 사용단위명(종일/반차 등, SYS025). */
            private final String unitNm;
            private final String selfYn;
            private final BigDecimal borrowDays;
            /** 카드 요약 문구(단건 카드와 동일 생성기). */
            private final List<String> summaryLines;
        }
    }
}
