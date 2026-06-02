import { createApp, h } from 'vue'

export function useModal() {
  let container = null
  let app = null

  const open = (component, props = {}) => {
    // 1. 기존 모달 제거 (재사용 방지)
    close()

    // 2. 컨테이너 DOM 생성
    container = document.createElement('div')
    document.body.appendChild(container)

    // 3. 닫기 함수 (emit("close")에서 사용됨)
    const closeCallback = () => {
      close()
    }

    // 4. App 생성 및 mount
    app = createApp({
      render: () => h(component, { ...props, onClose: closeCallback }),
    })

    // 🔽 전역 속성 복사
    app.config.globalProperties = window.__appGlobalProperties || {}

    app.mount(container)
  }

  const close = () => {
    if (app && container) {
      app.unmount()
      document.body.removeChild(container)
      app = null
      container = null
    }
  }

  return { open, close }
}
