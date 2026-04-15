package com.prafta.common.cmm.login.dto.request;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    String authCd;
    String email;
    String gender;
    String useYn;
}
