package com.prafta.web.user.user04.service;

import com.prafta.web.user.user04.application.param.ApprovalCandidateParam;
import com.prafta.web.user.user04.application.param.PresetActionParam;
import com.prafta.web.user.user04.application.param.PresetSaveParam;
import com.prafta.web.user.user04.dto.response.ApprovalCandidateListResponse;
import com.prafta.web.user.user04.dto.response.PresetListResponse;

/**
 * 결재라인 구성 보조 + 프리셋 CRUD 서비스 (prafta-019-D, prafta-020, User_04).
 */
public interface User04Service {

    /** 결재자 후보(사용자 리스트) 조회 (본인 사업장, 부서/이름 필터). */
    ApprovalCandidateListResponse getApprovalCandidates(ApprovalCandidateParam param);

    /** 본인 소유 결재라인 프리셋 목록(스텝 포함) 조회. */
    PresetListResponse getPresets(String cmpnyCd, String userCd);

    /** 프리셋 저장(신규/수정). @Transactional. 저장된 PRESET_ID 반환. */
    String savePreset(PresetSaveParam param);

    /** 프리셋 삭제. @Transactional. */
    void deletePreset(PresetActionParam param);

    /** 프리셋 기본 지정(본인 다른 프리셋 기본 해제). @Transactional. */
    void setDefaultPreset(PresetActionParam param);
}
