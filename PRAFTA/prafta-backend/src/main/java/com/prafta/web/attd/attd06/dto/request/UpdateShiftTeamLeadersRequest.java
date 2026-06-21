package com.prafta.web.attd.attd06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateShiftTeamLeadersRequest {
    private String siteCd;
    private String userCd;
    private String leaderYn;
    // prafta-com-016-D 보안 재작업: 조장 토글 WHERE 범위를 교대팀 단위로 좁히기 위한 키(권한 가드 1차 방어 + 키 정합).
    private String shiftCd;
    private String shiftTeamId;
    private String teamIdx;
}
