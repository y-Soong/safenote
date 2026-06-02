package com.prafta.web.user.user04.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 결재라인 프리셋 저장(생성/수정) 요청 body (prafta-020).
 * POST /user04/presets/save.
 *
 * <p>소유자(USER_CD)·회사·사업장은 토큰으로 강제(body 미신뢰). presetId 없으면 신규.
 */
@Getter
@Setter
@NoArgsConstructor
public class PresetSaveRequest {

    /** 프리셋 ID (수정 시 필수, 신규는 null/빈값) */
    @Size(max = 20)
    private String presetId;

    /** 프리셋 이름 */
    @NotBlank
    @Size(max = 100)
    private String presetNm;

    /** 기본 프리셋 여부 (Y/N) */
    @Size(max = 1)
    private String defaultYn;

    /** 결재 순서대로의 결재자 사용자코드 목록 (단계 상한 20, 코드 길이 20) */
    @Size(max = 20)
    private List<@Size(max = 20) String> approverUserCds;
}
