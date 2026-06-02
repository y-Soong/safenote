package com.prafta.common.cmm.leave.vo;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 연차 수동 부여(단일/일괄) 결과(attd09).
 *
 * <p>{@code grantedUserCds}는 INSERT가 성공한 대상 사용자 코드 목록(직원당 1건).
 * 트랜잭션 단위로 전건 성공 또는 전건 롤백된다.
 */
@Getter
@Builder
public class ManualGrantResultVO {

    /** 부여 성공 건수 */
    private final int grantedCount;

    /** 부여된 사용자 코드 목록 */
    private final List<String> grantedUserCds;
}
