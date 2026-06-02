package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-004-C1: TBM 입실 응답.
 * <p>D5 정책: 좌표 원본은 싣지 않고 거리(entryDistanceM)만 노출한다.
 * <p>alreadyEntered=true 면 UNIQUE 충돌(기입실)에 대한 멱등 안내.
 */
@Getter
@Builder
public class TbmEnterResponse {
    private final String attendanceCd;
    private final String entryAt;          // yyyy-MM-dd HH:mm:ss
    private final Integer entryDistanceM;  // 좌표 비노출, 거리 m만(D5)
    private final boolean alreadyEntered;  // 멱등(중복 입실) 안내
}
