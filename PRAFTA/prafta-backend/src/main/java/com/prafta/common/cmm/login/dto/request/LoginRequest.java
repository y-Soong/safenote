package com.prafta.common.cmm.login.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // ===== prafta-com-003 C3: 디바이스 식별/메타(부정탐지 baseline 적재용) =====
    //   모두 nullable. 웹 로그인은 미전송(null), 구버전 앱도 null. 적재는 로그인 성공 후
    //   예외 격리 훅에서 best-effort 로 수행하며, 적재 실패가 로그인 자체를 막지 않는다.
    //   deviceId 는 클라 제공값(위조 가능) — 식별/인가에 쓰지 않고 이력/디바이스 상태 적재에만 사용.
    //   axios(앱)가 보내는 디바이스ID 키는 gv_deviceId 이므로 @JsonProperty 로 바인딩한다.
    @JsonProperty("gv_deviceId")
    private String deviceId;
    private String deviceType;   // 'ANDROID' / 'IOS' (네이티브 미주입 시 null)
    private String deviceModel;
    private String osVersion;
    private String appVersion;
}
