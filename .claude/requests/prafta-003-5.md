## /src/views/attd/popup/AttdDayDetailPop.vue 화면 수정요청사항

# prafta-003.md의 보완 요청사항의 건

1. AttdDayDetailPop.vue의 fnSearch 의 조회 결과에 초과근무가 포함되지 않음

2. 화면에서 해당 초과근무 데이터를 처리할 구조는 vsCode와 연결된 CLAUDE CODE가 만들어 놓긴 했으나 실제 DB 컬럼과 컬럼명이 상이하기 때문에 vue 화면쪽의 변수명을 DB 컬럼과 맞춰줘야 함 

3. com.prafta.common, com.prafta.web에서 주석이나 error 메시지등에 한글이 깨져있는 경우가 있는데 UTF-8기준으로 복호화좀 해줘 아마 기존에 x-windows-949 로 인코딩 되어있을거야 