package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.AcctResult;
import com.prafta.web.acct.acct01.result.AcctVictimResult;
import com.prafta.web.acct.acct01.result.AttendanceLinkResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;
import com.prafta.web.acct.acct01.result.TbmLinkResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근태(스케줄 + 실근태) + TBM 교육 합본 출력(③) 응답.
 * 식별자는 사고 스냅샷, 상세는 사고일/사업장 라이브 재조회(하이브리드).
 * prafta-065 D3: 재해자 전원을 victimSections 로 순회 반환한다. victimList = 재해자 전원(순번순).
 * 일용직(userTypeCd='DAILY')은 스케줄이 없어 schedule=null + scheduleNote 안내. 실근태/TBM 은 DAILY 도 정상 조회.
 */
@Getter
@Builder
public class AttdTbmPrintResponse {
    private AcctResult acctHeader;

    /** @deprecated prafta-065 — victimSections 사용. 값 = 대표 재해자(victimSections 첫 항목)와 동일(A→B 배포 창 호환). */
    @Deprecated
    private boolean hasSchedule;
    /** @deprecated prafta-065 — victimSections 사용. */
    @Deprecated
    private String scheduleNote;
    /** @deprecated prafta-065 — victimSections 사용. */
    @Deprecated
    private ScheduleLinkResult schedule;
    /** @deprecated prafta-065 — victimSections 사용. */
    @Deprecated
    private List<AttendanceLinkResult> records;
    /** @deprecated prafta-065 — victimSections 사용. */
    @Deprecated
    private List<TbmLinkResult> tbmList;

    private List<AcctVictimResult> victimList;
    private List<VictimPrintSection> victimSections;
}
