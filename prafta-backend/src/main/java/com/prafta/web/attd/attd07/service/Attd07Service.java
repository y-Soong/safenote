package com.prafta.web.attd.attd07.service;

import com.prafta.web.attd.attd07.application.param.DailyAttdDetailDeleteParam;
import com.prafta.web.attd.attd07.application.param.DailyAttdDetailsParam;
import com.prafta.web.attd.attd07.application.param.MonthlyAttdListParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdInfosParam;
import com.prafta.web.attd.attd07.dto.response.AttdRecordListResponse;
import com.prafta.web.attd.attd07.dto.response.DailyAttdDetailsResponse;

public interface Attd07Service {

    AttdRecordListResponse getMonthlyAttdList(MonthlyAttdListParam param);

    void updateUserAttdInfos(UpdateUserAttdInfosParam param);

    DailyAttdDetailsResponse getDailyAttdDetails(DailyAttdDetailsParam param);

    void dailyAttdDetailDelete(DailyAttdDetailDeleteParam param);
}
