import { createRouter, createWebHistory } from 'vue-router'
// import HomeView from '../views/HomeView.vue'
import LoginView from '../views/login/LoginView.vue'
import MainView from '../views/main/MainView.vue'

const routes = [
  {path : '/', component : LoginView}
  , {path : '/main', component : MainView}
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
