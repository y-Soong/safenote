package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 정책 기준 부여 프리뷰(dry-run) 직원 1명 행 (prafta-022 작업 D).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.7(권한).
 *
 * <p>적용(hireDateGrant)을 실제 수행하지 않고, 적용 시 발생할 변화를 직원별로 집계한 결과다.
 *
 * <p>prafta-032 D6: 입사일 변경 처리방식 자동계산(KEEP 계열/RESET_ALL) 폐기로
 * "처리방식(handlingType)"·"취소예정(cancelCount)" 행 데이터를 제거했다.
 * 행은 추가예정 일수(addDays)와 안내 사유(note)만 표시한다.
 * <ul>
 *   <li>{@code addDays} : 실제 신규 INSERT 될 일수 합(멱등 skip 분 제외).</li>
 *   <li>{@code note}    : 제외/안내 사유(예: 입사일 미입력, 입사일 미래, 변경 없음).</li>
 * </ul>
 */
@Getter
@Builder
public class PolicyGrantPreviewRowVO {

    /** 대상 직원 코드 */
    private String userCd;

    /** 실제 신규 INSERT 될 일수 합(멱등 skip 분 제외) */
    private int addDays;

    /** 제외/안내 사유(없으면 null) */
    private String note;
}
