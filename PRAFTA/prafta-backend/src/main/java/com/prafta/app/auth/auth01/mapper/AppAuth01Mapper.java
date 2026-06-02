package com.prafta.app.auth.auth01.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * prafta-app-010-07: 회원 탈퇴 Mapper (auth01).
 *
 * <p>탈퇴는 단일 트랜잭션: 프리셋 hard delete → tb_user 마스킹/상태변경 → tb_del_user INSERT.
 * 연차 자동취소·결재자 알림은 미구현(D5). 출퇴근/근태/안전/TBM 기록은 보존(USER_CD 유지).
 */
@Mapper
public interface AppAuth01Mapper {

    /** 탈퇴 전 본인 USER_ID 조회(tb_del_user PK 용). 없으면 null. */
    String selectUserId(@Param("cmpnyCd") String cmpnyCd,
                        @Param("userCd") String userCd);

    /** 본인 소유 프리셋 디테일 전량 hard delete. */
    int deleteMyPresetSteps(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd);

    /** 본인 소유 프리셋 마스터 전량 hard delete. */
    int deleteMyPresetMasters(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd);

    /** tb_user 탈퇴 처리(ACCOUNT_STATUS='03' + PII 마스킹/무효화 + WITHDRAWAL_DATE + USER_PW 무효화). */
    int withdrawUser(@Param("cmpnyCd") String cmpnyCd,
                     @Param("userCd") String userCd,
                     @Param("maskedUserNm") String maskedUserNm);

    /** tb_del_user 탈퇴 이력 INSERT(마스킹 이름). */
    int insertDelUser(@Param("cmpnyCd") String cmpnyCd,
                      @Param("userId") String userId,
                      @Param("maskedUserNm") String maskedUserNm,
                      @Param("insertNo") String insertNo);
}
