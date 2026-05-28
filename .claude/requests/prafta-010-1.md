\## prafta-010.md 파일의 보완과 신규 요청에 대한 건

AttdDayDetailPop.vue 화면 수정 요청의 건

1. AttdDayDetailPop.vue 화면에 추가해준 외근 버튼을 눌렀을 때 GPS 지도 정보가 나오는 기능에 대한 검토요청

   1. USER\_CD '20260400010' 값을 기준으로 9일, 14일 기준의 근태와 연결된 TB\_USER\_ATTD\_GPS 데이터가 있는데 외근 버튼을 누르면 필수 입력값 누락이라는 메시지가 나오고 있음
2. 사용자의 근태생성 요청과 관련된 내용

   1. 현재 사용자가 근태생성 요청을할 경우 ATTD\_ID가 없어서 문제라고 전 요청에 대한 피드백을 받았는데 아래와같이 수정해줬으면 좋겠어

      1. 사용자가 근태생성 요청 시 ATTD\_ID를 먼저 채번해서 TB\_USER\_ATTD\_REQ에 넣는다. > 이후 관리자가 해당 요청을 승인할 경우 TB\_USER\_ATTD\_REQ에 넣어뒀던 

&#x09;	ATTD\_ID를 TB\_USER\_ATTD\_MGMT에 넣는다.

&#x09;2. 관련해서 사용자의 근태생성 요청의 경우 이력이 먼저 한번 생기고 관리자가 승인 혹은 반려 함에 따라서 두번째 이력이 나와야 함

&#x09;1. 지금은 사용자의 근태생성 요청 시 이력이 안쌓이는 구조

3\. TB\_USER\_ATTD\_REQ, TB\_USER\_ATTD\_GPS에 지금 기존과 다른 ID 체계를 가져가고 있는데 원래 규칙대로 수정 YYYYMMDD + FNC\_CMM\_SEQ\_NEXTVAL 함수를 활용한 ID 규칙

&#x09;1. TB\_USER\_ATTD\_REQ 테이블의 경우 FNC\_CMM\_SEQ\_NEXTVAL(#{gvCmpnyCd}, 'ATTD\_REQ\_ID') 로 채번

&#x09;2. TB\_USER\_ATTD\_GPS 테이블의 경우 FNC\_CMM\_SEQ\_NEXTVAL(#{gvCmpnyCd}, 'ATTD\_GPS\_ID') 로 채번



\## 추가 설명이 필요하거나 모호한 부분이 있을 경우는 채팅으로 질문해줘

