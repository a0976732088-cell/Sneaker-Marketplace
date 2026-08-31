<template>
  <section>
    <h1 class="h3 mb-3">前後端連線測試</h1>
    <p class="text-secondary">
      此頁會呼叫 SneakerMarket Backend 的 <code>GET /api/health</code>。
    </p>

    <button class="btn btn-primary mb-3" :disabled="loading" @click="checkBackend">
      再測一次
    </button>

    <LoadingSpinner v-if="loading" text="正在連線後端..." />
    <ErrorMessage v-else-if="errorMessage" :message="errorMessage" />

    <div v-else-if="health" class="card border-success">
      <div class="card-body">
        <h2 class="h5 card-title">後端連線成功</h2>
        <dl class="row mb-0">
          <dt class="col-sm-3">success</dt>
          <dd class="col-sm-9">{{ health.success }}</dd>

          <dt class="col-sm-3">message</dt>
          <dd class="col-sm-9">{{ health.message }}</dd>

          <dt class="col-sm-3">status</dt>
          <dd class="col-sm-9">{{ health.data?.status }}</dd>

          <dt class="col-sm-3">timestamp</dt>
          <dd class="col-sm-9">{{ health.timestamp }}</dd>
        </dl>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import { getBackendHealth } from '@/service/healthService'

const loading = ref(false)
const health = ref(null)
const errorMessage = ref('')

async function checkBackend() {
  loading.value = true
  health.value = null
  errorMessage.value = ''

  try {
    health.value = await getBackendHealth()
  } catch (error) {
    const detail = error.response?.data?.message || error.message
    errorMessage.value = `無法連線 SneakerMarket Backend：${detail}`
  } finally {
    loading.value = false
  }
}

onMounted(checkBackend)
</script>
