package com.prafta.web.user.user10.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user10.application.query.StdWorkUserListQuery;
import com.prafta.web.user.user10.result.StdWorkUserRowResult;
import com.prafta.web.user.user10.result.StdWorkUserScopeResult;

/**
 * 소정-10: 소정근로시간 관리(User_10) 매퍼.
 *
 * <p>이력 자체의 조회/등록/정정은 공용 서비스({@code StdWorkHoursService})가 단일 출처로 담당한다.
 * 본 매퍼는 <b>관리 화면 전용</b>인 ①대상 근로자 목록(유효 이력 조인) ②권한 게이트용 대상 스코프
 * 조회만 가진다 — 이력 쓰기 SQL 을 여기에 복제하지 않는다.
 */
@Mapper
public interface User10Mapper {

    /**
     * 소정근로시간 관리 대상 근로자 목록 (오늘 기준 유효 이력 LEFT JOIN).
     *
     * <p>이력이 없으면 소정 관련 컬럼이 null 로 나온다 — 서비스가 폴백값으로 덮지 않고
     * 화면이 "미입력" 배지를 그리게 한다.
     */
    List<StdWorkUserRowResult> selectStdWorkUserList(StdWorkUserListQuery query);

    /**
     * 대상 근로자의 서버 권위 스코프(사업장/부서/고용형태/이름) 1건.
     *
     * <p>이력 조회·등록·정정 EP 의 권한 게이트 입력이다. 요청 바디의 사업장/부서를 쓰면
     * 자기 부서 값을 실어 타 부서 근로자의 계약 정보를 열람·변경할 수 있다.
     *
     * @return 대상 행. 타 회사이거나 미존재면 null
     */
    StdWorkUserScopeResult selectUserScope(@Param("cmpnyCd") String cmpnyCd,
                                           @Param("userCd") String userCd);
}
