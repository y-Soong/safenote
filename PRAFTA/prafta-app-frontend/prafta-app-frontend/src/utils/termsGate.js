// termsGate.js — 앱 로그인 후 게이트 체인 라우팅 헬퍼.
//   체인: ① 필수약관 미동의 게이트(/TermsAgree, 미동의=로그아웃)
//        → ①-b 일용직 근로계약서 서명 게이트(/DailyContractSign, 미서명=로그아웃)  ← 일용직 계약서+승인제 T4
//        → ② 연동 회사 제3자 제공 동의 게이트(/ThirdPartyConsent, 동의/미동의 모두 통과)  ← PRAFTA-SUBCON-T4
//        → ③ 목적지(redirect || /MainView)
//
//   ★ 호출처 5곳(LoginView 정규/일용, PhoneAuthView, DefaultSchGateView, ForcedPasswordChangeView)은
//     모두 routeAfterLogin 하나로 수렴한다. 새 게이트를 호출처마다 추가하지 말 것(체인 누락 사고 전례).
//
//   가용성 우선: 게이트 조회 자체가 실패하면(네트워크/서버) 진입을 막지 않고 통과시킨다
//   (로그인은 이미 성공했고, 다음 진입 시 다시 게이트가 동작한다). 단 경고 로깅은 남긴다.
import api from '@/api/axios'
// 일용직(DAILY) 판정 — 세션값(gv_employmentType) 기반. 정규 사용자는 계약서 게이트 라운드트립 자체를 생략.
import { isDailyWorker } from '@/utils/employment'

/** 연동 회사 제3자 제공 동의 약관 ID(SYS008 '006'). 마이페이지 철회 확인 팝업 판별에도 사용. */
export const THIRD_PARTY_CONSENT_TERMS_ID = '006'

/**
 * 로그인 직후 라우팅 결정(전체 체인 ① → ② → ③).
 * @param {import('vue-router').Router} router  vue-router 인스턴스
 * @param {string} [redirect]  로그인 진입 시 보존된 redirect 경로(없으면 /MainView)
 */
export async function routeAfterLogin(router, redirect) {
  const fallback = redirect || '/MainView'

  try {
    const { data } = await api.get('/appApi/terms01/required-terms-pending')
    const pending = Array.isArray(data?.terms) ? data.terms : []

    if (pending.length > 0) {
      // ① 미동의 필수약관 존재 → 동의 화면으로. 목록은 history state 로 전달(새로고침 대비 화면이 재조회).
      //    TermsAgreeView 는 동의 완료/스킵 시 routeAfterRequiredTerms 로 ②에 합류한다.
      router.replace({
        path: '/TermsAgree',
        state: {
          pendingTerms: JSON.parse(JSON.stringify(pending)),
          redirect: fallback,
        },
      })
      return
    }
  } catch (e) {
    // 게이트 조회 실패는 비치명적: 가용성 우선으로 일단 통과(다음 진입 시 재시도).
    console.warn('[termsGate] 필수약관 게이트 조회 실패(통과 처리):', e?.message)
    router.replace(fallback)
    return
  }

  // 필수약관 미동의 없음 → ② 제3자 제공 동의 게이트 판정으로 진행.
  await routeAfterRequiredTerms(router, fallback)
}

/**
 * 필수약관 단계를 이미 통과한 뒤의 라우팅(①-b → ② → ③).
 * TermsAgreeView 가 동의 완료/스킵 시 호출한다(routeAfterLogin 을 부르면 ①을 재조회 → 중복 라운드트립·재진입 위험).
 *
 * ①-b 일용직 근로계약서 서명 게이트(일용직 계약서+승인제 T4, plan §6 기본안 4 — 필수약관 뒤·제3자 앞):
 *   일용직(gv_employmentType='DAILY')만 판정하며, 서버 sign-gate 가 signRequiredYn='Y' 를 주면
 *   서명 화면으로 보낸다(활성 계약서 없음/이미 서명 = 'N' 스킵, R2). 조회 실패는 가용성 우선 통과.
 * @param {import('vue-router').Router} router
 * @param {string} [redirect]
 */
export async function routeAfterRequiredTerms(router, redirect) {
  const fallback = redirect || '/MainView'

  if (isDailyWorker()) {
    try {
      const { data } = await api.get('/appApi/dailycontract01/sign-gate')

      if (data?.signRequiredYn === 'Y') {
        // 서명 필요 → 서명 화면으로. 완료 시 화면이 routeAfterContractSign 으로 ②에 합류한다.
        router.replace({
          path: '/DailyContractSign',
          state: { redirect: fallback },
        })
        return
      }
    } catch (e) {
      // 조회 실패 → 가용성 우선 통과(다음 로그인에 재시도).
      console.warn('[termsGate] 계약서 서명 게이트 조회 실패(통과 처리):', e?.message)
    }
  }

  // 계약서 게이트 통과(정규 사용자/스킵/조회 실패) → ② 제3자 제공 동의 게이트로 진행.
  await routeAfterContractSign(router, fallback)
}

/**
 * 계약서 서명 단계를 이미 통과한 뒤의 라우팅(② → ③).
 * DailyContractSignView 가 서명 완료 시 호출한다(routeAfterRequiredTerms 를 부르면 ①-b 를
 * 재조회 → 중복 라운드트립·재진입 위험 — TermsAgreeView 와 동일 구조).
 * @param {import('vue-router').Router} router
 * @param {string} [redirect]
 */
export async function routeAfterContractSign(router, redirect) {
  const fallback = redirect || '/MainView'

  try {
    // ② 활성 연동 사업장 소속 + 006 미응답이면 게이트 노출(동의/미동의 모두 통과 — 강제 아님).
    const { data } = await api.get('/appApi/terms01/subcon-consent-gate')

    if (data?.gateRequiredYn === 'Y') {
      router.replace({
        path: '/ThirdPartyConsent',
        state: { redirect: fallback },
      })
      return
    }
  } catch (e) {
    // 조회 실패 → 가용성 우선 통과(다음 로그인에 재시도).
    console.warn('[termsGate] 제3자 제공 동의 게이트 조회 실패(통과 처리):', e?.message)
  }

  // ③ 목적지 진입.
  router.replace(fallback)
}
