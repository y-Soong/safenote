// employment.js — 고용형태(EMPLOYMENT_TYPE[SYS041]) 기반 화면 분기 헬퍼.
//   작업 ID: prafta-app-025 J1-4
//   로그인 응답의 employmentType 을 LoginView 가 sessionStorage('gv_employmentType') 에 저장하고,
//   각 화면은 라운드트립 없이 본 헬퍼로 일용직 여부를 판정한다.
//   - 일용직(DAILY)은 스케줄/근무수정요청/초과근무요청 해당없음 → 근태조회 카드/탭/요청 진입점을 숨긴다.

const SESSION_KEY = 'gv_employmentType'

// 현재 로그인 사용자의 고용형태 코드 반환(없으면 빈 문자열).
export function getEmploymentType() {
  return sessionStorage.getItem(SESSION_KEY) || ''
}

// 일용직(DAILY) 여부. 그 외(REGULAR/CONTRACT/EXECUTIVE/null/빈값)는 false.
export function isDailyWorker() {
  return getEmploymentType() === 'DAILY'
}
