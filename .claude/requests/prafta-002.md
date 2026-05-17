## /src/views/attd/popup/AttdDayDetailPop.vue 화면 기준 수정요청사항

백엔드 
	- com.prafta.web.attd.attd07에 신규 API 개발
	- post 통신
	- url : update-user-attd-requests
	
프론트 엔드 
	- AttdDayDetailPop.vue 화면의 fnApproveReq 함수 동작 방식 변경 필요

	1. fnApproveReq 함수가 호출될 때 confirm 메시지 이후 API 통신을 시도해야 한다.
	2. TB_USER_ATTD_MGMT 테이블에 해당 근태정보를 찾아서 요청된 값으로 수정처리 해줘야한다.
	3. TB_USER_ATTD_REQ 테이블에 해당 근태 수정요청 정보를 찾아서 상태값을 변경해 줘야한다. (REQ_STATUS)
	4. 플로우 외에 다른 코드는 건드리면 안되고 모든 단계가 종료된 이후 전체 기능점검 필요