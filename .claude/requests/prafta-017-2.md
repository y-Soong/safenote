## prafta-017과 연계된 요청, 연차현황 개발 요청의 건

1. .claude\requests\ref\prafta-017 경로의 하기 화면들 참고
	- 05-leave-dashboard.html (메인화면)
	- 06-employee-leave-detail.html (연계화면)
	- 07-manual-grant-modal.html(연계 팝업)

2. Baim_08.vue로 신규 화면 생성.
	
3. CLAUDE_CODE_INSTRUCTIONS의 화면 5, 6, 7 부분 참고
	- 연차사용계획서 조회 버튼의 경우는 아직 세부 개발 내용이 미확정인 관계로 버튼만 만들어놓으면 됨
	- 06-employee-leave-detail.html 화면의 "이력상세" 버튼은 "새로고침" 으로 명칭 변경 후 데이터를 다시 읽어오는 기능으로 변환

## CLAUDE_CODE_INSTRUCTIONS 는 prafta의 DB 및 기존 개발 로직을 모르는 AI와 설계한 요청서이기 때문에 100% 신뢰하면 안됨
## prafta 기존 개발된걸 기준으로 CLAUDE_CODE_INSTRUCTIONS는 참고만하기
## 화면 기준으로 판단하여 개발진행 요청
## 애매한 부분이 있다면 질의 응답을 통해 개발 방향성 확정