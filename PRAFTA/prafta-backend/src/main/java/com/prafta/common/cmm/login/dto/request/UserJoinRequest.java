package com.prafta.common.cmm.login.dto.request;

import java.util.List;

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

    /**
     * 화면에서 동의한 약관 목록.
     *
     * <p>종전에는 이 필드가 없어 서버가 필수약관 전건을 {@code AGR_YN='Y'} 리터럴로 적재했다.
     * 즉 동의 기록이 사용자의 체크가 아니라 <b>가입 API 가 호출됐다는 사실</b>만으로 만들어졌다
     * (화면이 넘긴 termsList 는 어느 경로에서도 읽히지 않았다 — 2026-08-13 확인).
     * 개인정보 수집이용·제3자 제공 동의는 분쟁 시 입증 자료이므로 실제 응답을 받아 적재한다.
     *
     * <p>★<b>필수(@NotNull)로 만들지 말 것.</b> 이 EP 는 스토어 배포된 구버전 앱도 호출한다.
     * 구버전은 이 필드를 보내지 않으므로 필수화하면 그 즉시 전 구버전 가입이 막힌다.
     * 강제 여부는 {@code prafta.self-join.terms-consent.enforced} 토글이 판정한다
     * (앱 버전 수렴 후 on — LoginServiceImpl.assertJoinTermsAgreed 주석 참조).
     */
    List<AgrTermsRequest> agrTermsList;
}
