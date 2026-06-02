// /src/utils/kakaoMap.js
//
// Kakao Maps JS SDK 동적 로더 (앱 웹뷰용).
//   - 웹 프론트 AttdGpsCoordPanel.vue 의 loadKakaoMapScript() 로직을 앱으로 이식.
//   - dapi.kakao.com SDK 를 런타임에 1회 동적 로드한다(빌드 의존성 없음).
//   - 중복 로드 가드 + 10초 타임아웃 포함.
//   - JS 앱키는 env VITE_PUBLIC_KAKAO_APP_JS_KEY 에서 읽는다(웹 프론트와 동일 키).
//   - services 라이브러리(autoload=false)로 로드하여 Geocoder 역지오코딩을 지원한다.
//
// 사용처: prafta-app-008 외근 사유 시트(OffsiteReasonSheet.vue)의 지도 렌더.

/**
 * Kakao Maps JS SDK 를 동적으로 로드한다.
 * 이미 로드돼 있으면 즉시 resolve, 로딩 중이면 완료를 폴링 대기한다.
 *
 * @returns {Promise<void>} SDK(window.kakao.maps) 준비 완료 시 resolve.
 */
export const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    // 이미 로드 완료된 경우.
    if (window.kakao && window.kakao.maps) {
      resolve()
      return
    }

    // 스크립트 태그는 있으나 아직 초기화 전인 경우 — 완료를 폴링한다.
    const existingScript = document.querySelector('script[src*="dapi.kakao.com"]')
    if (existingScript) {
      const checkInterval = setInterval(() => {
        if (window.kakao && window.kakao.maps) {
          clearInterval(checkInterval)
          resolve()
        }
      }, 100)
      setTimeout(() => {
        clearInterval(checkInterval)
        if (!window.kakao || !window.kakao.maps) {
          reject(new Error('카카오 지도 API 로드 타임아웃'))
        }
      }, 10000)
      return
    }

    // 최초 로드 — env 에서 JS 앱키 확인.
    const kakaoKey = import.meta.env.VITE_PUBLIC_KAKAO_APP_JS_KEY
    if (!kakaoKey) {
      reject(new Error('카카오 지도 API 키가 없습니다.'))
      return
    }

    const scriptUrl = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&libraries=services&autoload=false`
    const script = document.createElement('script')
    script.src = scriptUrl
    script.async = true
    script.onload = () => {
      if (window.kakao && window.kakao.maps) {
        // autoload=false 이므로 명시적으로 maps.load 호출.
        window.kakao.maps.load(() => resolve())
      } else {
        reject(new Error('카카오 지도 API 객체를 찾을 수 없습니다.'))
      }
    }
    script.onerror = () => {
      reject(new Error('카카오 지도 API 로드 실패.'))
    }
    document.head.appendChild(script)
  })
}

export default { loadKakaoMapScript }
