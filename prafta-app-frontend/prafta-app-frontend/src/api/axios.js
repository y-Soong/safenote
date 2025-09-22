import axios from 'axios'

const api = axios.create({
  baseURL: '/', // 여기도 프록시 대상 URI로 맞춰야 함
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  }
})

// 요청 인터셉터
api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('token')
  
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default api
