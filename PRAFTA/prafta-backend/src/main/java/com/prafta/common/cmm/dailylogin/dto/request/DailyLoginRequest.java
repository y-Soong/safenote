package com.prafta.common.cmm.dailylogin.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // ===== 디바이스 식별/메타(단일 활성기기 규칙 + 푸시 토큰 등록 선행 행 생성용) =====
    //   모두 nullable. 웹 일용직 로그인은 미전송(null), 구버전 앱도 null. 적재는 로그인 성공 후
    //   예외 격리 훅에서 best-effort 로 수행하며, 적재 실패가 로그인 자체를 막지 않는다(정규 로그인 미러).
    //   deviceId 는 클라 제공값(위조 가능) — 식별/인가에 쓰지 않고 디바이스 상태/이력 적재에만 사용.
    //   axios(앱)가 보내는 디바이스ID 키는 gv_deviceId 이므로 @JsonProperty 로 바인딩한다.
    @JsonProperty("gv_deviceId")
    private String deviceId;
    private String deviceType;   // 'ANDROID' / 'IOS' (네이티브 미주입 시 null)
    private String deviceModel;
    private String osVersion;
    private String appVersion;
}
