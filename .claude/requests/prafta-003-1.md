## /src/views/attd/popup/AttdDayDetailPop.vue 화면 수정요청사항

# prafta-003.txt 요청 보완요청건

1. prafta-003 요청건 작업 이후 초과근무 저장 시 아래같은 오류 발생
	- 2026-05-14T19:41:27.239+09:00  WARN 27716 --- [io-8080-exec-10] .m.m.a.ExceptionHandlerExceptionResolver : Resolved [com.prafta.common.exception.ApiException: Overtime exceeds allowed window outside of schedule.]

	- 상기 확인된 오류 외에도 이상이 있는지 추가 검토 요청

2. attd07ServiceImpl내에 생성된 비즈니스 로직 중 util 성격의 코드들은 공통으로 사용할 수 있을지를 판단하여 com.prafta.common.util 하위로 코드 이관
	- 해당 경로 내에 존재하는 클래스에서 옮기고자 하는 코드와 성격이 유사한 클래스가 존재하지 않을 시 신규 클래스를 생성
	- 신규로 생성하는 클래스는 추후 범용적으로 사용할 클래스 이기 때문에 추후 확정성과 코드 분리성을 종합적으로 고려하여 생성 (Ex) 근태관련 비즈니스 로직에서만 쓸거같은데 ? 그러면 AttdUtils 이런식으로)