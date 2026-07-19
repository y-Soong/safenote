package com.prafta.web.chkLst.chkLst01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.chkLst.chkLst01.application.command.ChkptInfoCommand;
import com.prafta.web.chkLst.chkLst01.application.query.ChkptListQuery;
import com.prafta.web.chkLst.chkLst01.result.ChkptResult;
import com.prafta.web.chkLst.chkLst01.result.ChkptRowRaw;

@Mapper
public interface ChkLst01Mapper {
	List<ChkptResult> selectChkptList(ChkptListQuery query);

	void mergeChkptList(ChkptInfoCommand command);

	void updateChkptList(ChkptInfoCommand command);

	/** PRAFTA-SUBCON-T6-03: 점검대상 단건 원시행(미러 잠금 diff 판정용). 없으면 null. */
	ChkptRowRaw selectChkptRow(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("siteCd") String siteCd,
			@Param("chkLstType") String chkLstType, @Param("chkptCd") String chkptCd);

	/** PRAFTA-SUBCON-T6-04: 신규 점검대상코드 선채번(전파 대상 식별을 위해 저장 이전에 코드 확정). */
	String selectNextChkptCd(@Param("gvCmpnyCd") String gvCmpnyCd);

	/**
	 * PRAFTA-SUBCON-T6-03: 미러 점검대상의 담당자 단독 지정(잠금 예외).
	 * mergeChkptList(전체 UPSERT) 를 태우면 전파값(명칭/비고/사용여부)이 사용자 입력으로 오염되므로 전용 UPDATE 를 쓴다.
	 */
	void updateChkptMgmtUser(ChkptInfoCommand command);

	/** [보안검토 High-2] 호출자의 사업장 인가 여부(TB_USER_SITE_AUTH). 0이면 권한 없음. */
	int countUserSiteAuth(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("gvUserCd") String gvUserCd,
			@Param("siteCd") String siteCd);

	/** [보안검토 High-2] 메뉴/버튼 권한 보유 여부(TB_SYST_AUTH_MENU · ChkLst_01 의 SAVE/DELT). 0이면 권한 없음. */
	int selectMenuButtonAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd,
			@Param("menuDId") String menuDId, @Param("btnType") String btnType);
}
