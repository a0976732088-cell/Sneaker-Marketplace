import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import '@/assets/css/global.css'
import '@/assets/css/layout.css'
import '@/assets/css/components.css'
import "@/assets/css/calendarAndNotificationDashboard.css"

createApp(App)
  .use(router)
  .mount('#app')

axios.defaults.baseURL = import.meta.env.VITE_AXIOS_HTTP_BASEURL || ''