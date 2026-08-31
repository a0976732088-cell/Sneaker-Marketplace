import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import '@/assets/css/global.css'
import '@/assets/css/layout.css'
import '@/assets/css/components.css'

createApp(App)
  .use(router)
  .mount('#app')
