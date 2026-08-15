package com.prafta.web.user.user09.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user09.application.query.SelfJoinHistoryListQuery;
import com.prafta.web.user.user09.application.query.SelfJoinListQuery;
import com.prafta.web.user.user09.result.SelfJoinHistoryRowResult;
import com.prafta.web.user.user09.result.SelfJoinRowResult;
import com.prafta.web.user.user09.result.SelfJoinTargetResult;

/**
 * 소정-09: 셀프가입 승인/거부(User_09) 매퍼.
 *
 * <p>모든 쿼리는 CMPNY_CD 술어를 필수로 가진다(멀티테넌시). 부서 술어에는 반드시 SITE_CD 를
 * 함께 세운다 — NODE_CD 는 사업장 간 중복 코드다(실측: '001' 의 'n1' 이 7개 사업장에 존재).
 */
@Mapper
public interface User09Mapper {

    /** 셀프가입 신청 목록 ('06' 승인대기 / '07' 가입거부). 휴대폰은 암호문으로 나오며 서비스가 마스킹한다. */
    List<SelfJoinRowResult> selectSelfJoinList(SelfJoinListQuery query);

    /**
     * 셀프가입 처리 이력 목록 (TB_AUDIT_LOG RESOURCE_TYPE='SELF_JOIN_APPROVAL', 1페이지 분).
     *
     * <p>승인 시 계정 상태가 일반 재직자('01')와 같아져 TB_USER 로는 승인 건을 구분할 수 없다.
     * 이력의 유일한 출처는 감사 로그다. 휴대폰은 암호문으로 나오며 서비스가 마스킹한다.
     */
    List<SelfJoinHistoryRowResult> selectSelfJoinHistoryList(SelfJoinHistoryListQuery query);

    /** 셀프가입 처리 이력 전체 건수(페이저용). 목록과 <b>완전히 동일한 술어</b>를 쓴다. */
    int selectSelfJoinHistoryCount(SelfJoinHistoryListQuery query);

    /**
     * 승인/거부 대상 계정의 서버 권위값 1건 (사업장/부서/상태).
     *
     * @return 대상 행. 타 회사이거나 미존재면 null
     */
    SelfJoinTargetResult selectSelfJoinTarget(@Param("cmpnyCd") String cmpnyCd,
                                              @Param("userCd") String userCd);

    /**
     * 승인 — 인사정보 보강 + ACCOUNT_STATUS '06' → '01'.
     *
     * <p>WHERE 에 {@code ACCOUNT_STATUS='06'} 를 둬 동시 승인/거부를 낙관적으로 차단한다.
     *
     * @return 갱신 행 수 (0 = 이미 처리됨/대상 아님)
     */
    int updateApproveSelfJoin(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd,
                              @Param("hireDate") String hireDate,
                              @Param("employmentType") String employmentType,
                              @Param("rankCd") String rankCd,
                              @Param("updateNo") String updateNo);

    /**
     * 거부 — ACCOUNT_STATUS '06' → '07' + USE_YN='N' (행 보존, 재가입 시 재활용 대상).
     *
     * @return 갱신 행 수 (0 = 이미 처리됨/대상 아님)
     */
    int updateRejectSelfJoin(@Param("cmpnyCd") String cmpnyCd,
                             @Param("userCd") String userCd,
                             @Param("updateNo") String updateNo);

    /** 직급코드[COM007] 유효성 (회사 스코프, 사용중). 미입력 검증은 호출부에서 처리. */
    int selectRankCdExists(@Param("cmpnyCd") String cmpnyCd,
                           @Param("rankCd") String rankCd);
}
