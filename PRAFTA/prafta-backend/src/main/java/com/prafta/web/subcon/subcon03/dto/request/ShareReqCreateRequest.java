package com.prafta.web.subcon.subcon03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 데이터 공유 요청 생성 요청(PRAFTA-SUBCON-T3 §5-3).
 *
 * <p>요청측 회사(REQ_CMPNY_CD)/요청자는 클라가 보내지 않는다(서버 JWT 클레임 gv_* 사용).
 * 제공측 대응 사업장(TARGET_SITE_CD)도 클라 입력을 받지 않는다 — 서버가 사업장 연동 체인으로 해석한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShareReqCreateRequest {
    private String prvCmpnyCd;   // 제공측 회사코드(관계 ACCEPTED 상대)
    private String siteCd;       // 요청측(자사) 사업장코드 — 서버가 이 값으로 제공측 사업장을 해석
    private String dataType;     // 데이터 유형[SYS077] — 화이트리스트 ATTD 만 허용
    private String periodStr;    // 대상 기간 시작(YYYYMMDD)
    private String periodEnd;    // 대상 기간 종료(YYYYMMDD)
    private String closedOnlyYn; // 마감 근태만 요청 여부(Y/N — 기본 Y)
    private String purpose;      // 제공 목적(필수 ≤500자)
}
