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
  const {
    position,
    startDrag: rawStartDrag,
    stopDrag,
  } = useDraggable(0, 0);

  // 사용자가 한 번이라도 직접 드래그하면 자동 재중앙정렬을 멈춘다(수동 위치 존중)
  let userMoved = false;
  let resizeObserver = null;

  const updatePositionToCenter = () => {
    if (!modalRef.value) return;

    const modalWidth = modalRef.value.offsetWidth || 0;
    const modalHeight = modalRef.value.offsetHeight || 0;

    // 상/하단이 화면 밖으로 잘리지 않도록 최소 0 으로 보정
    const x = Math.max(0, (window.innerWidth - modalWidth) / horizontalRatio);
    const y = Math.max(0, (window.innerHeight - modalHeight) / verticalRatio);

    position.value = { x, y };
  };

  // 드래그 시작 시점에 수동 이동으로 표시 (이후 콘텐츠 크기 변화에도 위치 고정)
  const startDrag = (e) => {
    userMoved = true;
    rawStartDrag(e);
  };

  const recenterIfUntouched = () => {
    if (userMoved) return;
    updatePositionToCenter();
  };

  onMounted(async () => {
    // 모달 DOM 렌더링 이후에 크기 측정
    await nextTick();
    updatePositionToCenter();
    window.addEventListener("resize", recenterIfUntouched);

    // 비동기 데이터 로드 등으로 모달 크기가 바뀌면 (드래그 전까지) 다시 중앙으로
    if (typeof ResizeObserver !== "undefined" && modalRef.value) {
      resizeObserver = new ResizeObserver(() => recenterIfUntouched());
      resizeObserver.observe(modalRef.value);
    }
  });

  onUnmounted(() => {
    window.removeEventListener("resize", recenterIfUntouched);
    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }
  });

  return { position, startDrag, stopDrag };
}
