package com.prafta.web.baim.baim05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim05.application.command.ClearSlotCommand;
import com.prafta.web.baim.baim05.application.command.CloseSlotHisCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotUpdCommand;
import com.prafta.web.baim.baim05.application.command.InsertDailyQrUserCommand;
import com.prafta.web.baim.baim05.application.command.InsertSlotHisCommand;
import com.prafta.web.baim.baim05.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotFixedCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotNodeCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotSchCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotTypeCommand;
import com.prafta.web.baim.baim05.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.application.query.SlotHisQuery;
import com.prafta.web.baim.baim05.application.query.UserSlotCountQuery;
import com.prafta.web.baim.baim05.result.DailyUserLinkPolicyResult;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;
import com.prafta.web.baim.baim05.result.SlotHisResult;

@Mapper
public interface Baim05Mapper {
	DailyUserLinkPolicyResult selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery query);
	
	List<DailyUserSlotListResult> selectDailyUserSlotList(DailyUserSlotListQuery query);
	
	void saveDailyUserLinkPolicy(LinkPoliciesCommand command);
	
	int selectDailyUserSlotCnt(UserSlotCountQuery query);

	/** 사업장 최상단(루트) 노드 NODE_CD 조회(PARENT_NODE_CD IS NULL). 신규 슬롯 기본 소속부서값. 미존재면 null. */
	String selectSiteTopNodeCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

	/** 활성 계정 수 축소 가드: 점유 중(CURR_USER_CD 존재)인데 새 한도 인덱스(SLOT_NO >= dayLimitCnt) 밖 슬롯 수. >0이면 축소 차단. */
	int selectOccupiedSlotCntOverLimit(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("dayLimitCnt") int dayLimitCnt);

	void saveDailyUserSlot(DailyUserSlotCommand command);
	
	String selectDailyUserCd(@Param("cmpnyCd") String cmpnyCd);
	
	int selectDailyUserDuplicateCnt(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

	// ===== PRAFTA_COM_001 T1-07: 계정 등록(USE_YN) 토글 서버 게이트 =====

	/** 사업장 계정등록 정책 USE_YN 조회(미존재면 null). OFF면 QR 발급 차단. */
	String selectLinkPolicyUseYn(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

	// ===== PRAFTA_COM_001 T1-04a: QR 발급 통합형 중복 사전검증 =====

	/** TB_USER 사용자ID 중복 카운트(활성 USE_YN='Y'). 1 이상이면 중복. */
	int selectTbUserIdDupleCnt(@Param("cmpnyCd") String cmpnyCd, @Param("userId") String userId);

	/** TB_USER 휴대폰 HMAC 중복 카운트(활성 USE_YN='Y'). 1 이상이면 중복. */
	int selectTbUserMblHmacDupleCnt(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

	void insertDailyQrUser(InsertDailyQrUserCommand command);

	// ===== PRAFTA_COM_001 T1-04a(재작업): 비활성 일용직 재활성(dailyjoin 옵션2 미러) =====

	/** 재활성 대상 USER_CD 조회(같은 회사+휴대폰 비활성 TB_DAILY_USER 최신 1건). 없으면 null. */
	String selectReactivatableDailyUserCd(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);

	/** 재활성 — TB_DAILY_USER 기존 행 재사용(만료일/잠금 초기화 포함). */
	void reactivateDailyQrUser(InsertDailyQrUserCommand command);

	/** 재활성 — TB_USER 기존 통합행 재사용(EMPLOYMENT_TYPE='DAILY' 가드). 영향행 0이면 신규 INSERT 폴백. */
	int reactivateDailyQrUserTbUser(InsertDailyQrUserCommand command);

	/** 재활성 — TB_USER_SITE_AUTH upsert(사업장 권한 행 활성). */
	void upsertDailyQrUserTbUserSiteAuth(InsertDailyQrUserCommand command);

	// ===== PRAFTA_COM_001 T1-04a: QR 발급 통합형 TB_USER + SITE_AUTH 동시 INSERT =====

	/** 통합형 — TB_USER INSERT(EMPLOYMENT_TYPE='DAILY'/AUTH_CD='99999'/NODE_CD=NULL). */
	void insertDailyQrUserToTbUser(InsertDailyQrUserCommand command);

	/** 통합형 — TB_USER_SITE_AUTH INSERT(해당 사업장 1행). */
	void insertDailyQrUserTbUserSiteAuth(InsertDailyQrUserCommand command);
	
	void updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand command);

	/** 사업장 접근 권한 확인(TB_USER_SITE_AUTH 매핑, USE_YN='Y'). 1 이상이면 접근 가능. */
	int countUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd);

	/** 슬롯 점유자(CURR_USER_CD) 재조회. 비점유 또는 미존재면 null. */
	String selectSlotCurrUserCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("slotNo") String slotNo);

	/** 슬롯 비우기: CURR_USER_CD=NULL, SLOT_STATUS='01', FIXED_YN='N'. */
	void updateClearSlot(ClearSlotCommand command);

	/** 비우기 - 점유자 TB_DAILY_USER 비활성(USE_YN='N', ACCOUNT_STATUS='05'). */
	void deactivateDailyUser(ClearSlotCommand command);

	/** 비우기 - 통합형 TB_USER 비활성(EMPLOYMENT_TYPE='DAILY' 가드). */
	void deactivateTbUser(ClearSlotCommand command);

	/** 점유 유지/해지: 점유 슬롯(SLOT_STATUS='02')의 FIXED_YN 토글. */
	void updateSlotFixed(SetSlotFixedCommand command);

	/** 슬롯 구분(SLOT_TYPE) 변경: 비점유 슬롯(CURR_USER_CD IS NULL AND SLOT_STATUS!='02')만 대상. */
	void updateSlotType(SetSlotTypeCommand command);

	// ===== PRAFTA-daily-user-dept-2: 슬롯 소속부서(NODE_CD) 지정/해제 =====

	/** 해당 사업장에 속한 유효 노드(부서) 카운트(cross-site 노드 변조 차단). 1 이상이면 유효. */
	int countSiteNode(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("nodeCd") String nodeCd);

	/** 슬롯 소속부서(NODE_CD) UPDATE. nodeCd 가 null 이면 부서 해제. 점유중 슬롯도 대상(잠금 없음). */
	void updateSlotNode(SetSlotNodeCommand command);

	// ===== PRAFTA-055-2: 역할 게이트(노드 관리자 보강) =====

	/** 해당 사업장의 노드(부서) 정/부 관리자 여부. 1 이상이면 관리자. */
	int countNodeAdminInSite(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("userCd") String userCd);

	// ===== PRAFTA-055-1: 슬롯 사용 이력(TB_DAILY_USER_SLOT_HIS) 적재 =====

	/** 이력ID 채번('H' + YYYYMMDD + 시퀀스). */
	String selectDailySlotHisId(@Param("cmpnyCd") String cmpnyCd);

	/** 점유 시작 INSERT(RELEASE_* = NULL). */
	void insertSlotHis(InsertSlotHisCommand command);

	/** 점유 해제 — 슬롯 PK 3키의 열린 행(RELEASE_DTIME IS NULL) 1건 UPDATE. */
	void closeSlotHis(CloseSlotHisCommand command);

	// ===== PRAFTA-055-3: 슬롯 사용 이력 조회(최근 30일) =====

	/** 슬롯 사용 이력 목록 조회(PII 마스킹은 SQL 처리). */
	List<SlotHisResult> selectDailyUserSlotHisList(SlotHisQuery query);

	// ===== PRAFTA-daily-user-dept-3: QR 발급 점유 시 슬롯 지정부서 → TB_USER.NODE_CD 매칭 =====

	/** 점유한 슬롯의 지정 부서(NODE_CD) 조회. 미존재/미지정이면 null. */
	String selectSlotNodeCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("slotNo") String slotNo);

	/** 슬롯 지정부서를 점유 일용직(EMPLOYMENT_TYPE='DAILY')의 TB_USER.NODE_CD 로 무조건 세팅. */
	void updateTbUserNodeCdFromSlot(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("nodeCd") String nodeCd, @Param("gvUserCd") String gvUserCd);

	// ===== baim05-slot-default-sch: 슬롯 기본 근무타입(DEFAULT_SCH_CD) 지정/해제 =====

	/** 슬롯 기본 근무타입 UPDATE. schCd 가 null 이면 해제(근로자 본인 선택 폴백). 점유중 슬롯도 대상. */
	void updateSlotDefaultSch(SetSlotSchCommand command);

	/** 점유한 슬롯의 기본 근무타입(DEFAULT_SCH_CD) 조회. 미존재/미지정이면 null. */
	String selectSlotDefaultSchCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("slotNo") String slotNo);

	// ===== baim05-qr-phone-precheck: 관리자 QR 발급 전 휴대폰 중복 사전확인 =====

	/** 재활성 대상(같은 회사+휴대폰 비활성 일용직 최신 1건)의 마스킹 이름. 없으면 null. */
	String selectReactivatableDailyUserNmMasked(@Param("cmpnyCd") String cmpnyCd, @Param("mblNoHmac") String mblNoHmac);
}
