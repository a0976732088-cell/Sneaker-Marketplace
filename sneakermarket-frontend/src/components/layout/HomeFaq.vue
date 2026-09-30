<script setup>
const questions = [
  {
    id: "shopping",
    question: "一般商城與競標商城有什麼不同？",
    answer: "一般商城可以依商品標示的價格選購鞋款；競標商城則在指定時間內開放出價。參與競標前，請先查看商品內容、結標時間與出價條件。",
    links: [{ label: "逛一般商城", name: "commerce" }, { label: "探索競標", name: "auction" }],
  },
  {
    id: "bidding",
    question: "第一次參與競標，該從哪裡開始？",
    answer: "登入會員後，前往競標商城並開啟感興趣的商品。確認結標時間、最低加價與下次最低出價，再選擇金額並確認出價；是否可出價，以商品頁顯示的狀態為準。",
    links: [{ label: "查看競標商品", name: "auction" }],
  },
  {
    id: "orders",
    question: "在哪裡查看訂單與我的競標紀錄？",
    answer: "登入後，可從會員中心進入「訂單」與「競標」。你也可以使用下方入口，直接查看訂單或自己的出價紀錄。",
    links: [{ label: "查看我的訂單", name: "member-orders" }, { label: "查看出價紀錄", name: "auction-my-bids" }],
  },
  {
    id: "events",
    question: "如何找到近期活動與報名資訊？",
    answer: "前往活動日曆查看活動，選擇感興趣的活動了解日期、內容與報名資訊。實際報名方式及開放狀態，請以各活動頁面顯示的資訊為準。",
    links: [{ label: "查看活動日曆", name: "calendar" }],
  },
];
</script>

<template>
  <section id="home-faq" class="home-faq" aria-labelledby="home-faq-title">
    <header class="faq-intro">
      <p class="faq-eyebrow">GET STARTED</p>
      <h2 id="home-faq-title">第一次來酷客？</h2>
      <p class="faq-description">從選購到競標，先了解你想知道的小事。</p>
    </header>
    <div class="faq-list">
      <details v-for="item in questions" :key="item.id" class="faq-item">
        <summary>
          <span>{{ item.question }}</span>
          <svg class="faq-chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
        </summary>
        <div class="faq-answer">
          <p>{{ item.answer }}</p>
          <div class="faq-links">
            <RouterLink v-for="link in item.links" :key="link.name" :to="{ name: link.name }">
              {{ link.label }} <span aria-hidden="true">→</span>
            </RouterLink>
          </div>
        </div>
      </details>
    </div>
  </section>
</template>

<style scoped>
.home-faq { display: grid; grid-template-columns: 0.8fr 1.5fr; gap: 48px; max-width: 1200px; margin: 64px auto 24px; padding: 36px 0; border-top: 1px solid #e7e3de; scroll-margin-top: 100px; color: #252525; }
.faq-eyebrow { margin: 0 0 12px; color: #62564b; font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.faq-intro h2 { margin: 0 0 12px; font-size: 28px; font-weight: 800; line-height: 1.4; }
.faq-description { margin: 0; color: #5b5b5b; font-size: 15px; line-height: 1.8; }
.faq-list { min-width: 0; border-top: 1px solid #e7e3de; }
.faq-item { border-bottom: 1px solid #e7e3de; }
.faq-item summary { display: flex; align-items: center; justify-content: space-between; gap: 20px; min-height: 64px; padding: 16px 8px; list-style: none; cursor: pointer; font-size: 16px; font-weight: 700; line-height: 1.6; }
.faq-item summary::-webkit-details-marker { display: none; }
.faq-item summary:hover { background: #f8f7f5; }
.faq-chevron { width: 20px; height: 20px; flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 1.8; }
.faq-item[open] .faq-chevron { transform: rotate(180deg); }
.faq-answer { padding: 0 8px 16px; }
.faq-answer p { margin: 0 0 8px; color: #5b5b5b; font-size: 15px; line-height: 1.8; }
.faq-links { display: flex; flex-wrap: wrap; gap: 8px 24px; }
.faq-links a { display: inline-flex; align-items: center; gap: 12px; min-height: 44px; color: #252525; font-size: 14px; font-weight: 700; text-decoration: underline; text-underline-offset: 4px; }
.faq-item summary:focus-visible, .faq-links a:focus-visible { outline: 2px solid #62564b; outline-offset: 3px; border-radius: 2px; }
@media (max-width: 767px) {
  .home-faq { grid-template-columns: 1fr; gap: 24px; margin-top: 40px; padding-top: 28px; }
  .faq-intro h2 { font-size: 24px; }
}
</style>
