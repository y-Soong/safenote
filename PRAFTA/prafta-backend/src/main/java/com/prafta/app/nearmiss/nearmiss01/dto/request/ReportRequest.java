package com.prafta.app.nearmiss.nearmiss01.dto.request;

import lombok.Data;

/**
 * A1 근로자 아차사고 보고 등록 요청 (multipart/form-data).
 *
 * <p>단일 사진은 컨트롤러에서 @RequestPart(value="item") MultipartFile 로 별도 수신한다.
 * <p>multipart 바인딩 특성상 Lombok @Data 사용(setter 필수).
 * <p>식별자(cmpnyCd/siteCd/userCd)는 본문에서 받지 않고 JWT 클레임에서만 도출(IDOR 차단).
 */
@Data
public class ReportRequest {
    private String incidentTypeCd;      // 사건유형[SYS061] (필수)
    private String processCd;           // 공정코드[COM002] (선택)
    private String occurDtime;          // 발생일시 'YYYY-MM-DD HH:mm' (필수)
    private String locationDesc;        // 발생장소(직접입력, 선택)
    private String description;          // 사건 경위(필수)
    private String potentialSeverityCd; // 잠재중대성[SYS062] (선택)
    private String immediateActionDesc; // 보고자 즉시조치(선택)
}
