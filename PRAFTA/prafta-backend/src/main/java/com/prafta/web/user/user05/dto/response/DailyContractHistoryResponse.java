package com.prafta.web.user.user05.dto.response;

import java.util.List;

import com.prafta.web.user.user05.result.DailyContractSignHisResult;
import com.prafta.web.user.user05.result.DailyEntryHisResult;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 계약이력 팝업 응답 (GET /webApi/user05/daily-contract-history).
 *
 * <p>signList = 계약서 서명 이력(서명본 열람은 기존 /webApi/user08/contract-sign-image 스트림 재사용),
 * entryList = 입장 승인요청/로그인 이력(계약서 미등록 로그인 판별 포함).
 */
@Value
@Builder
public class DailyContractHistoryResponse {
    List<DailyContractSignHisResult> signList;
    List<DailyEntryHisResult> entryList;
}
