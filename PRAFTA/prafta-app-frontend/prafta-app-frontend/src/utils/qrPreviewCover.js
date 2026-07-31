//
// QR 스캐너 프리뷰를 컨테이너에 꽉 차게(cover) 보이게 하는 유틸.
//
// ★왜 CSS 만으로 하면 안 되는가 (2026-07-31 실측 회귀):
//   html5-qrcode 의 스캔 루프는 비디오 엘리먼트의 "레이아웃 크기"가 실제 카메라
//   프레임의 화면비와 같다고 전제한다(html5-qrcode.js foreverScan):
//     widthRatio  = video.videoWidth  / video.clientWidth
//     heightRatio = video.videoHeight / video.clientHeight
//     drawImage(video, 0, 0, videoWidth, videoHeight, 0, 0, qrRegion.w, qrRegion.h)
//   여기서 qrRegion 은 뷰파인더(=clientWidth/clientHeight) 크기다. 따라서 video 에
//   width/height:100% + object-fit:cover 를 주면 엘리먼트가 세로로 길어지면서
//   가로형 프레임이 세로 캔버스에 짓눌려 그려지고, QR 이 찌그러져 디코딩이 실패한다.
//   (증상: 카메라 화면은 꽉 차는데 QR 을 아무리 대도 인식되지 않음)
//
//   그래서 레이아웃 크기는 라이브러리가 정한 자연 비율 그대로 두고, 화면을 채우는
//   것은 transform: scale 로만 한다. transform 은 clientWidth/clientHeight 를
//   바꾸지 않으므로 디코딩 좌표가 보존된다. 보이는 결과는 object-fit:cover 와 같다.
//
// 사용법: 스캐너 start() 가 끝난 뒤 컨테이너 엘리먼트를 넘긴다. 반환된 함수를
//         onBeforeUnmount 에서 호출해 해제한다.
//

/**
 * 컨테이너를 채우도록 내부 video 의 배율(--qr-cover-scale)을 계산해 적용한다.
 *
 * @param {HTMLElement|null} container 스캐너 컨테이너(#qr-reader 등)
 * @returns {() => void} 해제 함수
 */
export function startCoverScale(container) {
  if (!container) return () => {}

  const apply = () => {
    const video = container.querySelector('video')
    if (!video) return
    const cw = container.clientWidth
    const ch = container.clientHeight
    const vw = video.clientWidth
    const vh = video.clientHeight
    // 아직 스트림 레이아웃이 잡히지 않은 프레임(0) 에서는 건드리지 않는다.
    if (!cw || !ch || !vw || !vh) return
    // 축소는 하지 않는다(1 미만이면 여백이 생겨 cover 가 깨진다).
    const scale = Math.max(cw / vw, ch / vh, 1)
    container.style.setProperty('--qr-cover-scale', String(scale))
  }

  apply()

  // 비디오 크기는 'playing' 이후에야 확정되고, 회전/키보드 등으로 컨테이너도 바뀐다.
  let ro = null
  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(apply)
    ro.observe(container)
    const video = container.querySelector('video')
    if (video) ro.observe(video)
  }

  window.addEventListener('resize', apply)
  window.addEventListener('orientationchange', apply)

  return () => {
    if (ro) ro.disconnect()
    window.removeEventListener('resize', apply)
    window.removeEventListener('orientationchange', apply)
  }
}
