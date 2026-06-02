import { createApp, h } from "vue";

export function useModal() {
  let container = null;
  let app = null;

  const open = (component, props = {}) => {
    // 1. 기존 모달 제거 (재사용 방지)
    close();

    // 2. 열려 있는 select 등 포커스 해제 → 옵션 목록이 모달 뒤에 남는 현상 방지
    if (
      document.activeElement &&
      typeof document.activeElement.blur === "function"
    ) {
      document.activeElement.blur();
    }

    // 3. 배경 컨트롤 방지용 body 클래스
    document.body.classList.add("prafta-modal-open");

    // 4. 컨테이너 DOM 생성
    container = document.createElement("div");
    document.body.appendChild(container);

    // 5. 닫기 함수 (emit("close")에서 사용됨)
    const closeCallback = () => {
      close();
    };

    // 6. App 생성 및 mount
    app = createApp({
      render: () => h(component, { ...props, onClose: closeCallback }),
    });

    // 🔽 전역 속성 복사
    app.config.globalProperties = window.__appGlobalProperties || {};

    app.mount(container);
  };

  const close = () => {
    document.body.classList.remove("prafta-modal-open");
    if (app && container) {
      app.unmount();
      document.body.removeChild(container);
      app = null;
      container = null;
    }
  };

  return { open, close };
}
