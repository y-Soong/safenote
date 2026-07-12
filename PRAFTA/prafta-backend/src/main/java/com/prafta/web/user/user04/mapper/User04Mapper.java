package com.prafta.web.user.user04.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user04.result.ApprovalCandidateResult;
import com.prafta.web.user.user04.result.AprvPresetStepVO;
import com.prafta.web.user.user04.result.AprvPresetVO;

/**
 * 결재라인 결재자 후보 조회 + 프리셋 CRUD Mapper (prafta-019-D, prafta-020, User_04).
 */
@Mapper
public interface User04Mapper {

    /** 본인 사업장 활성 사용자 후보 목록 (본인 제외, 소속부서·사용자명 필터). */
    List<ApprovalCandidateResult> selectApprovalCandidates(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCd") String siteCd,
                                                           @Param("excludeUserCd") String excludeUserCd,
                                                           @Param("nodeCd") String nodeCd,
                                                           @Param("userNm") String userNm);

    /** 본인 직급 순서(SORT_IDX) — 프리셋 임계값. 직급 미배정 시 null. */
    Integer selectUserRankSortIdx(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("userCd") String userCd);

    // ============================================================
    // 결재라인 프리셋 CRUD (prafta-020)
    // ============================================================

    /** 본인 소유 프리셋 마스터 목록 (기본 우선, 이름순). */
    List<AprvPresetVO> selectPresetMasters(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("userCd") String userCd);

    /** 본인 소유 프리셋의 전체 스텝(결재자) — 결재자 표시정보 JOIN, 프리셋·스텝 순. */
    List<AprvPresetStepVO> selectPresetStepsByUser(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("userCd") String userCd);

    /** 프리셋 소유자 USER_CD (소유권 검증용). 미존재 시 null. */
    String selectPresetOwner(@Param("cmpnyCd") String cmpnyCd,
                             @Param("presetId") String presetId);

    /** 프리셋 기본여부 DEFAULT_YN ('Y'/'N'). 미존재 시 null (8.4 삭제 가드용). */
    String selectPresetDefaultYn(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("presetId") String presetId);

    /** 프리셋 ID 채번 ('P' + YYYYMMDD + 시퀀스). */
    String selectNextPresetId(@Param("cmpnyCd") String cmpnyCd);

    /** 결재자 후보 유효성: 본인 사업장 활성 사용자(시스템계정 제외) 여부 카운트. */
    int countActiveCandidate(@Param("cmpnyCd") String cmpnyCd,
                             @Param("siteCd") String siteCd,
                             @Param("userCd") String userCd);

    int insertPresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId,
                           @Param("userCd") String userCd,
                           @Param("presetNm") String presetNm,
                           @Param("defaultYn") String defaultYn,
                           @Param("insertNo") String insertNo);

    int updatePresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId,
                           @Param("presetNm") String presetNm,
                           @Param("defaultYn") String defaultYn,
                           @Param("updateNo") String updateNo);

    int insertPresetStep(@Param("cmpnyCd") String cmpnyCd,
                         @Param("presetId") String presetId,
                         @Param("stepNo") int stepNo,
                         @Param("approverUserCd") String approverUserCd,
                         @Param("insertNo") String insertNo);

    int deletePresetSteps(@Param("cmpnyCd") String cmpnyCd,
                          @Param("presetId") String presetId);

    int deletePresetMaster(@Param("cmpnyCd") String cmpnyCd,
                           @Param("presetId") String presetId);

    /** 본인 소유 전 프리셋 기본해제 (DEFAULT_YN='N'). */
    int clearDefaultForUser(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd);

    /** 특정 프리셋 기본지정 (DEFAULT_YN='Y'). */
    int setDefault(@Param("cmpnyCd") String cmpnyCd,
                   @Param("presetId") String presetId,
                   @Param("updateNo") String updateNo);
}
