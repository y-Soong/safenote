package com.prafta.web.baim.baim05.result;

public record DailyUserSlotListResult(
	String chk
	, String cmpnyCd
	, String siteCd
	, String siteNm
	, String slotNo
	, String slotType
	, String slotTypeNm
	, String fixedYn
	, String expired
	, String useYn
	, String currUserId
	, String currUserNm
	, String mblNo
	, String slotStatus
	, String slotStatusNm
	, String nodeCd
	, String nodeNm
	, String defaultSchCd	// 슬롯 기본 근무타입(점유 시 TB_USER.DEFAULT_SCH_CD 로 복사). null=근로자 본인 선택
){

}
