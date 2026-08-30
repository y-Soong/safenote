package com.prafta.web.user.user05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user05.application.query.DailyUserListQuery;
import com.prafta.web.user.user05.result.DailyContractSignHisResult;
import com.prafta.web.user.user05.result.DailyEntryHisResult;
import com.prafta.web.user.user05.result.DailyUserListRaw;

@Mapper
public interface User05Mapper {

    /** 사업장 접근 권한 확인(TB_USER_SITE_AUTH 매핑, USE_YN='Y'). 1 이상이면 접근 가능. */
    int countUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd);

    /** 일일사용자 슬롯 점유 이력 목록 조회(만료 포함, 이름 평문+휴대폰 암호문 원시 반환, 사업장 스코프 강제). */
    List<DailyUserListRaw> selectDailyUserList(DailyUserListQuery query);

    /** 대상 일용직의 사업장코드 조회(계약이력 조회 인가 판정용). 미존재 시 null. */
    String selectDailyUserSiteCd(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 계약서 서명 이력(사용자 단위, 최신순) — 계약서명(TB_DAILY_CONTRACT) 병기. */
    List<DailyContractSignHisResult> selectDailyContractSignHis(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 입장 승인요청/로그인 이력(사용자 단위, 최신순) — 연결 서명 존재 여부(signYn) 포함. */
    List<DailyEntryHisResult> selectDailyEntryHis(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);
}
