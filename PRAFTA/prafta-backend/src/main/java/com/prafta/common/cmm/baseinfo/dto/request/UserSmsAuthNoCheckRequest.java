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
public class UserSmsAuthNoCheckRequest {
	private String cmpnyCd;

    /**
     * 검증 대상 휴대폰번호(하이픈 허용).
     *
     * <p>[3차 / sec N-10 · qa Q-5] 1차 sec L-3 의 잔여분이다.
     * 2차는 발송 EP({@code sms-auth-sends})에만 검증을 붙여 <b>검증 EP 는 여전히 NPE(500)</b> 였다
     * ({@code BaseinfoServiceImpl.userSmsAuthCheck} 첫 줄이 {@code param.mblNo().replaceAll(...)}).
     * 무인증 EP 에서 500 이 나면 스택트레이스가 로그를 채우고, 하필 대입 방어의 대상 EP 다.
     *
     * <p>패턴은 발송 EP({@code UserSmsAuthNoRequest})와 동일하게 유지한다 —
     * 발송은 되는데 검증은 400 이 나는(또는 그 반대) 불일치를 만들지 않기 위함이다.
     */
    @FieldLabel("휴대폰번호")
    @NotBlank
    @Pattern(regexp = "^0\\d{1,2}-?\\d{3,4}-?\\d{4}$", message = "휴대폰번호 형식이 올바르지 않습니다.")
    private String mblNo;

    /**
     * 인증번호.
     *
     * <p>★{@code @Pattern}(6자리 숫자)을 붙이지 말 것.
     * 형식 검증이 서비스 진입보다 앞서면 형식이 틀린 요청이 <b>대입 카운터를 올리지 않고</b> 조기 반환되어,
     * 공격자가 "형식 오류 → 카운터 회피" 로 무한 시도할 수 있는 경로가 생긴다
     * ({@code BaseinfoServiceImpl.userSmsAuthCheck} Javadoc 과 같은 사유. 별건 백로그).
     */
    private String certNo;
}
