<script setup>
defineProps({
  tabs: {
    type: Array,
    required: true,
  },
  activeTab: {
    type: String,
    required: true,
  },
  keyword: {
    type: String,
    required: true,
  },
  placement: {
    type: String,
    required: true,
  },
});

const emit = defineEmits([
  "update:activeTab",
  "update:keyword",
  "update:placement",
]);
</script>

<template>
  <div class="panel-tabs" role="tablist">
    <button
      v-for="tab in tabs"
      :key="tab"
      :class="{ active: activeTab === tab }"
      @click="emit('update:activeTab', tab)"
    >
      {{ tab }}
    </button>
  </div>

  <div class="filter-row">
    <label class="search-box">
      <span>⌕</span>

      <input
        :value="keyword"
        type="search"
        placeholder="搜尋廣告名稱或編號..."
        @input="emit('update:keyword', $event.target.value)"
      />
    </label>

    <select
      :value="placement"
      aria-label="篩選廣告版位"
      @change="emit('update:placement', $event.target.value)"
    >
      <option>全部版位</option>
      <option>橫幅</option>
      <option>彈窗</option>
      <option>側欄</option>
      <option>卡片</option>
    </select>
  </div>
</template>

<style scoped>
.panel-tabs {
  display: flex;
  gap: 23px;
  padding: 0 21px;
  border-bottom: 1px solid var(--line);
}

.panel-tabs button {
  position: relative;
  padding: 17px 1px 13px;
  border: 0;
  color: #8490a1;
  background: none;
  font-size: 14px;
  font-weight: 700;
}

.panel-tabs button.active {
  color: var(--ink);
}

.panel-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--coral);
  content: "";
}

.panel-tabs button span {
  display: inline-grid;
  min-width: 17px;
  height: 17px;
  margin-left: 4px;
  place-items: center;
  border-radius: 8px;
  background: #f0f2f5;
  color: #7d8797;
  font-size: 12px;
}

.filter-row {
  display: flex;
  gap: 9px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
}

.search-box {
  display: flex;
  align-items: center;
  width: min(310px, 100%);
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fafbfc;
}

.search-box span {
  padding-left: 11px;
  color: #97a1b0;
  font-size: 18px;
}

.search-box input {
  width: 100%;
  padding: 9px;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 14px;
}

.filter-row select {
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: #536071;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
}
</style>
