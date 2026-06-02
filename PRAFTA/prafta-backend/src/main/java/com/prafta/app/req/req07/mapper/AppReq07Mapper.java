package com.prafta.app.req.req07.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.req.req07.application.command.AttdReqInsertCommand;

/**
 * prafta-app-007: 모바일 앱 근태 요청 폼 3종 (스케줄 수정 / 근태 보정 / 초과근무) 전용 Mapper.
 *
 * <p>REQ_ID 채번은 LeaveFlowMapper.selectNextReqId 와 동등한 SQL 을 복제 (네임스페이스 분리, dependency 최소화).
 */
@Mapper
public interface AppReq07Mapper {

    /**
     * REQ_ID 채번 (CONCAT(YYYYMMDD, FNC_CMM_SEQ_NEXTVAL)).
     * LeaveFlowMapper.selectNextReqId 와 동일 시퀀스('ATTD_REQ_ID') 사용 — 채번 공간 공유.
     */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * tb_user_attd_req INSERT (3 endpoint 공통).
     * 각 endpoint 가 사용하지 않는 컬럼은 command 에서 null 로 전달.
     */
    int insertAttdReq(AttdReqInsertCommand command);

    /**
     * 중복 요청 차단 (P10): 동일 USER_CD + WORK_YMD + REQ_TYPE + REQ_STATUS='01' 미처리 행 카운트.
     * 결과 > 0 이면 4xx (ATTD_400_090).
     */
    int countDuplicateReq(@Param("cmpnyCd") String cmpnyCd
                          , @Param("siteCd") String siteCd
                          , @Param("userCd") String userCd
                          , @Param("workYmd") String workYmd
                          , @Param("reqType") String reqType);

    /**
     * 근태 보정 자동 분기 (Q2): 해당 (USER_CD, WORK_YMD, WORK_SEQ) 의 미삭제 tb_user_attd_mgmt 행의 ATTD_ID 조회.
     * 결과:
     * <ul>
     *   <li>행 존재 → ATTD_ID 반환 → REQ_TYPE='02' (근태 수정), TARGET_ID=반환값.</li>
     *   <li>행 부재 → null 반환 → REQ_TYPE='01' (근태 생성), TARGET_ID=null.</li>
     * </ul>
     */
    String selectExistingAttdId(@Param("cmpnyCd") String cmpnyCd
                                , @Param("siteCd") String siteCd
                                , @Param("userCd") String userCd
                                , @Param("workYmd") String workYmd
                                , @Param("workSeq") Integer workSeq);
}
