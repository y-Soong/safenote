package com.prafta.web.subcon.subcon01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연동 관계 이력/해지 영향 요약 조회 요청(관계ID 단건).
 *
 * <p>당사자(요청측/상대측) 검증은 서버가 JWT 클레임(gv_cmpnyCd)으로 강제한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RelationHistRequest {
    private Long relationId;   // 관계ID(tb_cmpny_relation PK)
}
