package com.prafta.web.subcon.subcon03.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 데이터 공유 요청 승인(= 스냅샷 생성) 요청(PRAFTA-SUBCON-T3 §5-6·§5-7).
 *
 * <p>bundleSnapshotIds 는 제공측이 하위로부터 수신 보유 중인 스냅샷 중 함께 묶어 보낼 대상이다.
 * 클라 목록은 신뢰하지 않는다 — 승인 트랜잭션 안에서 후보 4조건(소유/사업장체인/기간포함/미마감표식)을
 * 전부 재검증하고 미매칭 시 SUBCON_409_008 로 거부한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShareReqApproveRequest {
    private Long shareReqId;              // 공유요청ID
    private List<Long> bundleSnapshotIds; // 함께 제공할 수신 보유 스냅샷ID 목록(선택)
}
