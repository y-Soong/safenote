package com.prafta.web.baim.baim05.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 QR 일일계정 발급 요청.
 *
 * <p>PRAFTA_COM_001 T1-04(Med 재작업): siteCd/userNm/mblNo/slotNo 입력 검증.
 * 검증이 없으면 mblNo null 시 휴대폰 없는 계정 생성 + 중복검증 우회, slotNo null 시 0행 UPDATE 로
 * 슬롯 없는 계정이 생성된다. 컨트롤러의 @Valid 와 연동해 진입 시점에 차단한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class InsertDailyQrUserRequest {

    @NotBlank
    @Size(max = 50)
    private String siteCd;

    @NotBlank
    @Size(max = 50)
    private String userNm;

    // 휴대폰번호(하이픈 허용). 서버에서 Normalizers.normalizePhone 으로 숫자만 추출 후 HMAC/ENC 처리.
    @NotBlank
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$")
    private String mblNo;

    // 슬롯 번호(1~N, TB_DAILY_USER_SLOT.SLOT_NO varchar(4)). 숫자 1~4자리.
    @NotBlank
    @Pattern(regexp = "^\\d{1,4}$")
    private String slotNo;
}
