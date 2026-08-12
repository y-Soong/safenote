package com.prafta.common.cmm.login.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 셀프가입(회원가입) 요청 — <b>비로그인 공개 EP</b>({@code @NoAuth})의 입력이다.
 *
 * <p>★★[security C-1] <b>권한(authCd) 필드를 두지 않는다. 절대 되살리지 말 것.</b>
 * 이 EP 는 인증 없이 누구나 호출할 수 있으므로, 바디로 받은 권한코드를 그대로 적재하면
 * {@code authCd:"master"} 로 가입해 관리자 승인 한 번으로 회사 전체 권한 계정이 완성된다
 * (승인 시트에 권한 입력란이 없어 승인자는 알아챌 수도 없다). 셀프가입 계정의 권한은
 * <b>서버가 일반사원('99999')으로 고정</b>한다 — INSERT/재가입 UPDATE/승인 UPDATE 3곳 모두.
 *
 * <p>같은 이유로 {@code useYn} 도 신뢰하지 않는다(INSERT 는 리터럴 값을 쓴다).
 */
@Getter
@Setter
@NoArgsConstructor
public class UserJoinRequest{
	@FieldLabel("회사코드")
	@NotBlank()
	@Size(max = 50)
    String cmpnyCd;
	
	@FieldLabel("아이디")
	@NotBlank()
	@Size(max = 50)
    String userId;
	
	@FieldLabel("비밀번호")
	@NotBlank()
	@Size(max = 50)
    String userPw;
	
	@FieldLabel("이름")
	@NotBlank()
	@Size(max = 50)
    String userNm;
	
	@FieldLabel("사업장")
	@NotBlank()
	@Size(max = 50)
    String siteCd;
	
	@FieldLabel("휴대폰번호")
	@NotBlank()
	@Size(max = 50)
    String mblNo;
    
    @FieldLabel("생년월일")
	@NotBlank()
	@Size(max = 6)
    String birthDt;
	
    String nodeCd;
    String email;
    String gender;
    String useYn;

    /**
     * 셀프가입 SMS 본인인증 번호 (선택 전달).
     *
     * <p>[security H-1] 서버는 <b>실제 인증 완료 기록</b>(TB_SMS_AUTH_CODE.VERIFIED_YN='Y',
     * PURPOSE_CD='SELF_JOIN')을 휴대폰 HMAC 으로 직접 확인하므로 본 값이 없어도 검증은 이뤄진다.
     * 값이 오면 인증 기록의 코드와 일치하는지 한 번 더 대조한다(향후 클라이언트 계약 강화용).
     */
    String certNo;
}
