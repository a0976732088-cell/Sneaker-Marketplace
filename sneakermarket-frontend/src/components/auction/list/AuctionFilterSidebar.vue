<template>
  <div class="container py-4 auction-catalog">
    <header class="catalog-header">
      <h2 class="h4 fw-bold mb-0">
        競標商品
        <span v-if="!loading" class="catalog-count">
          ({{ totalElements }})
        </span>
      </h2>

      <div class="catalog-tools">
        <button
          type="button"
          class="btn btn-link btn-sm text-dark text-decoration-none filter-toggle-btn filter-visibility-button"
          :aria-expanded="showFilters"
          aria-controls="auction-filters"
          @click="$emit('toggle-filters')"
        >
          {{ showFilters ? "« 隱藏篩選條件" : "» 顯示篩選條件" }}
        </button>

        <label class="visually-hidden" for="auction-sort"> 排序方式 </label>

        <select
          id="auction-sort"
          v-model="sortModel"
          class="form-select form-select-sm w-auto sort-select"
          :disabled="loading"
          @change="$emit('sort-change')"
        >
          <option value="endAt,asc">即將結束</option>
          <option value="createdAt,desc">最新上架</option>
          <option value="currentPrice,asc">價格：低到高</option>
          <option value="currentPrice,desc">價格：高到低</option>
          <option value="bidCount,desc">出價次數：多到少</option>
        </select>
      </div>
    </header>

    <p v-if="optionsError" class="alert alert-warning py-2" role="status">
      {{ optionsError }}
    </p>

    <div class="row g-4 catalog-layout">
      <!-- 左側篩選 -->
      <aside
        v-if="showFilters"
        id="auction-filters"
        class="col-12 col-md-2"
        aria-label="競標商品篩選"
      >
        <div class="filter-sidebar">
          <form @submit.prevent="$emit('apply-price')">
            <!-- 分類 -->
            <div class="filter-block">
              <ul class="category-list">
                <li>
                  <button
                    type="button"
                    class="category-item"
                    :class="{ active: selectedCategoryIds.length === 0 }"
                    :aria-pressed="selectedCategoryIds.length === 0"
                    @click="$emit('clear-facet', selectedCategoryIds)"
                  >
                    全部分類
                  </button>
                </li>

                <li v-for="category in categories" :key="category.id">
                  <button
                    type="button"
                    class="category-item"
                    :class="{
                      active: selectedCategoryIds.includes(category.id),
                    }"
                    :aria-pressed="selectedCategoryIds.includes(category.id)"
                    @click="
                      $emit('toggle-facet', selectedCategoryIds, category.id)
                    "
                  >
                    {{ category.name }}
                  </button>
                </li>
              </ul>
            </div>

            <!-- 品牌 -->
            <details class="filter-group">
              <summary class="filter-toggle">
                <span>
                  品牌
                  <span v-if="selectedBrandIds.length" class="filter-count">
                    {{ selectedBrandIds.length }}
                  </span>
                </span>
                <span class="filter-icon" aria-hidden="true"></span>
              </summary>

              <div class="filter-options">
                <label
                  v-for="brand in brands"
                  :key="brand.id"
                  class="filter-option"
                >
                  <input
                    type="checkbox"
                    :checked="selectedBrandIds.includes(brand.id)"
                    @change="$emit('toggle-facet', selectedBrandIds, brand.id)"
                  />
                  <span>{{ brand.name }}</span>
                </label>
              </div>
            </details>

            <!-- 狀態 -->
            <details class="filter-group">
              <summary class="filter-toggle">
                <span>
                  競標狀態
                  <span v-if="selectedStatuses.length" class="filter-count">
                    {{ selectedStatuses.length }}
                  </span>
                </span>
                <span class="filter-icon" aria-hidden="true"></span>
              </summary>

              <div class="filter-options">
                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedStatuses.includes('ACTIVE')"
                    @change="$emit('toggle-facet', selectedStatuses, 'ACTIVE')"
                  />
                  <span>競標中</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedStatuses.includes('SCHEDULED')"
                    @change="
                      $emit('toggle-facet', selectedStatuses, 'SCHEDULED')
                    "
                  />
                  <span>尚未開始</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedStatuses.includes('SOLD')"
                    @change="$emit('toggle-facet', selectedStatuses, 'SOLD')"
                  />
                  <span>已結標</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedStatuses.includes('UNSOLD')"
                    @change="$emit('toggle-facet', selectedStatuses, 'UNSOLD')"
                  />
                  <span>流標</span>
                </label>
              </div>
            </details>

            <!-- 商品狀況 -->
            <details class="filter-group">
              <summary class="filter-toggle">
                <span>
                  商品狀況
                  <span v-if="selectedConditions.length" class="filter-count">
                    {{ selectedConditions.length }}
                  </span>
                </span>
                <span class="filter-icon" aria-hidden="true"></span>
              </summary>

              <div class="filter-options">
                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedConditions.includes('NEW')"
                    @change="$emit('toggle-facet', selectedConditions, 'NEW')"
                  />
                  <span>全新</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedConditions.includes('LIKE_NEW')"
                    @change="
                      $emit('toggle-facet', selectedConditions, 'LIKE_NEW')
                    "
                  />
                  <span>近全新</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedConditions.includes('USED')"
                    @change="$emit('toggle-facet', selectedConditions, 'USED')"
                  />
                  <span>二手</span>
                </label>
              </div>
            </details>

            <!-- 尺寸 -->
            <details class="filter-group">
              <summary class="filter-toggle">
                <span>
                  尺寸
                  <span v-if="selectedShoeSizes.length" class="filter-count">
                    {{ selectedShoeSizes.length }}
                  </span>
                </span>
                <span class="filter-icon" aria-hidden="true"></span>
              </summary>

              <div class="filter-options">
                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 8')"
                    @change="$emit('toggle-facet', selectedShoeSizes, 'US 8')"
                  />
                  <span>US 8</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 8.5')"
                    @change="
                      $emit('toggle-facet', selectedShoeSizes, 'US 8.5')
                    "
                  />
                  <span>US 8.5</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 9')"
                    @change="$emit('toggle-facet', selectedShoeSizes, 'US 9')"
                  />
                  <span>US 9</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 9.5')"
                    @change="
                      $emit('toggle-facet', selectedShoeSizes, 'US 9.5')
                    "
                  />
                  <span>US 9.5</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 10')"
                    @change="$emit('toggle-facet', selectedShoeSizes, 'US 10')"
                  />
                  <span>US 10</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 10.5')"
                    @change="
                      $emit('toggle-facet', selectedShoeSizes, 'US 10.5')
                    "
                  />
                  <span>US 10.5</span>
                </label>

                <label class="filter-option">
                  <input
                    type="checkbox"
                    :checked="selectedShoeSizes.includes('US 11')"
                    @change="$emit('toggle-facet', selectedShoeSizes, 'US 11')"
                  />
                  <span>US 11</span>
                </label>
              </div>
            </details>

            <!-- 價格 -->
            <details class="filter-group price-filter">
              <summary class="filter-toggle">
                <span>
                  目前價格
                  <span
                    v-if="appliedMinPrice !== '' || appliedMaxPrice !== ''"
                    class="filter-count"
                  >
                    1
                  </span>
                </span>
                <span class="filter-icon" aria-hidden="true"></span>
              </summary>

              <div class="filter-options">
                <div class="price-inputs">
                  <div>
                    <label class="visually-hidden" for="auction-min-price">
                      最低價格
                    </label>

                    <input
                      id="auction-min-price"
                      v-model="minPriceModel"
                      type="number"
                      min="0"
                      step="1"
                      class="form-control form-control-sm"
                      placeholder="最低價"
                    />
                  </div>

                  <span aria-hidden="true">—</span>

                  <div>
                    <label class="visually-hidden" for="auction-max-price">
                      最高價格
                    </label>

                    <input
                      id="auction-max-price"
                      v-model="maxPriceModel"
                      type="number"
                      min="0"
                      step="1"
                      class="form-control form-control-sm"
                      placeholder="最高價"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  class="btn btn-dark btn-sm w-100 apply-filter-button"
                  :disabled="loading"
                >
                  {{ loading ? "套用中..." : "套用金額篩選" }}
                </button>
              </div>
            </details>

            <div class="filter-actions">
              <button
                type="button"
                class="btn btn-outline-dark btn-sm w-100 clear-filter-button"
                :disabled="loading"
                @click="$emit('clear-filters')"
              >
                清除全部篩選
              </button>
            </div>
          </form>
        </div>
      </aside>

      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  brands: { type: Array, required: true },
  categories: { type: Array, required: true },
  selectedBrandIds: { type: Array, required: true },
  selectedCategoryIds: { type: Array, required: true },
  selectedStatuses: { type: Array, required: true },
  selectedConditions: { type: Array, required: true },
  selectedShoeSizes: { type: Array, required: true },
  minPrice: { type: [String, Number], required: true },
  maxPrice: { type: [String, Number], required: true },
  appliedMinPrice: { type: [String, Number], required: true },
  appliedMaxPrice: { type: [String, Number], required: true },
  sort: { type: String, required: true },
  showFilters: { type: Boolean, required: true },
  loading: { type: Boolean, required: true },
  totalElements: { type: Number, required: true },
  optionsError: { type: String, required: true },
});

const emit = defineEmits([
  "toggle-filters",
  "update:sort",
  "sort-change",
  "toggle-facet",
  "clear-facet",
  "update:minPrice",
  "update:maxPrice",
  "apply-price",
  "clear-filters",
]);

const sortModel = computed({
  get: () => props.sort,
  set: (value) => emit("update:sort", value),
});

const minPriceModel = computed({
  get: () => props.minPrice,
  set: (value) => emit("update:minPrice", value),
});

const maxPriceModel = computed({
  get: () => props.maxPrice,
  set: (value) => emit("update:maxPrice", value),
});
</script>

<style scoped>
.catalog-header {
  position: sticky;
  top: 56px;
  z-index: 20;

  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;

  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  background: #fff;
}

.catalog-header h2 {
  font-size: 1.5rem;
}

.catalog-count {
  color: inherit;
}

.catalog-tools {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.filter-visibility-button {
  white-space: nowrap;
}

.filter-toggle-btn {
  font-size: 0.95rem;
  font-weight: 500;
}

.filter-visibility-button:hover {
  text-decoration: underline;
}

.sort-select {
  border: 0;
  background-color: transparent;
  box-shadow: none;
  font-size: 0.95rem;
  font-weight: 500;
}

.sort-select:focus {
  outline: none;
  box-shadow: none;
}

.catalog-layout {
  align-items: flex-start;
}

.filter-sidebar {
  position: sticky;
  top: 103px;
  font-size: 0.95rem;
}

.filter-block {
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #dee2e6;
}

.category-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.category-item {
  display: block;
  width: 100%;
  padding: 0.18rem 0;
  border: 0;
  background: none;
  color: #212529;
  cursor: pointer;
  font-size: 1.05rem;
  font-weight: 500;
  line-height: 1.7;
  text-align: left;
}

.category-item.active {
  font-weight: 700;
  text-decoration: underline;
}

.filter-group {
  margin: 0;
  padding: 0 0 0.5rem;
  border-bottom: 1px solid #dee2e6;
  margin-bottom: 0.5rem;
}

.filter-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  color: #212529;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  list-style: none;
}

.filter-toggle::-webkit-details-marker {
  display: none;
}

.filter-toggle::marker {
  content: "";
}

.filter-icon {
  color: #495057;
}

.filter-icon::before {
  content: "+";
}

.filter-group[open] .filter-icon::before {
  content: "−";
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.3rem;
  height: 1.3rem;
  padding: 0 0.25rem;
  margin-left: 0.35rem;
  border-radius: 50%;
  background: #111;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 600;
}

.filter-options {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-top: 0.4rem;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  font-size: 0.95rem;
}

.filter-option input[type="checkbox"] {
  flex: 0 0 auto;
  width: 1rem;
  height: 1rem;
  accent-color: #111;
}

.filter-group .form-control {
  min-height: 36px;
  border-color: #dee2e6;
  border-radius: 0.25rem;
  font-size: 0.95rem;
}

.price-inputs {
  display: grid;

  grid-template-columns:
    minmax(0, 1fr)
    auto
    minmax(0, 1fr);

  align-items: center;
  gap: 0.35rem;
}

.price-inputs input {
  min-width: 0;
}

.price-inputs span {
  color: #495057;
}

.filter-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.apply-filter-button {
  min-height: 36px;
  border: 1px solid #111;
  border-radius: 0.25rem;
  background: #111;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 700;
}

.apply-filter-button:hover:not(:disabled) {
  background: #2c2c2c;
}

.apply-filter-button:disabled {
  border-color: #ced4da;
  background: #e9ecef;
  color: #495057;
  opacity: 1;
}

.clear-filter-button {
  border-radius: 0.25rem;
  font-size: 0.95rem;
}

@media (min-width: 768px) and (max-width: 991.98px) {
  .price-inputs {
    grid-template-columns: minmax(0, 1fr);
  }

  .price-inputs > span {
    display: none;
  }
}

@media (max-width: 767.98px) {
  .filter-sidebar {
    position: static;
  }
}
</style>