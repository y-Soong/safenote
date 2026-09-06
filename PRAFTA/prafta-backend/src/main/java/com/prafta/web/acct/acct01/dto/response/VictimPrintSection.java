package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.AcctVictimResult;
import com.prafta.web.acct.acct01.result.AttendanceLinkResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;
import com.prafta.web.acct.acct01.result.TbmLinkResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근태 + TBM 합본 출력(③)의 재해자 1명 단위 섹션(prafta-065 D3 — 재해자 전원 순회).
 * 일용직은 스케줄이 없어 schedule=null + scheduleNote 안내.
 */
@Getter
@Builder
public class VictimPrintSection {
    private AcctVictimResult victim;
    private boolean hasSchedule;
    private String scheduleNote;
    private ScheduleLinkResult schedule;
    private List<AttendanceLinkResult> records;
    private List<TbmLinkResult> tbmList;
}
