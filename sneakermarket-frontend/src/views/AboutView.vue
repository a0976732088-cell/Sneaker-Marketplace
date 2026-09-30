<template>
  <article class="about-page" aria-labelledby="about-title">
    <RouterLink :to="{ name: 'home' }" class="about-home">← 返回首頁</RouterLink>
    <header class="about-header">
      <p class="about-eyebrow">OUR STORY</p>
      <h1 id="about-title">關於酷客 Kicks</h1>
      <p class="about-lead">從不同的起點出發，一起打造共同的作品。</p>
      <p>我們是資展國際的五位學員，來自不同產業與背景。在短期的密集學習中，我們將所學轉化為實際作品，共同打造了酷客 Kicks 球鞋商城與競標平台。</p>
    </header>

    <section aria-labelledby="about-origin">
      <h2 id="about-origin">從一個人的興趣，成為五個人的作品</h2>
      <p>這個專案的起點，來自一位組員對球鞋與球類運動的興趣。平常購買鞋款、關注運動的經驗，讓我們選擇以球鞋作為網站主題；而 Yahoo 的競標模式，也啟發我們將一般購物與限時競標結合，提供不同的探索與交易方式。</p>
      <p>我們的共同目標，是讓操作更直覺、資訊更清楚，讓使用者能更快找到喜歡的鞋款與需要的功能。</p>
    </section>

    <section aria-labelledby="about-team">
      <h2 id="about-team">遇到難題，一起找到下一步</h2>
      <p>開發過程中，最讓我們難忘的，是每當有人遇到瓶頸，團隊就會主動聚在一起討論。大家從不同背景與角度提出想法，協助彼此釐清問題、嘗試解法。</p>
      <p>這段經驗讓我們體會到，團隊合作不只是完成各自的分工，也是在有人卡住時，一起找到繼續前進的方法。</p>
    </section>

    <section aria-labelledby="about-future">
      <h2 id="about-future">我們想讓它走得更遠</h2>
      <p>未來，我們希望持續改善購物與管理兩端的操作體驗。除了讓使用者更容易選購與參與競標，也希望讓管理者進入後台後，能透過清楚的流程完成日常工作，減少摸索與重複操作。</p>
      <p>我們也期待依照管理需求提供營運報表，協助團隊掌握整體營運狀況，作為改善流程與降低成本的參考。</p>
      <p>在介面上，我們希望探索 AI 與動畫互動的可能，讓使用者可以選擇不同的瀏覽模式。無論是簡潔的購物畫面，或更具動態感的互動介面，都應保有清楚的資訊、流暢的操作與易用的體驗。</p>
    </section>

    <div class="about-closing">
      <p>酷客 Kicks 是我們踏入軟體開發的起點，<br class="closing-break" />也是一份仍在持續成長的作品。</p>
      <RouterLink :to="{ name: 'commerce' }" class="about-discover">探索我們的商城 <span aria-hidden="true">→</span></RouterLink>
    </div>
  </article>
</template>

<style scoped>
.about-page { max-width: 840px; margin: 16px auto 56px; color: #252525; }
.about-home { display: inline-flex; align-items: center; min-height: 44px; margin-bottom: 28px; color: #5b5b5b; font-size: 14px; text-underline-offset: 4px; }
.about-eyebrow { margin: 0 0 12px; color: #62564b; font-size: 12px; font-weight: 700; letter-spacing: .12em; }
h1 { margin: 0 0 16px; font-size: 36px; font-weight: 800; line-height: 1.4; }
.about-lead { margin-bottom: 24px; color: #252525; font-size: 20px; font-weight: 600; line-height: 1.6; }
.about-header > p:last-child, section p { color: #5b5b5b; font-size: 16px; line-height: 1.9; }
section { margin-top: 36px; padding-top: 32px; border-top: 1px solid #e7e3de; }
h2 { margin: 0 0 18px; font-size: 22px; font-weight: 700; line-height: 1.5; }
section p { margin: 0 0 16px; }
section p:last-child { margin-bottom: 0; }
.about-closing { margin-top: 40px; padding: 28px 32px; border-left: 3px solid #62564b; background: #f8f7f5; }
.about-closing p { margin: 0 0 16px; font-size: 18px; font-weight: 600; line-height: 1.8; }
.about-discover { display: inline-flex; align-items: center; gap: 20px; min-height: 44px; color: #252525; font-size: 14px; font-weight: 700; text-underline-offset: 4px; }
a:focus-visible { outline: 2px solid #62564b; outline-offset: 3px; border-radius: 2px; }
@media (max-width: 575px) {
  .about-page { margin-top: 0; }
  h1 { font-size: 28px; }
  h2 { font-size: 20px; }
  .about-lead { font-size: 18px; }
  .about-closing { padding: 24px 20px; }
  .closing-break { display: none; }
}
</style>
