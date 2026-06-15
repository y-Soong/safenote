package com.prafta.app.safety.history.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.safety.history.application.param.MySafetyHistoryParam;
import com.prafta.app.safety.history.application.query.MyHistoryQuery;
import com.prafta.app.safety.history.dto.response.MySafetyHistoryItem;
import com.prafta.app.safety.history.dto.response.MySafetyHistoryResponse;
import com.prafta.app.safety.history.mapper.AppMySafetyHistoryMapper;
import com.prafta.app.safety.history.result.InspectionHistoryResult;
import com.prafta.app.safety.history.result.RiskHistoryResult;
import com.prafta.app.safety.history.service.AppMySafetyHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 내 안전활동 이력 서비스 구현 (prafta-app-025 J1-10 B-6).
 *
 * <p>본인 이력(점검 INSERT_NO=userCd / 위험성 INIT_ASSESSOR_ID=userCd)을 도메인별로 조회한 뒤
 *    표시 계약(MySafetyHistoryItem)으로 변환하고 occurredDate DESC 로 병합한다(IDOR — 식별자 JWT 전용).
 *    페이징은 메모리 슬라이스(J1-5 패턴): 합본 정렬 후 page/pageSize 로 잘라 hasMore 산출.
 *
 * <p>kind=INSPECT/RISK 면 해당 도메인만 조회(불필요 쿼리 절약). ALL 이면 둘 다 조회 후 병합.
 *    TBM 이수 이력은 이번 범위 제외(확장 여지 — 지시서 §6 미결 1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppMySafetyHistoryServiceImpl implements AppMySafetyHistoryService {

    /** 메모리 슬라이스 과다 오프셋 가드(딥 페이징 방지 — J1-5 동형). */
    private static final int MAX_OFFSET = 5000;

    private final AppMySafetyHistoryMapper mapper;

    @Override
    public MySafetyHistoryResponse selectMyHistory(MySafetyHistoryParam param) {
        // 사업장 미지정(토큰 gv_siteCd 공백)이면 누수 방지로 빈 결과 반환(본인 사업장 스코프 필수).
        if (!StringUtils.hasText(param.gvSiteCd())) {
            log.info("내 안전활동 이력 - 토큰 사업장 미지정으로 빈 결과 - userCd={}", param.gvUserCd());
            return MySafetyHistoryResponse.builder().items(Collections.emptyList()).hasMore(false).build();
        }

        MyHistoryQuery query = new MyHistoryQuery(param.gvCmpnyCd(), param.gvSiteCd(), param.gvUserCd());

        List<MySafetyHistoryItem> merged = new ArrayList<>();

        boolean includeInspect = !MySafetyHistoryParam.KIND_RISK.equals(param.kind());
        boolean includeRisk = !MySafetyHistoryParam.KIND_INSPECT.equals(param.kind());

        int inspectCnt = 0;
        int riskCnt = 0;

        if (includeInspect) {
            List<InspectionHistoryResult> inspections = nullSafe(mapper.selectMyInspectionHistory(query));
            inspectCnt = inspections.size();
            for (InspectionHistoryResult r : inspections) {
                merged.add(toInspectionItem(r));
            }
        }
        if (includeRisk) {
            List<RiskHistoryResult> risks = nullSafe(mapper.selectMyRiskHistory(query));
            riskCnt = risks.size();
            for (RiskHistoryResult r : risks) {
                merged.add(toRiskItem(r));
            }
        }

        // occurredDate DESC 정렬(null 은 뒤로). 동률은 List 추가 순서를 보존(점검 → 위험성, 안정 정렬).
        merged.sort(Comparator.comparing(
                MySafetyHistoryItem::getOccurredDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int total = merged.size();
        int from = param.page() * param.pageSize();
        // 과다 오프셋/범위 밖이면 빈 페이지.
        if (from < 0 || from > MAX_OFFSET || from >= total) {
            log.info("내 안전활동 이력 - userCd={}, kind={}, 점검={}, 위험성={}, total={}, page={} → 빈 페이지",
                    param.gvUserCd(), param.kind(), inspectCnt, riskCnt, total, param.page());
            return MySafetyHistoryResponse.builder().items(Collections.emptyList()).hasMore(false).build();
        }
        int to = Math.min(from + param.pageSize(), total);
        List<MySafetyHistoryItem> pageItems = new ArrayList<>(merged.subList(from, to));
        boolean hasMore = to < total;

        log.info("내 안전활동 이력 - userCd={}, kind={}, 점검={}, 위험성={}, total={}, page={}, 반환={}건, hasMore={}",
                param.gvUserCd(), param.kind(), inspectCnt, riskCnt, total, param.page(), pageItems.size(), hasMore);

        return MySafetyHistoryResponse.builder().items(pageItems).hasMore(hasMore).build();
    }

    // ============================ 표시 변환 ============================

    /** 점검 행 → 합본 표시 행(title=항목명(폴백 체크포인트명), subText=결과 + 비고). */
    private MySafetyHistoryItem toInspectionItem(InspectionHistoryResult r) {
        String title = StringUtils.hasText(r.inspectItemSubj()) ? r.inspectItemSubj() : r.chkptNm();

        StringBuilder sub = new StringBuilder();
        sub.append("결과 ").append("O".equals(r.inspectAnswerType()) ? "양호" : "불량");
        if (StringUtils.hasText(r.answerDesc())) {
            sub.append(" · ").append(r.answerDesc());
        }

        // 안정 식별 키(체크포인트 + 항목 + 일자).
        String key = "INSPECT:" + r.chkptCd() + ":" + r.inspectItemCd() + ":" + r.workDate();

        return MySafetyHistoryItem.builder()
                .type("INSPECT")
                .key(key)
                .title(title)
                .subText(sub.toString())
                .displayDate(r.occurredDate())
                .occurredDate(r.occurredDate())
                .filePath(r.filePath())
                .build();
    }

    /** 위험성 행 → 합본 표시 행(title=위험요인(폴백 공정명), subText=상태명 + 위험성 Lv). */
    private MySafetyHistoryItem toRiskItem(RiskHistoryResult r) {
        String title = StringUtils.hasText(r.hazardNm()) ? r.hazardNm() : r.processNm();

        StringBuilder sub = new StringBuilder();
        if (StringUtils.hasText(r.assessmentStatusNm())) {
            sub.append(r.assessmentStatusNm());
        }
        if (StringUtils.hasText(r.initRiskLv())) {
            if (sub.length() > 0) {
                sub.append(" · ");
            }
            sub.append("위험성 ").append(r.initRiskLv());
        }

        String key = "RISK:" + r.processCd() + ":" + r.assessmentCd();

        return MySafetyHistoryItem.builder()
                .type("RISK")
                .key(key)
                .title(title)
                .subText(sub.toString())
                .displayDate(r.occurredDate())
                .occurredDate(r.occurredDate())
                .filePath(r.initFilePath())
                .build();
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
