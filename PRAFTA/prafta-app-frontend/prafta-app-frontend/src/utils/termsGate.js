// termsGate.js — 앱 로그인 후 "필수약관 미동의 게이트" 라우팅 헬퍼.
//   토큰 세팅 직후 호출한다. 미동의 필수약관이 있으면 동의 화면(/TermsAgree)으로,
//   없으면 원래 목적지(redirect 또는 /MainView)로 라우팅한다.
//
//   정규/일용직/본인인증 3경로 공통으로 사용한다(LoginView/PhoneAuthView).
//
//   가용성 우선: 게이트 조회 자체가 실패하면(네트워크/서버) 진입을 막지 않고 통과시킨다
//   (로그인은 이미 성공했고, 다음 진입 시 다시 게이트가 동작한다). 단 경고 로깅은 남긴다.
import api from '@/api/axios'

/**
 * 로그인 직후 라우팅 결정.
 * @param {import('vue-router').Router} router  vue-router 인스턴스
 * @param {string} [redirect]  로그인 진입 시 보존된 redirect 경로(없으면 /MainView)
 */
export async function routeAfterLogin(router, redirect) {
  const fallback = redirect || '/MainView'

  try {
    const { data } = await api.get('/appApi/terms01/required-terms-pending')
    const pending = Array.isArray(data?.terms) ? data.terms : []

    if (pending.length > 0) {
      // 미동의 필수약관 존재 → 동의 화면으로. 목록은 history state 로 전달(새로고침 대비 화면이 재조회).
      router.replace({
        path: '/TermsAgree',
        state: {
          pendingTerms: JSON.parse(JSON.stringify(pending)),
          redirect: fallback,
        },
      })
      return
    }

    // 미동의 없음 → 곧바로 목적지 진입.
    router.replace(fallback)
  } catch (e) {
    // 게이트 조회 실패는 비치명적: 가용성 우선으로 일단 통과(다음 진입 시 재시도).
    console.warn('[termsGate] 필수약관 게이트 조회 실패(통과 처리):', e?.message)
    router.replace(fallback)
  }
}
