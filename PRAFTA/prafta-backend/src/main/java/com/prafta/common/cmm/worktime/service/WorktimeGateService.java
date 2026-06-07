package com.prafta.common.cmm.worktime.service;

import com.prafta.common.dto.TokenInfo;

/**
 * prafta-app-022: 근무중 게이트(WorktimeGate) 공용 서비스.
 *
 * <p>"근무 중에만 허용"하는 안전활동(안전점검·위험성발굴 등록, TBM 입실 등)에서, 호출 시점에
 * 본인이 근무중인지 판정하고 미근무 시 차단한다. 앱/웹 어느 모듈에서도 호출 가능한 공용 영역
 * ({@code com.prafta.common.cmm.worktime})에 둔다.
 *
 * <p>근무중 정의(정책: safety §1): 당일(DB NOW 기준) 본인 {@code tb_user_attd_mgmt} 중
 * {@code CHECK_IN_TIME IS NOT NULL AND CHECK_OUT_TIME IS NULL AND DEL_YN='N'} 1건 이상.
 * 식별값(cmpnyCd/siteCd/userCd)은 전부 {@link TokenInfo} 출처(IDOR 차단).
 *
 * <p>본 게이트는 아차사고 보고(즉시성 예외)·관리/조회 기능에는 적용하지 않는다(정책: safety §2).
 */
public interface WorktimeGateService {

    /**
     * 본인이 현재(오늘) 근무중인지 판정한다.
     *
     * @param token JWT 클레임 정보(cmpnyCd/siteCd/userCd 출처)
     * @return 당일 열린 근태 1건 이상이면 true
     */
    boolean isWorking(TokenInfo token);

    /**
     * 본인이 근무중이 아니면 {@code WORKTIME_403_001} 로 차단한다.
     *
     * <p>안전활동 등록/입실 흐름의 진입부(저장/INSERT 직전)에서 호출한다.
     *
     * @param token JWT 클레임 정보(cmpnyCd/siteCd/userCd 출처)
     * @throws com.prafta.common.exception.ApiException 미근무 시 WORKTIME_403_001
     */
    void assertWorking(TokenInfo token);
}
