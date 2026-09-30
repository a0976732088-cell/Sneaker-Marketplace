<script setup>
const questions = [
  {
    question: '如何選擇適合自己的球鞋尺寸？',
    answer: '穿上平常搭配球鞋的襪子，站立量測雙腳從腳跟到最長腳趾的長度，以較長的一腳作為參考。不同品牌與鞋款的尺寸標示、版型可能不同，請比對該品牌、該鞋款的尺寸資訊，避免只憑平常穿的號碼選購。',
    notes: [
      '確認尺寸使用的單位，例如 US、UK、EU 或公分；不同單位不能直接當成相同號碼。',
      '一般商城請在商品頁確認可選尺寸與庫存，再選擇尺寸加入購物車。',
      '競標商品請先確認頁面標示的尺寸，勿假設得標後可以更換尺寸。',
      '這邊提醒您，目前本網站只提供US作為單位!',
    ],
    links: [{ label: '查看商城鞋款', name: 'commerce' }],
  },
  {
    question: '競標狀態代表什麼？結標後去哪裡看結果？',
    answer: '競標狀態顯示商品目前所處的階段；是否可以出價，請以商品頁的狀態與提示為準。',
    notes: [
      '尚未開始：還沒到開放競標的時間，目前無法出價。',
      '競標中：正在開放競價，可依商品頁顯示的條件參與出價。',
      '已結標：競標已結束並有得標者，不代表已完成付款。',
      '流標：競標結束，但未產生得標者。',
      '已取消：競標已取消，無法繼續出價。',
    ],
    closing: '登入後，前往「我的出價紀錄」確認是否得標；如已得標，可到「競標訂單」查看訂單與後續付款資訊。',
    links: [{ label: '我的出價紀錄', name: 'auction-my-bids' }, { label: '競標訂單', name: 'auction-orders' }],
  },
  { question: '一般商城與競標商城有什麼不同？', answer: '一般商城可以依商品標示的價格選購鞋款；競標商城在指定時間內開放出價。參與競標前，請查看商品內容、結標時間與出價條件。', links: [{ label: '逛一般商城', name: 'commerce' }, { label: '探索競標', name: 'auction' }] },
  { question: '第一次參與競標，該從哪裡開始？', answer: '登入會員後，前往競標商城並開啟感興趣的商品。確認結標時間、最低加價與下次最低出價，再選擇金額並確認出價；是否可出價，以商品頁顯示的狀態為準。', links: [{ label: '查看競標商品', name: 'auction' }] },
  { question: '在哪裡查看訂單與我的競標紀錄？', answer: '登入後，可從會員中心進入「訂單」與「競標」，或透過下方入口查看訂單與自己的出價紀錄。', links: [{ label: '查看我的訂單', name: 'member-orders' }, { label: '查看出價紀錄', name: 'auction-my-bids' }] },
  { question: '如何找到近期活動與報名資訊？', answer: '前往活動日曆，選擇感興趣的活動查看日期、內容與報名資訊。實際報名方式與開放狀態，請以各活動頁面顯示的資訊為準。', links: [{ label: '查看活動日曆', name: 'calendar' }] },
];
</script>

<template>
  <section class="faq-page" aria-labelledby="faq-title">
    <RouterLink :to="{ name: 'home' }" class="faq-home">← 返回首頁</RouterLink>
    <header>
      <p class="faq-eyebrow">GET STARTED</p>
      <h1 id="faq-title">常見問題</h1>
      <p class="faq-description">第一次來酷客？從選購到競標，找到你想知道的答案。</p>
    </header>
    <div class="faq-list">
      <details v-for="item in questions" :key="item.question">
        <summary><span>{{ item.question }}</span><span class="faq-chevron" aria-hidden="true">⌄</span></summary>
        <div class="faq-answer">
          <p>{{ item.answer }}</p>
          <ul v-if="item.notes" class="faq-notes">
            <li v-for="note in item.notes" :key="note">{{ note }}</li>
          </ul>
          <p v-if="item.closing">{{ item.closing }}</p>
          <div class="faq-links">
            <RouterLink v-for="link in item.links" :key="link.name" :to="{ name: link.name }">{{ link.label }} <span aria-hidden="true">→</span></RouterLink>
          </div>
        </div>
      </details>
    </div>
  </section>
</template>

<style scoped>
.faq-page { max-width: 840px; margin: 16px auto 56px; color: #252525; }
.faq-home { display: inline-flex; align-items: center; min-height: 44px; margin-bottom: 28px; color: #5b5b5b; font-size: 14px; }
.faq-eyebrow { margin: 0 0 12px; color: #62564b; font-size: 12px; font-weight: 700; letter-spacing: .12em; }
h1 { margin: 0 0 16px; font-size: 32px; font-weight: 800; }
.faq-description, .faq-answer p { color: #5b5b5b; font-size: 16px; line-height: 1.8; }
.faq-description { margin: 0 0 32px; }
.faq-list { border-top: 1px solid #e7e3de; }
details { border-bottom: 1px solid #e7e3de; }
summary { display: flex; align-items: center; justify-content: space-between; gap: 20px; min-height: 64px; padding: 16px 12px; list-style: none; cursor: pointer; font-size: 16px; font-weight: 700; line-height: 1.6; }
summary::-webkit-details-marker { display: none; }
summary:hover { background: #f8f7f5; }
.faq-chevron { flex-shrink: 0; }
details[open] .faq-chevron { transform: rotate(180deg); }
.faq-answer { padding: 0 12px 16px; }
.faq-answer p { margin: 0 0 8px; }
.faq-notes { margin: 12px 0 16px; padding-left: 24px; color: #5b5b5b; font-size: 16px; line-height: 1.8; }
.faq-notes li + li { margin-top: 8px; }
.faq-links { display: flex; flex-wrap: wrap; gap: 8px 24px; }
.faq-links a { display: inline-flex; align-items: center; gap: 12px; min-height: 44px; color: #252525; font-size: 14px; font-weight: 700; text-underline-offset: 4px; }
summary:focus-visible, a:focus-visible { outline: 2px solid #62564b; outline-offset: 3px; border-radius: 2px; }
@media (max-width: 575px) { .faq-page { margin-top: 0; } h1 { font-size: 28px; } }
</style>
