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
public class LoginRequest {
	@FieldLabel("아이디")
	@NotBlank()
	@Size(max = 50)
	private String userId;
	
    private String userNm;
    
    @FieldLabel("비밀번호")
	@NotBlank()
    @Size(max = 50)
    private String userPw;
    
    private String systValDCd;
}
