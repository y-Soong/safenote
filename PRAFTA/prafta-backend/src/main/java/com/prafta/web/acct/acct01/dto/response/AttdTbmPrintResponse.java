package com.prafta.web.acct.acct01.dto.response;

import java.util.List;

import com.prafta.web.acct.acct01.result.AcctResult;
import com.prafta.web.acct.acct01.result.AttendanceLinkResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;
import com.prafta.web.acct.acct01.result.TbmLinkResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 근태(스케줄 + 실근태) + TBM 교육 합본 출력(③) 응답.
 * 식별자는 사고 스냅샷, 상세는 사고일/사업장 라이브 재조회(하이브리드). 대상은 사고 피해자 본인 한정.
 * 일용직(victimUserTypeCd='DAILY')은 스케줄이 없어 schedule=null + scheduleNote 안내. 실근태/TBM 은 DAILY 도 정상 조회.
 */
@Getter
@Builder
public class AttdTbmPrintResponse {
    private AcctResult acctHeader;
    private boolean hasSchedule;
    private String scheduleNote;
    private ScheduleLinkResult schedule;
    private List<AttendanceLinkResult> records;
    private List<TbmLinkResult> tbmList;
}
