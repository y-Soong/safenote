package com.prafta.web.user.user01.dto.request;

import java.util.List;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 경력 인정 저장 요청 (PRAFTA-017-4).
 * creditList 전량을 delete-and-insert(USE_YN='Y' 교체)한다.
 * cmpnyCd는 서버가 토큰으로 강제하므로 신뢰하지 않는다(IDOR 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCreditRequest {
    private String cmpnyCd;

    @FieldLabel("사용자코드")
    @NotBlank
    @Size(max = 20)
    private String userCd;

    @Valid
    private List<CreditItem> creditList;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreditItem {
        @FieldLabel("인정 개월 수")
        @Min(0)
        private Integer creditMonths;

        @FieldLabel("상세 설명")
        @Size(max = 500)
        private String reasonDetail;
    }
}
