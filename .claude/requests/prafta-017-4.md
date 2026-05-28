## prafta-017과 연계된 요청, 사용자정보 화면(UserInfoPop.vue) 수정 및 신규 화면 개발

1. .claude\requests\ref\prafta-017 경로의 하기 화면들 참고
	- 01-user-info-popup.html (사용자 화면 수정 모델)
	- 02-hire-date-edit-modal.html (사용자 화면 연계 화면)

2. UserInfoPop.vue를 수정하여 01-user-info-popup.html 개발
	
3. CLAUDE_CODE_INSTRUCTIONS의 화면 1, 2 부분 참고
	- UserInfoPop.vue 화면 수정 후 근태/연차 정보 하위에 입사일, 경력인정 항목 추가 등의 기능은 'master', 'hr' 권한을 갖고있는 사용자만 가능하도록 조건 추가필요
	- UserInfoPop.vue 화면의 기존 기능은 유지해야 함 (해당 팝업을 열었을때, 해당 팝업을 연 사용자의 권한을 기준으로 팝업에서 담고있는 사용자의 권한을 설정할 수 있는 조건 등)
	- 01-user-info-popup.html 에 있는 "신규 추가 영역", "M&A, 경력직, 그룹사 이동 등"의 텍스트는 삭제
	- 신규 팝업인 02-hire-date-edit-modal.html 에서 변경할 입사일의 초기값은 기존 입사일과 동일하게 처리

## CLAUDE_CODE_INSTRUCTIONS 는 prafta의 DB 및 기존 개발 로직을 모르는 AI와 설계한 요청서이기 때문에 100% 신뢰하면 안됨
## prafta 기존 개발된걸 기준으로 CLAUDE_CODE_INSTRUCTIONS는 참고만하기
## 화면 기준으로 판단하여 개발진행 요청
## 애매한 부분이 있다면 질의 응답을 통해 개발 방향성 확정