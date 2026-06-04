\##  법정 연차 부여 정책 관리 신규 화면 개발



1. baim/Baim\_07.vue에 신규화면으로 개발
2. .claude/ref/prafta-017/ 경로 하위에 참고 화면과 참고할 수 있는 작업 요청서가 들어있음(CLAUDE\_CODE\_INSTRUCTIONS.md)

   1. Baim\_07.vue와 관련있는 화면 (신규화면)

      1. 03-leave-policy.html			(메인 화면)
      2. 04-leave-policy-custom.html		(연차 정책 직접 설정)
      3. 08-policy-change-impact.html	(정책 변경 영향 분석)
   2. User\_01 > UserInfoPop.vue 수정 관련 화면 (기존화면 수정)

      1. 01-user-info-popup.html
      2. 02-hire-date-edit-modal.html
   3. Attd\_09.vue와 관련있는 화면 (신규화면)

      1. 05-leave-dashboard.html		(메인 화면)
      2. 06-employee-leave-detail.html		 (사용자 연차 상세)
      3. 07-manual-grant-modal.html		(연차 수동부여)



\# CLAUDE\_CODE\_INSTRUCTIONS.md 에 테이블이랑 정책들을 넣어놓긴 했는데 prafta 프로젝트에 이해도가 떨어지는 AI와 작업한거라 100% 신용할 수 없으니 2번 항목에 나열한 화면들을 기준으로 판단해줘

\# 현재 prafta의 구조와 맞지않는 부분이 있다면 CLAUDE\_CODE\_INSTRUCTIONS.md를 그대로 따르지말고 반드시 질의응답을 통해 방향성을 결정하고 진행해줘

\# 연차와 관련해서 스케줄러 배치가 필요한데 일단은 프로젝트 내에 적당한 경로를 만들어서 연차 부여에 필요한 배치역할을 대신하게끔 개발해줘 (가장 마지막, 추후 배치는 별도의 프로젝트로 분리시킨뒤 거기서 돌릴 예정)



\## 추가 설명이 필요하거나 모호한 부분이 있을 경우는 채팅으로 질문해줘

