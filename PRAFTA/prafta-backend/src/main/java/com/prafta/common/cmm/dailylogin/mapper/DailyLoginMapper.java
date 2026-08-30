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
     * 비활성 사유 노출용 보조 조회(USE_YN 가드 없음 — 비활성/만료/종료사업장 계정도 반환).
     *
     * <p>로그인 본 조회({@link #selectDailyUserForLogin})가 0건일 때만 호출한다. 서비스는 여기서
     * 반환된 행의 비밀번호가 일치하고 계정이 비활성(USE_YN!='Y' 또는 ACCOUNT_STATUS='05' 또는 탈퇴)인
     * 경우에만 {@code DAILYLOGIN_400_003}(명시 안내)을 노출한다. 그 외(비번 불일치/사업장 종료 등)는
     * 통합 메시지(001)로 귀결시켜 계정 존재를 노출하지 않는다.
     */
    List<DailyUserResult> selectDailyUserForInactiveCheck(DailyLoginQuery query);

    /**
     * 비밀번호 잠금 잔여분 조회 — DB 서버 시계 기준(세션 타임존 무관, 정규 selectPwdLockRemainMinutes 미러).
     *
     * <p>PWD_LOCK_EXPIRE_DTIME 은 DATE_ADD(NOW(), ...) 로 DB 서버 시계로 기록되므로,
     * Java 시계(LocalDateTime.now())와 비교하면 세션 타임존이 UTC 인 운영에서 항상 "만료"로
     * 오판되어 잠금이 무력화된다. 잔여 판정을 DB 로 옮겨 서버 시계끼리만 비교한다.
     *
     * @return 잔여 분(올림). 잠금 아님/만료면 null
     */
    Integer selectDailyPwdLockRemainMinutes(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

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
