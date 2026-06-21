package com.prafta.web.attd.attd12.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd12.application.query.FraudAttdSuspectQuery;
import com.prafta.web.attd.attd12.result.SharedDeviceLoginResult;

/**
 * prafta-com-016-F 9-1 - 부정 출퇴근(기기 공유) 의심 탐지 매퍼(읽기 전용).
 *
 * <p>판정 데이터 출처는 출퇴근 시 도장되는 CHECK_IN_DEVICE_UUID(대부분 NULL)가 아니라
 *   로그인마다 DEVICE_UUID(NOT NULL)가 적재되는 tb_user_device_login_hist 다.
 *   "한 사용자가 APP 로그인한 기기를 그 로그인 직전 7일 내에 다른 사용자가 APP 로그인한 적이 있으면
 *   부정사용(기기 공유/대리) 의심" 으로 본다.
 *
 * <p>login_hist 에는 SITE_CD/NODE_CD 가 없으므로 스코프/권한은 USER_CD → TB_USER 조인으로 도출한다.
 *   node_tree(하위부서 RECURSIVE) + target_user(USE_YN='Y', 미퇴사) 스코프는 Attd11Mapper 패턴 재사용.
 *   cross-site IDOR: gvCmpnyCd + siteCd(세션 고정 검증 통과값)로 제한.
 */
@Mapper
public interface Attd12Mapper {

    /**
     * 공유 기기 의심 로그인 행 조회.
     *
     * <p>조회월(LOGIN_DTIME 의 YYYYMM)에 스코프 내 사용자가 APP 로그인한 기기 중,
     *   같은 기기를 그 로그인 직전 7일 내에 다른 USER_CD 가 APP 로그인한 적이 있는 "공유 의심 기기"를
     *   먼저 식별하고, 그 기기에 관여한 로그인 이력(사용자·시각·메타)을 평탄 행으로 반환한다.
     *   서비스에서 DEVICE_UUID 로 그룹핑해 기기 중심(1 기기 → N 사용자) 표시 모델로 조립한다.
     */
    List<SharedDeviceLoginResult> selectSharedDeviceLoginSuspects(FraudAttdSuspectQuery query);
}
