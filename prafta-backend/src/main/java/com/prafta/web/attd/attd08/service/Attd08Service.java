package com.prafta.web.attd.attd08.service;

import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;
import com.prafta.web.attd.attd08.dto.response.AttdGpsTrailResponse;
import com.prafta.web.attd.attd08.dto.response.AttdListsResponse;

public interface Attd08Service {

    AttdListsResponse getAttdLists(AttdListsParam param);

    AttdGpsTrailResponse getAttdGpsTrail(AttdGpsTrailParam param);
}
