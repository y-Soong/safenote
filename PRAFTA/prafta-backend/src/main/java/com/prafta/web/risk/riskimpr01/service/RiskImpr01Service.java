package com.prafta.web.risk.riskimpr01.service;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.web.risk.riskimpr01.application.param.ImprovementCompleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemDeleteParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemListParam;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemSaveParam;
import com.prafta.web.risk.riskimpr01.dto.response.ImprovementItemListResponse;

public interface RiskImpr01Service {

    // 개선항목 목록 조회 (평가키 스코프)
    ImprovementItemListResponse selectImprovementItems(ImprovementItemListParam param);

    // 개선항목 upsert (사진 포함). 003/004 차단, 신규/수정 분기
    void saveImprovementItem(ImprovementItemSaveParam param, MultipartFile file);

    // 개선항목 삭제 (soft delete). 003/004 차단
    void deleteImprovementItem(ImprovementItemDeleteParam param);

    // 개선완료 (005→003 전이 + REVAL_* 동기화 + 개선 후 위험도 1-3 가드)
    void completeImprovement(ImprovementCompleteParam param);
}
