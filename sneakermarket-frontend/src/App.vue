<script setup>
import AppNavbar from '@/components/layout/AppNavbar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
</script>

<template>
  <div class="app-shell">
    <AppNavbar />

    <main class="app-main">
      <div class="container py-4">
        <RouterView />
      </div>
    </main>

    <AppFooter />
  </div>
</template>
