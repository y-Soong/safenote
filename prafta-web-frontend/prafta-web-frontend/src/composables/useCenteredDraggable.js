// useCenteredDraggable.js
// 공통으로 모달을 화면 중앙(또는 비율 기준)으로 배치하면서 드래그 가능하게 해주는 훅

import { onMounted, onUnmounted, nextTick } from "vue";
import { useDraggable } from "./useDraggable";

/**
 * @param {Ref<HTMLElement | null>} modalRef - 모달 루트 요소 ref
 * @param {Object} options
 * @param {number} options.horizontalRatio - 가로 위치 비율 (기본 2: 정확히 중앙)
 * @param {number} options.verticalRatio - 세로 위치 비율 (기본 2: 정확히 중앙)
 *
 * 예) verticalRatio = 3.5 이면 화면 세로 기준 약 1/3.5 지점에 위치 (살짝 위쪽)
 */
export function useCenteredDraggable(modalRef, options = {}) {
  const { horizontalRatio = 2, verticalRatio = 2 } = options;

  // 실제 드래그 처리는 기존 useDraggable 로 위임
  const { position, startDrag } = useDraggable(0, 0);

  const updatePositionToCenter = () => {
    if (!modalRef.value) return;

    const modalWidth = modalRef.value.offsetWidth || 0;
    const modalHeight = modalRef.value.offsetHeight || 0;

    position.value = {
      x: (window.innerWidth - modalWidth) / horizontalRatio,
      y: (window.innerHeight - modalHeight) / verticalRatio,
    };
  };

  onMounted(async () => {
    // 모달 DOM 렌더링 이후에 크기 측정
    await nextTick();
    updatePositionToCenter();
    window.addEventListener("resize", updatePositionToCenter);
  });

  onUnmounted(() => {
    window.removeEventListener("resize", updatePositionToCenter);
  });

  return { position, startDrag };
}
