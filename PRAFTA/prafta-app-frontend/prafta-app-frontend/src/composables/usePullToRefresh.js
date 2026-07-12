import { ref, computed } from 'vue'

/**
 * 당겨서 새로고침(pull-to-refresh) 공통 로직.
 *
 * MainView 의 인라인 구현을 추출한 단일 출처. 스크롤 컨테이너 ref 와 새로고침 콜백을 받아
 * 터치 핸들러와 표시 상태(인디케이터 props)를 반환한다. 각 화면은 스크롤 컨테이너에
 * 핸들러를 바인딩하고, 컨테이너 최상단에 PullRefreshIndicator 를 배치한다.
 *
 * 사용법:
 *   const scrollEl = ref(null)
 *   const ptr = usePullToRefresh(scrollEl, async () => { await reload() })
 *   // template:
 *   //   <main ref="scrollEl"
 *   //         @touchstart.passive="ptr.onPullStart"
 *   //         @touchmove="ptr.onPullMove"
 *   //         @touchend="ptr.onPullEnd"
 *   //         @touchcancel="ptr.onPullEnd">
 *   //     <PullRefreshIndicator v-bind="ptr.indicatorProps" />
 *   //     ...본문...
 *   //   </main>
 *
 * 동작 원리(MainView 원본과 동일):
 *   1) touchstart 시점에 스크롤이 최상단이면 추적 시작
 *   2) touchmove 에서 데드존(SLOP)을 넘는 첫 의미있는 아래 방향 이동에서만 당김 모드 확정
 *      (그 외 방향이면 추적을 끊어 네이티브 스크롤 보존 — 상단 붙음/스크롤 먹힘 방지)
 *   3) 당김 모드 확정 후에만 preventDefault(iOS 고무줄 억제) + 인디케이터 높이 갱신(저항감)
 *   4) touchend 시 임계값 이상이면 onRefresh 실행
 *
 * @param {import('vue').Ref<HTMLElement|null>} scrollElRef 스크롤 컨테이너 element ref
 * @param {() => (Promise<void>|void)} onRefresh 새로고침 실행 콜백(완료까지 await)
 * @param {{ threshold?: number, maxPull?: number, resistance?: number }} [options]
 */
export function usePullToRefresh(scrollElRef, onRefresh, options = {}) {
  const PULL_THRESHOLD = options.threshold ?? 70 // 이 거리 이상 당기고 놓으면 새로고침
  const MAX_PULL = options.maxPull ?? 120 // 인디케이터 최대 높이
  const RESISTANCE = options.resistance ?? 0.5 // 당김 저항감(이동거리 → 인디케이터 높이 환산 배율)
  // 방향 확정 데드존(px). 손가락을 댈 때의 미세한 초기 떨림으로 preventDefault 가 걸려
  // 네이티브 스크롤 제스처 전체가 취소되는 버그를 막는다.
  const PULL_ENGAGE_SLOP = 6

  const pullDistance = ref(0) // 현재 당김 거리(px, 인디케이터 높이)
  const isRefreshing = ref(false) // 새로고침 진행 중
  const isDragging = ref(false) // 손가락으로 당기는 중(애니메이션 토글용)

  const pullReady = computed(() => pullDistance.value >= PULL_THRESHOLD)
  const pullIndicatorHeight = computed(() => (isRefreshing.value ? 48 : pullDistance.value))

  let touchStartY = 0
  let tracking = false // 이 제스처를 추적 중인가(스크롤 컨테이너 최상단에서 시작했을 때만)
  let pullArmed = false // 당겨서 새로고침 모드로 확정됐는가(확정 후에만 preventDefault)

  // 스크롤이 최상단에 닿았는지 판정(0 이하 = 최상단)
  const isScrolledToTop = () => {
    const el = scrollElRef.value
    if (!el) return false
    return el.scrollTop <= 0
  }

  const onPullStart = (e) => {
    if (isRefreshing.value) return
    // 매 제스처 상태 초기화. 추적은 스크롤 컨테이너 최상단에서만 시작.
    pullArmed = false
    tracking = isScrolledToTop()
    if (tracking) touchStartY = e.touches[0].clientY
  }

  const onPullMove = (e) => {
    if (!tracking || isRefreshing.value) return
    const delta = e.touches[0].clientY - touchStartY // 아래로 당기면 양수

    // 아직 당김 모드로 확정되지 않았다면: 데드존을 넘는 '첫 의미있는 이동'에서 방향을 확정한다.
    //   - 최상단에서 아래로 당긴 경우에만 새로고침 모드(pullArmed)로 진입.
    //   - 그 외(위로 스크롤 등)는 추적을 끊어 이후 preventDefault 가 절대 호출되지 않게 한다.
    if (!pullArmed) {
      if (Math.abs(delta) < PULL_ENGAGE_SLOP) return // 판단 보류(네이티브 스크롤 그대로 둠)
      if (delta > 0 && isScrolledToTop()) {
        pullArmed = true
      } else {
        tracking = false
        return
      }
    }

    isDragging.value = true
    pullDistance.value = Math.min(MAX_PULL, delta * RESISTANCE) // 저항감
    // iOS 고무줄/추가 스크롤 억제(당김 모드로 확정된 경우에만)
    if (e.cancelable) e.preventDefault()
  }

  const onPullEnd = async () => {
    isDragging.value = false
    const wasArmed = pullArmed
    pullArmed = false
    if (!tracking) return
    tracking = false
    const shouldRefresh = wasArmed && pullDistance.value >= PULL_THRESHOLD
    pullDistance.value = 0
    if (!shouldRefresh || isRefreshing.value) return
    isRefreshing.value = true
    try {
      await onRefresh()
    } finally {
      isRefreshing.value = false
    }
  }

  // PullRefreshIndicator 에 그대로 v-bind 할 수 있는 표시 상태 묶음.
  const indicatorProps = computed(() => ({
    isRefreshing: isRefreshing.value,
    isDragging: isDragging.value,
    pullReady: pullReady.value,
    pullDistance: pullDistance.value,
    pullIndicatorHeight: pullIndicatorHeight.value,
  }))

  return {
    pullDistance,
    isRefreshing,
    isDragging,
    pullReady,
    pullIndicatorHeight,
    onPullStart,
    onPullMove,
    onPullEnd,
    indicatorProps,
  }
}
