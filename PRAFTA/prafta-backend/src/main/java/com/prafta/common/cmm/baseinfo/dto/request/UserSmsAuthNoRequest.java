package com.prafta.common.cmm.baseinfo.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserSmsAuthNoRequest {
	private String cmpnyCd;

    /**
     * 수신 휴대폰번호(하이픈 허용).
     *
     * <p>SMS2-D3(sec M-1 / L-3): 이 엔드포인트는 {@code @NoAuth} 이고 입력값이 그대로 발송 수신번호가 된다.
     * 형식 검증이 없으면 다음 두 가지가 열린다.
     * <ul>
     *   <li><b>국제번호 발송</b> — 국가별 단가가 국내의 수 배~수십 배라 과금 피해가 그만큼 커진다.</li>
     *   <li><b>레이트리밋 버킷 회피</b> — {@code 01012345678} 과 {@code 821012345678} 은 서로 다른 HMAC 이라
     *       같은 사람인데 다른 버킷으로 세어진다.</li>
     * </ul>
     * 국내 휴대폰/유선 형태(0으로 시작, 총 10~11자리)만 통과시킨다.
     */
    @FieldLabel("휴대폰번호")
    @NotBlank
    @Pattern(regexp = "^0\\d{1,2}-?\\d{3,4}-?\\d{4}$", message = "휴대폰번호 형식이 올바르지 않습니다.")
    private String mblNo;

    private String dupChkYn;		/* 휴대폰번호 중복 체크 필요 여부 Y/N */
}
