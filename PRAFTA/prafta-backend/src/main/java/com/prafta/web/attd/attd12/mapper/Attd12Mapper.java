package com.prafta.web.attd.attd12.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd12.application.query.FraudAttdSuspectQuery;
import com.prafta.web.attd.attd12.result.FraudAttdRowResult;
import com.prafta.web.attd.attd12.result.UserDeviceBaselineResult;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 탐지 매퍼(on-view 대조, 읽기 전용).
 *
 * <p>규칙1(한 기기 → 같은 날 다계정)/규칙2(평소 기기와 다름)/규칙3(신규 기기) 판정은
 *   SQL 그룹핑/baseline 대조가 섞여 복잡하므로(Attd_11 가 화면단 재판정을 택한 것과 동일 이유)
 *   스코프 내 원시 행 + 사용자 baseline 디바이스 집합을 조회한 뒤 service 에서 판정/그룹핑한다.
 *
 * <p>스코프(node_tree 하위부서 RECURSIVE + target_user)는 Attd11Mapper 패턴 재사용.
 *   cross-site IDOR: 쿼리 자체가 gvCmpnyCd + siteCd 로 제한되고, siteCd 는 Param 에서
 *   세션 고정 사업장과 일치 검증을 통과한 값이다(서버 강제).
 */
@Mapper
public interface Attd12Mapper {

    /** 스코프(사업장/부서) 내 해당 월의 출퇴근 행 + 디바이스UUID(출/퇴근). */
    List<FraudAttdRowResult> selectScopedAttdRows(FraudAttdSuspectQuery query);

    /** 스코프 내 사용자들의 baseline 디바이스(로그인 이력에서 관측된 distinct (USER_CD, DEVICE_UUID)). */
    List<UserDeviceBaselineResult> selectUserDeviceBaseline(FraudAttdSuspectQuery query);
}
