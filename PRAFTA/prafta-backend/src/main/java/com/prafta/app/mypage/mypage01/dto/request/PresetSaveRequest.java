package com.prafta.app.mypage.mypage01.dto.request;

import java.util.List;

import lombok.Data;

/**
 * prafta-app-010-05: 프리셋 저장(신규/수정) 요청.
 *
 * <p>{@code presetId} null=신규, 존재=수정. {@code approverUserCds}는 위→아래 결재 순서.
 * 소유자는 JWT userCd 로 강제한다(바디 미수신).
 */
@Data
public class PresetSaveRequest {
    private String presetId;
    private String presetNm;
    private String defaultYn;            // "Y" | "N"
    private List<String> approverUserCds;
}
