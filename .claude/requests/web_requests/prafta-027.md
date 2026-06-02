\## 초과근무 승인 및 반려에 대한 전체 플로우 검토 요청(Attd\_10.vue, Attd\_07.vue > AttdDayDetailPop.vue)



\# test-scenarios-leave-attd.txt 파일을 기준으로 테스트 중 초과근무에 대한 부분을 확인하다 보니 오동작중인 로직이 많아서 이에 대한 확인 및 수정요청

1. Attd\_07.vue > AttdDayDetailPop.vue

   1. 초과근무 요청 데이터 생성 후 Attd\_07.vue와 연결되어 있는 AttdDayDetailPop.vue 에서 승인 시 "요청을 처리할 수 없습니다." 라는 메시지가 나옴(Attd\_10.vue에선 승인가능)
   2. 초과근무 요청 데이터 생성 후 Attd\_07.vue와 연결되어 있는 AttdDayDetailPop.vue 에서 반려 시 처리이력이 안남음
2. Attd\_10.vue

   1. 초과근무 요청 데이터 생성 후 Attd\_10.vue 에서 승인 시 처리이력이 안남음
   2. 초과근무 요청 데이터 생성 후 Attd\_10.vue 에서 반려 시 처리이력이 안남음



\## 추가 질문사항이 있을경우 질의를 통해서 명령어 작성을 완성해줘

