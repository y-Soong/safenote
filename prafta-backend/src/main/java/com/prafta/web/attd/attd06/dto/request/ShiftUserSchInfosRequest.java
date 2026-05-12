package com.prafta.web.attd.attd06.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftUserSchInfosRequest {
    private ShiftMeta shiftMeta;
    private List<Team> teamList;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ShiftMeta {
    	private String shiftTeamId;
        private String siteCd;
        private String shiftCd;
        private String shiftTeamNm;
        private String startDate;
        private String endDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Team {
        private String teamIdx;
        private String teamNm;
        private List<Member> members;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Member {
        private String userCd;
    }
}
