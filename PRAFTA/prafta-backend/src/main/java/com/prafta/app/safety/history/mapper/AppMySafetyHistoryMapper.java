package com.prafta.app.safety.history.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.safety.history.application.query.MyHistoryQuery;
import com.prafta.app.safety.history.result.InspectionHistoryResult;
import com.prafta.app.safety.history.result.RiskHistoryResult;

/**
 * 내 안전활동 이력 매퍼 (prafta-app-025 J1-10 B-6).
 *
 * <p>본인 이력만 반환한다: 점검 INSERT_NO=userCd / 위험성 INIT_ASSESSOR_ID=userCd (IDOR).
 *    사진 경로는 FNC_CMM_INFO_SRCH('FILE_PATH') 로 해석(평문 FILE_PATH 직노출 비상속).
 *    페이징은 서비스에서 병합 후 슬라이스하므로 매퍼는 도메인별 전건을 시간 역순으로 반환한다.
 */
@Mapper
public interface AppMySafetyHistoryMapper {

    /** 본인 순회점검 이력(항목 평면, WORK_DATE DESC). */
    List<InspectionHistoryResult> selectMyInspectionHistory(MyHistoryQuery query);

    /** 본인 위험성평가 이력(INIT_ASSESS_DATE DESC). */
    List<RiskHistoryResult> selectMyRiskHistory(MyHistoryQuery query);
}
