let showAlertModal = null

export function registerAlertHandler(fn) {
  showAlertModal = fn
}

export function $alert(message) {
  return new Promise((resolve) => {
    if (showAlertModal) {
      showAlertModal(message, resolve)
    } else {
      alert(message) // fallback
      resolve()
    }
  })
}
