// leaveFeature.js — 연차 기능 노출 판정(소정-06) 소비 헬퍼.
//   작업 ID: 소정-12 (UI-E, plan §3 / §4)
//
//   서버 판정: GET /comApi/leave-feature/visibility
//     응답 { leaveFeatureVisible:boolean, statutoryAutoGrantYn:'Y'|'N', grantHistoryExists:boolean }
//     숨김 조건 = 회사 "법정 연차 자동 부여" 토글 off  AND  회사 연차 부여 이력 0건
//     (5인 미만 사업장 등 연차유급휴가 적용 제외 회사 — 지시서 §연차 부여 on/off 토글)
//
//   ★폴백 규칙: 판정 실패(네트워크/서버 오류)·미판정 상태는 전부 "보이는 쪽"이다.
//     연차 기능이 조용히 사라지는 편보다 잘못 보이는 편이 안전하다(사용자 지시).
//     서버가 명시적으로 leaveFeatureVisible=false 를 준 경우에만 숨긴다.
//
//   상태 보관: 모듈 ref(화면 반응성) + sessionStorage 캐시(웹뷰 새로고침 시 깜빡임/중복 호출 방지).
//     일용직(DAILY)은 기존 게이트가 이미 연차 화면을 전부 숨기므로 라운드트립 자체를 생략한다.
import { computed, ref } from 'vue'

import api from '@/api/axios'
import { isDailyWorker } from '@/utils/employment'

/** 세션 캐시 키. 'Y'=노출 / 'N'=숨김. 키 자체가 없으면 "미판정"(=노출 폴백). */
const SESSION_KEY = 'gv_leaveFeatureVisible'

/** 현재 판정값(폴백 기본 = 노출). */
const visibleState = ref(sessionStorage.getItem(SESSION_KEY) !== 'N')

/** 동시 호출 중복 방지(단일 플라이트). */
let inFlight = null

/** 화면에서 v-if 로 쓰는 반응형 판정값. */
export const leaveFeatureVisible = computed(() => visibleState.value)

/** 비반응형 즉시 판정(스크립트 분기용). */
export function isLeaveFeatureVisible() {
  return visibleState.value
}

/** 판정 캐시 초기화(로그아웃/계정 전환 대비). 초기화 후 값은 폴백(노출)이다. */
export function clearLeaveFeatureVisibility() {
  sessionStorage.removeItem(SESSION_KEY)
  visibleState.value = true
}

/**
 * 연차 기능 노출 판정 조회(서버 라운드트립). 실패는 비차단 — 노출 폴백으로 수렴한다.
 * @returns {Promise<boolean>} 노출 여부
 */
export async function loadLeaveFeatureVisibility() {
  // 일용직은 연차 개념 자체가 없어 기존 isDailyWorker 게이트가 모든 진입점을 이미 숨긴다 → 호출 생략.
  if (isDailyWorker()) return visibleState.value

  if (inFlight) return inFlight

  const request = (async () => {
    try {
      const { data } = await api.get('/comApi/leave-feature/visibility')
      // 명시적 false 만 숨김. 필드 누락(구버전 서버)·비boolean 은 노출 폴백.
      const visible = data?.leaveFeatureVisible === false ? false : true
      visibleState.value = visible
      sessionStorage.setItem(SESSION_KEY, visible ? 'Y' : 'N')
      return visible
    } catch (e) {
      // 판정 실패 → 노출 폴백(캐시도 남기지 않아 다음 진입에서 재시도).
      console.warn('[leaveFeature] 연차 노출 판정 조회 실패(노출 폴백):', e?.message)
      sessionStorage.removeItem(SESSION_KEY)
      visibleState.value = true
      return true
    } finally {
      inFlight = null
    }
  })()

  inFlight = request
  return request
}

/**
 * 판정값 확보(캐시 우선). 화면 onMounted 에서 부담 없이 호출할 수 있다.
 *   - 캐시 있음 → 즉시 반환(라운드트립 없음)
 *   - 캐시 없음 → 폴백(노출)로 되돌린 뒤 서버 조회
 * @returns {Promise<boolean>} 노출 여부
 */
export async function ensureLeaveFeatureVisibility() {
  const cached = sessionStorage.getItem(SESSION_KEY)
  if (cached === 'Y' || cached === 'N') {
    visibleState.value = cached !== 'N'
    return visibleState.value
  }
  // 세션이 초기화된 직후(재로그인/웹뷰 재적재)에는 이전 계정 판정이 남지 않도록 폴백부터 복원한다.
  visibleState.value = true
  return loadLeaveFeatureVisibility()
}
