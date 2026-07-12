package com.prafta.app.terms.terms01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.terms.terms01.application.command.TermsAgrUpsertCommand;
import com.prafta.app.terms.terms01.mapper.result.OptionalTermsResult;
import com.prafta.app.terms.terms01.mapper.result.PendingTermsResult;

/**
 * 앱 약관(Terms) Mapper.
 *
 * <p>스키마 변경 없음. TB_TERMS / TB_TERMS_USER_AGR_MGMT / TB_SYST_VAL_D(SYS008) 만 사용한다.
 *    USER_CD 는 항상 호출 측(서비스)이 JWT 로 확정한 값을 바인딩한다(IDOR 차단).
 */
@Mapper
public interface Terms01Mapper {

    /** 미동의 필수약관(REQUIRED_YN='Y' AND USE_YN='Y') 목록 — 사용자 현재버전 미동의분만(회사 스코프). */
    List<PendingTermsResult> selectPendingRequiredTerms(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /** 선택약관(REQUIRED_YN='N' AND USE_YN='Y') 목록 + 사용자 현재버전 동의여부(없으면 'N', 회사 스코프). */
    List<OptionalTermsResult> selectOptionalTerms(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * 특정 약관이 "선택약관(REQUIRED_YN='N' AND USE_YN='Y')" 이면 현재버전을 반환, 아니면 null.
     * 선택약관 토글 경로의 게이트 우회(필수약관/미사용약관 변조) 차단에 사용한다.
     */
    String selectOptionalTermsCurrentVersion(@Param("termsId") String termsId);

    /** 약관 동의 upsert(INSERT ... ON DUPLICATE KEY UPDATE AGR_YN). 영향행 반환. */
    int upsertTermsAgr(TermsAgrUpsertCommand command);
}
