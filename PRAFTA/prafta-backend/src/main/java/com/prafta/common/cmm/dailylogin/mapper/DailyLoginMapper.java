package com.prafta.common.cmm.dailylogin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.dailylogin.application.command.DailyActiveTokenCommand;
import com.prafta.common.cmm.dailylogin.application.command.DailyUserPwdFailCommand;
import com.prafta.common.cmm.dailylogin.application.command.DailyUserPwdUnlockCommand;
import com.prafta.common.cmm.dailylogin.application.query.DailyLoginQuery;
import com.prafta.common.cmm.dailylogin.result.DailyUserResult;

/**
 * PRAFTA-app-027-2 — 일용직 직접 로그인 매퍼.
 *
 * <p>정규 LoginMapper 와 완전히 분리한다(TB_DAILY_USER 전용). 리프레시 토큰은 정규와 동일한
 * TB_AUTH_TOKEN 을 재사용한다(FK/스코프 제약 없음 — 일용직 USER_CD 수용 가능).
 */
@Mapper
public interface DailyLoginMapper {

    /**
     * 로그인 대상 일용직 조회(USE_YN='Y' 가드 내장).
     *
     * <p>cmpnyCd 미전송 + 동일 USER_ID 가 복수 회사에 활성 공존하면 다건이 반환될 수 있다.
     * 단건이 아닌 경우(0건/다건)는 서비스에서 통합 차단(500 노출 방지).
     */
    List<DailyUserResult> selectDailyUserForLogin(DailyLoginQuery query);

    /**
     * 잠금 만료 해제(PWD_LOCK_YN='N', 실패횟수 0). 정규 미러.
     *
     * <p>로그인 진입 시 호출 — 실제 잠금 만료 시각이 지난 행에만 적용(미잠금 구간 실패 누적 보존).
     */
    int updateDailyUserPwdUnlock(DailyUserPwdUnlockCommand command);

    /** 비밀번호 인증 성공 시 실패 카운트/잠금 무조건 초기화. */
    int updateDailyUserPwdReset(DailyUserPwdUnlockCommand command);

    /** 비밀번호 실패 누적 + 임계 도달 시 잠금. 정규 미러. */
    int updateDailyUserPwdFail(DailyUserPwdFailCommand command);

    /** 마지막 로그인 일시 갱신. */
    int updateDailyUserLastLoginDtime(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 동일 (회사/사용자/클라이언트) 기존 세션 revoke. 정규 미러. */
    int revokeDailyActiveToken(DailyActiveTokenCommand command);

    /** 신규 리프레시 토큰 세션 INSERT. 정규 미러. */
    int insertDailyAuthToken(DailyActiveTokenCommand command);
}
