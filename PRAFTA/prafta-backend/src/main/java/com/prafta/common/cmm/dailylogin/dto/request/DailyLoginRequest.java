package com.prafta.common.cmm.dailylogin.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-app-027-2 — 일용직 직접 로그인 요청.
 *
 * <p>자격증명은 POST + JSON 본문으로만 수신한다(쿼리스트링 노출 금지).
 * cmpnyCd 는 선택값 — 입력 시 해당 회사로 추가 제약, 미입력 시 USER_ID 단독으로 조회한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class DailyLoginRequest {

    @FieldLabel("아이디")
    @NotBlank
    @Size(max = 50)
    private String userId;

    @FieldLabel("비밀번호")
    @NotBlank
    @Size(max = 100)
    private String userPw;

    // 선택값: 입력 시 AND CMPNY_CD 제약 추가.
    @Size(max = 50)
    private String cmpnyCd;
}
