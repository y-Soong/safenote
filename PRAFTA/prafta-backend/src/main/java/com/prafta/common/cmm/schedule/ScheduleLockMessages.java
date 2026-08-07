package com.prafta.common.cmm.schedule;

import java.util.List;

import com.prafta.common.cmm.schedule.vo.ScheduleLockVO;

/**
 * 스케줄 변경 잠금(prafta-com-016 / E3 당일분모 전환) <b>대표 사유 판정 + 사용자 문구</b> 공용 헬퍼.
 *
 * <p>{@code ScheduleChangeGuardService.findLockedDays} 가 돌려주는 잠금 목록에서 대표 사유 1건을 뽑고,
 *   그 사유에 맞는 안내 문구를 만든다. 한 날에 여러 잠금이 공존할 수 있으므로 <b>조치 부담이 큰 순서</b>
 *   (OT &gt; 확정 연차 &gt; 미결 시간차)로 대표 1건만 안내한다(기존 Attd05 셀 스킵 사유 로직 승계).
 *
 * <p><b>신설 배경(F-7, 2026-08-06)</b> — {@code ATTD_400_164} 가 "시간 단위 연차"만 지목하는데
 *   실제 잠금 범위는 "확정 연차 전 단위(종일·반차·시간차) + 미결 시각 보유 연차(반차·시간차)" 다.
 *   확정 반차로 막힌 관리자가 존재하지 않는 시간차를 찾는 오안내가 운영에서 실측되었다(2026-08-05).
 *   같은 코드베이스에서 Attd_05 는 정확한 문구를, Attd_07/Req_07 은 뭉뚱그린 문구를 주던 <b>경로 간
 *   품질 불일치</b>를 구조적으로 제거하기 위해 판정 로직을 본 헬퍼로 모은다.
 */
public final class ScheduleLockMessages {

    private ScheduleLockMessages() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 대표 잠금 사유 구분. */
    public enum LockKind {
        /** 초과근무(등록 또는 신청) 보유. */
        OVERTIME,
        /** 확정 연차 보유(단위 무관 — 종일·반차·시간차). */
        CONFIRMED_LEAVE,
        /**
         * 미결 <b>시각 보유</b> 연차 신청만 존재(반차 + 시간차).
         *
         * <p>★ 상수명은 "HOURLY" 지만 <b>반차도 포함</b>한다(반차 시간대 도입, 2026-08-08).
         *   미결 잠금 술어가 {@code START/END_TIME NOT NULL} 이고 반차 REQ 가 이제 경계 시각을
         *   기록하기 때문이다. 이름만 보고 시간차 전용으로 오해하지 말 것 — 그 오해가 F-7 오안내의 원인이었다.
         */
        PENDING_HOURLY_LEAVE
    }

    /**
     * 확정 연차(단위 무관)로 잠긴 경우의 안내 문구.
     *
     * <p>반반차(05)는 LC-10 으로 폐지되어 신규 신청이 불가하므로 열거에서 제외한다(2026-08-08).
     *   잔존 구 데이터가 잠금에 걸려도 "연차" 상위 개념으로 읽히므로 오안내가 되지 않는다.
     */
    public static final String MSG_CONFIRMED_LEAVE =
            "연차(종일·반차·시간차)가 등록된 날의 근무계획은 변경할 수 없습니다. 연차를 먼저 취소·처리한 뒤 변경해 주세요.";

    /**
     * 미결 <b>시각 보유</b> 연차 신청(반차 + 시간차)만으로 잠긴 경우의 안내 문구.
     *
     * <p>★ 문구 정정(2026-08-08) — 반차 시간대 도입으로 반차 REQ 가 경계 시각을 기록하게 되면서
     *   미결 <b>반차</b>도 이 분기에 들어온다. 종전 문구("시간 단위 연차"만 지목)를 그대로 두면
     *   반차로 막힌 관리자가 존재하지 않는 시간차를 찾게 된다 — F-7 과 동일 계열의 오안내다.
     *   {@code USER_400_073}(사업장 이동)은 같은 이유로 이미 "반차 또는 시간 단위 연차"로 확장돼 있다.
     */
    public static final String MSG_PENDING_HOURLY_LEAVE =
            "반차 또는 시간 단위 연차 신청이 처리 대기 중인 날의 근무계획은 변경할 수 없습니다. 해당 신청을 먼저 승인·반려한 뒤 변경해 주세요.";

    /**
     * 잠금 목록에서 대표 사유를 뽑는다. 우선순위 = OT &gt; 확정 연차 &gt; 미결 시간차.
     * 목록이 비어 있으면 {@link LockKind#PENDING_HOURLY_LEAVE}(가장 약한 사유)를 돌려준다(기존 Attd05 동작 동일).
     */
    public static LockKind resolveLockKind(List<ScheduleLockVO> locks) {
        boolean otLocked = false;
        boolean confirmedLeave = false;
        if (locks != null) {
            for (ScheduleLockVO lock : locks) {
                if (lock == null) continue;
                if (lock.getReason() == ScheduleLockVO.Reason.OT) {
                    otLocked = true;
                } else if (!lock.isLeavePending()) {
                    confirmedLeave = true;
                }
            }
        }
        if (otLocked) {
            return LockKind.OVERTIME;
        }
        return confirmedLeave ? LockKind.CONFIRMED_LEAVE : LockKind.PENDING_HOURLY_LEAVE;
    }

    /**
     * 연차 잠금(LEAVE)으로 근무계획 변경이 차단될 때의 안내 문구({@code ATTD_400_164} 동적 메시지).
     *
     * <p>OT 잠금은 본 문구의 대상이 아니다(해당 경로는 상호배제 가드가 따로 담당). 목록에 OT 만 있어도
     *   호출자는 LEAVE 잠금이 있을 때만 부르므로, 판정은 확정 연차 우선 → 미결 시간차 순으로만 갈린다.
     */
    public static String scheduleChangeBlockedMessage(List<ScheduleLockVO> locks) {
        boolean confirmedLeave = false;
        if (locks != null) {
            for (ScheduleLockVO lock : locks) {
                if (lock == null) continue;
                if (lock.getReason() == ScheduleLockVO.Reason.LEAVE && !lock.isLeavePending()) {
                    confirmedLeave = true;
                    break;
                }
            }
        }
        return confirmedLeave ? MSG_CONFIRMED_LEAVE : MSG_PENDING_HOURLY_LEAVE;
    }
}
