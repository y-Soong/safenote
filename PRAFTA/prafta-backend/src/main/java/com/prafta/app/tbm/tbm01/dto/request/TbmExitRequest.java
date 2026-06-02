package com.prafta.app.tbm.tbm01.dto.request;

import lombok.Data;

/**
 * prafta-app-004-C2: TBM 종료 요청(multipart/form-data).
 * <p>종료 서명 파일은 컨트롤러에서 @RequestPart(value="item") MultipartFile 로 별도 수신.
 * <p>multipart 바인딩 특성상 Lombok @Data 사용(setter 필수).
 * <p>USER_CD/CMPNY_CD/SITE_CD 는 바디로 받지 않는다(JWT 출처). USER_TYPE_CD='REGULAR' 고정.
 */
@Data
public class TbmExitRequest {
    private String sessionCd;
    private String exitPwd;
}
