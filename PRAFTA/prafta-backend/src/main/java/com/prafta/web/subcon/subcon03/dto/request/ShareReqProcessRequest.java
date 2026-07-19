package com.prafta.web.subcon.subcon03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 데이터 공유 요청 상태 전이(취소/거부) 요청(PRAFTA-SUBCON-T3 §4).
 *
 * <p>행위자 소속 회사는 클라가 보내지 않는다(서버 JWT 클레임 gv_cmpnyCd 사용).
 * comment 는 거부 시 필수(≤500자), 취소는 선택.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShareReqProcessRequest {
    private Long shareReqId;  // 공유요청ID(tb_cmpny_share_req PK)
    private String comment;   // 처리 코멘트(거부 사유 등)
}
