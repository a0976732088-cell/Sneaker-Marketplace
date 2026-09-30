<script setup>
const currentYear = new Date().getFullYear();
const links = [
  { label: "一般商城", name: "commerce" },
  { label: "競標商城", name: "auction" },
  { label: "活動日曆", name: "calendar" },
  { label: "會員中心", name: "member" },
  { label: "常見問題", name: "faq" },
  { label: "關於我們", name: "about" },
];
</script>

<template>
  <footer class="app-footer">
    <div class="container footer-content">
      <section class="footer-brand" aria-labelledby="footer-brand-title">
        <p class="footer-eyebrow">FIND YOUR NEXT PAIR</p>
        <h2 id="footer-brand-title">酷客 <span>Kicks</span></h2>
        <p class="footer-description">從日常穿搭到球場實戰，探索喜愛的鞋款。<br />逛商城、參與競標，也找到屬於你的球鞋風格。</p>
        <RouterLink :to="{ name: 'commerce' }" class="footer-discover">探索所有鞋款→</RouterLink>
      </section>
      <nav class="footer-group" aria-labelledby="footer-explore-title">
        <h2 id="footer-explore-title">探索酷客</h2>
        <ul class="footer-links">
          <li v-for="link in links" :key="link.name"><RouterLink :to="{ name: link.name }">{{ link.label }}</RouterLink></li>
        </ul>
      </nav>
      <section class="footer-group" aria-labelledby="footer-help-title">
        <h2 id="footer-help-title">購物小提醒</h2>
        <ul class="footer-tips">
          <li><h3>找到合適的鞋款</h3><p>購買前，請先確認商品資訊與所選尺寸。</p></li>
          <li><h3>參與球鞋競標</h3><p>出價前，請查看結標時間、最低加價與下次最低出價。</p></li>
          <li><h3>追蹤你的訂單</h3><p>登入會員中心，查看訂單與競標紀錄。</p></li>
          <li><h3>小心假冒客服與付款通知</h3><p>若有人以訂單異常、解除分期或退款為由，要求操作 ATM、提供密碼或簡訊驗證碼，請停止操作。勿透過陌生連結付款，請自行開啟網站確認訂單資訊。</p></li>
        </ul>
      </section>
    </div>
    <div class="container footer-bottom">
      <p>© {{ currentYear }} 酷客 Kicks．保留所有權利。</p>
      <p>球鞋電子商城與競標平台</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer { margin-top: auto; border-top: 1px solid #e7e3de; background: #f8f7f5; color: #252525; }
.footer-content { display: grid; grid-template-columns: 1.5fr 0.8fr 1.2fr; gap: 48px; padding-top: 48px; padding-bottom: 36px; }
.footer-content > * { min-width: 0; }
.footer-eyebrow { margin: 0 0 12px; color: #62564b; font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.footer-brand h2 { margin: 0 0 16px; font-size: 28px; font-weight: 800; }
.footer-brand h2 span { font-weight: 500; }
.footer-description, .footer-tips p { color: #5b5b5b; font-size: 14px; line-height: 1.8; }
.footer-description { margin: 0 0 16px; }
.footer-discover { display: inline-flex; align-items: center; gap: 24px; min-height: 44px; color: #252525; font-size: 14px; font-weight: 700; text-decoration: none; }
.footer-group > h2 { margin: 4px 0 16px; font-size: 16px; font-weight: 700; }
.footer-links, .footer-tips { margin: 0; padding: 0; list-style: none; }
.footer-links a { display: flex; align-items: center; min-height: 44px; width: fit-content; color: #5b5b5b; font-size: 14px; text-decoration: none; }
.footer-links a:hover, .footer-discover:hover { color: #252525; text-decoration: underline; text-underline-offset: 5px; }
.app-footer a:focus-visible { outline: 2px solid #62564b; outline-offset: 4px; border-radius: 2px; }
.footer-tips li + li { margin-top: 16px; }
.footer-tips h3 { margin: 0 0 4px; font-size: 14px; font-weight: 700; }
.footer-tips p { margin: 0; }
.footer-bottom { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px 24px; padding-top: 20px; padding-bottom: 24px; border-top: 1px solid #e7e3de; color: #5b5b5b; font-size: 12px; line-height: 1.6; }
.footer-bottom p { margin: 0; }
@media (max-width: 991px) {
  .footer-content { grid-template-columns: 1fr 1fr; gap: 32px; }
  .footer-brand { grid-column: 1 / -1; }
}
@media (max-width: 575px) {
  .footer-content { grid-template-columns: 1fr; gap: 28px; padding: 32px 24px; }
  .footer-bottom { flex-direction: column; padding: 20px 24px 24px; }
}
</style>
