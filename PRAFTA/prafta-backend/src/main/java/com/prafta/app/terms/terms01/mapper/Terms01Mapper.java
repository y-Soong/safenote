package com.prafta.app.terms.terms01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.terms.terms01.mapper.result.PendingTermsResult;

/**
 * 앱 약관(Terms) Mapper.
 *
 * <p>스키마 변경 없음. TB_TERMS / TB_TERMS_USER_AGR_MGMT / TB_SYST_VAL_D(SYS008) 만 사용한다.
 *    USER_CD 는 항상 호출 측(서비스)이 JWT 로 확정한 값을 바인딩한다(IDOR 차단).
 *
 * <p>★ SUBCON-T4: 동의 upsert 는 본 매퍼에서 제거되고 ConsentMapper.upsertTermsAgr 로 일원화되었다.
 *    모든 동의 변경은 ConsentHistoryRecorder 를 경유해야 한다(이력 우회 경로 차단).
 */
@Mapper
public interface Terms01Mapper {

    /** 미동의 필수약관(REQUIRED_YN='Y' AND USE_YN='Y') 목록 — 사용자 현재버전 미동의분만(회사 스코프). */
    List<PendingTermsResult> selectPendingRequiredTerms(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    // ★ 선택약관 목록/현재버전 resolve 는 ConsentMapper(common/cmm/consent)로 이관되었다.
    //   웹 내 정보 팝업이 같은 구문을 공유해야 하므로 복사본을 두지 않는다(Terms01Mapper.xml 주석 참조).
}
