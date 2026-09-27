<script setup>
import { reactive } from 'vue'

// logo 圖檔放在前端專案的 public/logos/ 底下（請下載各品牌官方圖檔）
// 檔案還沒放進去時，會自動改顯示文字，不會出現破圖
const paymentMethods = [
  { name: 'VISA', src: '/logos/visa.svg' },
  { name: 'Mastercard', src: '/logos/mastercard.svg' },
  { name: 'JCB', src: '/logos/jcb.svg' },
  { name: 'Apple Pay', src: '/logos/apple-pay.svg' },
  { name: 'LINE Pay', src: '/logos/line-pay.svg' },
  { name: '街口支付', src: '/logos/jkopay.svg' },
]

const deliveryMethods = [
  { name: '7-ELEVEN 店到店', src: '/logos/7-eleven.svg' },
  { name: '宅配到府', icon: 'truck' },
]

const failed = reactive({})
const onError = (name) => { failed[name] = true }
</script>

<template>
  <footer class="commerce-footer">
    <div class="footer-inner">
      <section class="footer-group">
        <h2 class="group-title">付款方式</h2>
        <ul class="logo-row">
          <li v-for="m in paymentMethods" :key="m.name" class="logo-chip">
            <img
              v-if="!failed[m.name]"
              :src="m.src"
              :alt="m.name"
              @error="onError(m.name)"
            />
            <span v-else class="logo-text">{{ m.name }}</span>
          </li>
        </ul>
        <p class="group-note">金流服務由綠界科技 ECPay 與 LINE Pay 提供</p>
      </section>

      <section class="footer-group">
        <h2 class="group-title">配送方式</h2>
        <ul class="logo-row">
          <li v-for="d in deliveryMethods" :key="d.name" class="logo-chip">
            <!-- 宅配沒有串特定物流商，用通用卡車圖示 -->
            <template v-if="d.icon === 'truck'">
              <svg class="truck-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 6h11v9H3zM14 9h4l3 3v3h-7z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                <circle cx="7" cy="17" r="1.8" fill="none" stroke="currentColor" stroke-width="1.6" />
                <circle cx="17" cy="17" r="1.8" fill="none" stroke="currentColor" stroke-width="1.6" />
              </svg>
              <span class="logo-text">{{ d.name }}</span>
            </template>
            <template v-else>
              <img
                v-if="!failed[d.name]"
                :src="d.src"
                :alt="d.name"
                @error="onError(d.name)"
              />
              <span v-else class="logo-text">{{ d.name }}</span>
            </template>
          </li>
        </ul>
        <p class="group-note">全台一律運費 NT$100</p>
      </section>
    </div>

    <p class="copyright">© 2026 KICKS</p>
  </footer>
</template>

<style scoped>
.commerce-footer {
  margin-top: 4rem;
  border-top: 1px solid #e2e2e2;
  background: #fafafa;
  color: #333;
}

.footer-inner {
  max-width: 1140px;
  margin: 0 auto;
  padding: 2.5rem 1.5rem 1.5rem;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2.5rem;
}

.group-title {
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0 0 0.9rem;
  color: #111;
}

.logo-row {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.logo-chip {
  height: 40px;
  min-width: 64px;
  padding: 0 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  background: #fff;
  border: 1px solid #e2e2e2;
  border-radius: 6px;
}

.logo-chip img {
  height: 22px;
  width: auto;
  display: block;
}

.logo-text {
  font-size: 0.8rem;
  font-weight: 500;
  color: #444;
  white-space: nowrap;
}

.truck-icon {
  width: 22px;
  height: 22px;
  color: #444;
}

.group-note {
  margin: 0.75rem 0 0;
  font-size: 0.8rem;
  color: #777;
}

.copyright {
  max-width: 1140px;
  margin: 0 auto;
  padding: 1rem 1.5rem 1.5rem;
  border-top: 1px solid #ececec;
  font-size: 0.75rem;
  color: #999;
}

@media (max-width: 768px) {
  .footer-inner {
    grid-template-columns: 1fr;
    gap: 1.75rem;
  }
}
</style>