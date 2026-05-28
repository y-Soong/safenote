\## TB\_USER\_ATTD\_REQ 테이블 데이터 접근 방식 수정 요청



1. REQ\_TYPE를 현재는 ATTD\_MODIFY/ATTD\_CREATE/OT\_REGISTER/LEAVE\_REQUEST 이런 타입으로 쓰고있는데 아래와 같이 수정해줘

   1. TB\_SYST\_VAL\_D 테이블의 SYST\_VAL\_CD 값 "SYS032"를 기준으로 상태값 관리
2. REQ\_STATUS를 현재는 REQUESTED/APPROVED/REJECTED/CANCELLED로 관리하고 있지만 아래와 같이 수정해줘

   1. TB\_SYST\_VAL\_D 테이블의 SYST\_VAL\_CD 값 "SYS033"을 기준으로 상태값 관리



\# 상기 요청에 따라 TB\_USER\_ATTD\_REQ 테이블 컬럼 comment도 수정

\# 관련해서 Attd\_07.vue, Attd\_08.vue, AttdDayDetailPop.vue 화면 수정 요청 (추가 다른 화면에서도 영향도가 있는지를 체크하여 함께 수정)





\## 추가 설명이 필요하거나 모호한 부분이 있을 경우는 채팅으로 질문해줘

