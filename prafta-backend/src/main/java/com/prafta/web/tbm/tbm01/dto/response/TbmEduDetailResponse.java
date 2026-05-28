package com.prafta.web.tbm.tbm01.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduUsedSessionResult;

import lombok.Builder;
import lombok.Value;

/**
 * prafta-033-A: W-03 콘텐츠 상세 응답.
 *  - tbmEduInfo: 콘텐츠 묶음 마스터 1건
 *  - tbmEduItemInfoList: 세부항목(미디어) 목록
 *  - usedSessionList: 이 묶음을 사용한 TBM 세션 이력(B 이전 빈 목록)
 */
@Value
@Builder(toBuilder = true)
public class TbmEduDetailResponse {
	TbmEduInfoResult tbmEduInfo;

	List<TbmEduItemInfoResult> tbmEduItemInfoList;

	List<TbmEduUsedSessionResult> usedSessionList;
}
