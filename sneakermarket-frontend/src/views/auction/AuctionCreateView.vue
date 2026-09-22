<template>
  <section class="auction-create-page">
    <header class="create-hero">
      <div class="create-hero-inner">
        <div class="create-hero-content">
          <p class="create-eyebrow">SneakerMarket</p>
          <h1>建立競標</h1>
          <p class="create-hero-copy">
            填寫商品與競標資訊，開始你的球鞋競價。
          </p>
        </div>

        <div class="create-hero-icon" aria-hidden="true">👟</div>
      </div>
    </header>

    <div class="create-content">

      <!-- API / 表單錯誤 -->
      <div v-if="errorMessage" class="create-alert" role="alert">
        {{ errorMessage }}
      </div>

      <p v-if="!ready && !errorMessage" class="create-loading" role="status">
        正在確認登入與載入選項...
      </p>
      <form
        v-if="ready"
        class="create-form-shell"
        novalidate
        @submit.prevent="submitAuction"
      >
        <!-- 商品基本資料 -->
        <section class="create-section">
          <header class="section-heading">
            <p class="section-eyebrow">STEP 01</p>
            <h2>商品基本資料</h2>
            <p>填寫品牌、尺寸與商品狀況，讓買家快速掌握商品資訊。</p>
          </header>

          <p class="required-note">
            <span class="required-mark" aria-hidden="true">*</span>
            為必填欄位
          </p>

          <div class="create-field-grid">
            <!-- 品牌 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-brand">
                品牌
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-brand"
                :ref="setFieldElement('brandId')"
                v-model.number="form.brandId"
                class="form-select"
                :class="{ 'is-invalid-field': fieldErrors.brandId }"
                required
                :aria-invalid="Boolean(fieldErrors.brandId)"
                :aria-describedby="fieldErrors.brandId ? 'auction-brand-error' : undefined"
                @change="revalidateErroredField('brandId')"
              >
                <option disabled value="">請選擇品牌</option>

                <option
                  v-for="brand in brands"
                  :key="brand.id"
                  :value="brand.id"
                >
                  {{ brand.name }}
                </option>
              </select>

              <p
                v-if="fieldErrors.brandId"
                id="auction-brand-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.brandId }}
              </p>
            </div>

            <!-- 分類 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-category">
                分類
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-category"
                :ref="setFieldElement('categoryId')"
                v-model.number="form.categoryId"
                class="form-select"
                :class="{ 'is-invalid-field': fieldErrors.categoryId }"
                required
                :aria-invalid="Boolean(fieldErrors.categoryId)"
                :aria-describedby="fieldErrors.categoryId ? 'auction-category-error' : undefined"
                @change="revalidateErroredField('categoryId')"
              >
                <option disabled value="">請選擇分類</option>

                <option
                  v-for="category in categories"
                  :key="category.id"
                  :value="category.id"
                >
                  {{ category.name }}
                </option>
              </select>

              <p
                v-if="fieldErrors.categoryId"
                id="auction-category-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.categoryId }}
              </p>
            </div>

            <!-- 商品名稱 -->
            <div class="col-md-8">
              <label class="form-label" for="auction-product-name">
                商品名稱
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-product-name"
                :ref="setFieldElement('productName')"
                v-model.trim="form.productName"
                type="text"
                maxlength="150"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.productName }"
                required
                :aria-invalid="Boolean(fieldErrors.productName)"
                :aria-describedby="fieldErrors.productName ? 'auction-product-name-error' : undefined"
                @input="revalidateErroredField('productName')"
              />

              <p
                v-if="fieldErrors.productName"
                id="auction-product-name-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.productName }}
              </p>
            </div>

            <!-- 型號 -->
            <div class="col-md-4">
              <label class="form-label"> 型號／系列 </label>

              <input
                v-model.trim="form.model"
                type="text"
                maxlength="100"
                class="form-control"
              />
            </div>

            <!-- 尺寸 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-size">
                尺寸
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-size"
                :ref="setFieldElement('size')"
                v-model.trim="form.size"
                class="form-select"
                :class="{ 'is-invalid-field': fieldErrors.size }"
                required
                :aria-invalid="Boolean(fieldErrors.size)"
                :aria-describedby="fieldErrors.size ? 'auction-size-error' : undefined"
                @change="revalidateErroredField('size')"
              >
                <option disabled value="">請選擇尺寸</option>
                <option
                  v-for="size in AUCTION_SHOE_SIZES"
                  :key="size"
                  :value="size"
                >
                  {{ size }}
                </option>
              </select>

              <p
                v-if="fieldErrors.size"
                id="auction-size-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.size }}
              </p>
            </div>

            <!-- 商品狀況 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-condition">
                商品狀況
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-condition"
                :ref="setFieldElement('condition')"
                v-model="form.condition"
                class="form-select"
                :class="{ 'is-invalid-field': fieldErrors.condition }"
                required
                :aria-invalid="Boolean(fieldErrors.condition)"
                :aria-describedby="fieldErrors.condition ? 'auction-condition-error' : undefined"
                @change="revalidateErroredField('condition')"
              >
                <option disabled value="">請選擇商品狀況</option>

                <option value="NEW">全新</option>

                <option value="LIKE_NEW">近全新</option>

                <option value="USED">二手</option>
              </select>

              <p
                v-if="fieldErrors.condition"
                id="auction-condition-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.condition }}
              </p>
            </div>

            <!-- 商品介紹 -->
            <div class="col-12">
              <label class="form-label" for="auction-description">
                商品介紹
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <textarea
                id="auction-description"
                :ref="setFieldElement('description')"
                v-model.trim="form.description"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.description }"
                rows="4"
                required
                :aria-invalid="Boolean(fieldErrors.description)"
                :aria-describedby="fieldErrors.description ? 'auction-description-error' : undefined"
                @input="revalidateErroredField('description')"
              ></textarea>

              <p
                v-if="fieldErrors.description"
                id="auction-description-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.description }}
              </p>
            </div>

            <!-- 瑕疵說明 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-defect-option">
                瑕疵說明
              </label>

              <select
                id="auction-defect-option"
                v-model="selectedFormOptions.defectNote"
                class="form-select"
                @change="handleFormOptionChange('defectNote')"
              >
                <option value="">請選擇（可不填）</option>
                <option
                  v-for="option in AUCTION_DEFECT_NOTE_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
                <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
              </select>

              <div
                v-if="selectedFormOptions.defectNote === CUSTOM_OPTION_VALUE"
                class="custom-field"
              >
                <label class="custom-field-label" for="auction-defect-custom">
                  請輸入自訂內容
                </label>
                <textarea
                  id="auction-defect-custom"
                  v-model.trim="form.defectNote"
                  class="form-control"
                  rows="3"
                  maxlength="1000"
                ></textarea>
              </div>
            </div>

            <!-- 真偽來源 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-authenticity-option">
                真偽／來源說明
              </label>

              <select
                id="auction-authenticity-option"
                v-model="selectedFormOptions.authenticityNote"
                class="form-select"
                @change="handleFormOptionChange('authenticityNote')"
              >
                <option value="">請選擇（可不填）</option>
                <option
                  v-for="option in AUCTION_AUTHENTICITY_NOTE_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
                <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
              </select>

              <div
                v-if="selectedFormOptions.authenticityNote === CUSTOM_OPTION_VALUE"
                class="custom-field"
              >
                <label
                  class="custom-field-label"
                  for="auction-authenticity-custom"
                >
                  請輸入自訂內容
                </label>
                <textarea
                  id="auction-authenticity-custom"
                  v-model.trim="form.authenticityNote"
                  class="form-control"
                  rows="3"
                  maxlength="1000"
                ></textarea>
              </div>
            </div>

            <!-- 商品所在地 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-location">
                商品所在地
              </label>

              <div class="location-combobox">
                <input
                  id="auction-location"
                  :ref="setFieldElement('location')"
                  v-model.trim="form.location"
                  type="text"
                  maxlength="100"
                  autocomplete="off"
                  class="form-select location-combobox-input"
                  :class="{ 'is-invalid-field': fieldErrors.location }"
                  placeholder="選擇縣市或輸入文字搜尋"
                  role="combobox"
                  aria-autocomplete="list"
                  aria-controls="auction-create-location-options"
                  :aria-expanded="locationMenuOpen"
                  :aria-activedescendant="activeLocationOptionId"
                  :aria-invalid="Boolean(fieldErrors.location)"
                  :aria-describedby="
                    fieldErrors.location ? 'auction-location-error' : undefined
                  "
                  @click="openLocationMenu"
                  @input="handleLocationInput"
                  @change="normalizeLocation"
                  @keydown.down.prevent="moveLocationOption(1)"
                  @keydown.up.prevent="moveLocationOption(-1)"
                  @keydown.enter="handleLocationEnter"
                  @keydown.esc.prevent="closeLocationMenu"
                  @blur="closeLocationMenu"
                />

                <ul
                  v-if="locationMenuOpen"
                  id="auction-create-location-options"
                  class="location-combobox-menu"
                  role="listbox"
                >
                  <li
                    v-for="(city, index) in filteredTaiwanCities"
                    :id="`auction-create-location-option-${index}`"
                    :key="city"
                    class="location-combobox-option"
                    :class="{ 'is-active': index === activeLocationIndex }"
                    role="option"
                    :aria-selected="form.location === city"
                    @pointerdown.prevent="selectLocation(city)"
                  >
                    {{ city }}
                  </li>

                  <li
                    v-if="filteredTaiwanCities.length === 0"
                    class="location-combobox-empty"
                    role="presentation"
                  >
                    找不到符合的縣市
                  </li>
                </ul>
              </div>

              <p
                v-if="fieldErrors.location"
                id="auction-location-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.location }}
              </p>
            </div>
          </div>
        </section>

        <!-- 圖片 -->
        <section class="create-section create-image-section">
          <header class="section-heading">
            <p class="section-eyebrow">STEP 02</p>
            <h2>商品圖片</h2>
            <p>上傳清楚的商品照片，完整呈現球鞋外觀與細節。</p>
          </header>

          <div
            class="image-upload-panel"
            :class="{ 'is-invalid-panel': fieldErrors.images }"
          >
            <label class="create-label" for="auction-images">
              選擇圖片
              <span class="required-mark" aria-hidden="true">*</span>
            </label>

            <input
              id="auction-images"
              :ref="setFieldElement('images')"
              type="file"
              class="create-control create-file-input"
              :class="{ 'is-invalid-field': fieldErrors.images }"
              accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
              multiple
              :aria-invalid="Boolean(fieldErrors.images)"
              :aria-describedby="
                fieldErrors.images
                  ? 'auction-image-help auction-images-error'
                  : 'auction-image-help'
              "
              @change="handleImageChange"
            />

            <p id="auction-image-help" class="upload-help">
              至少 1 張，最多 {{ MAX_AUCTION_IMAGE_COUNT }} 張；支援
              JPG、JPEG、PNG、WEBP。
            </p>

            <p
              v-if="fieldErrors.images"
              id="auction-images-error"
              class="field-error"
              role="alert"
            >
              {{ fieldErrors.images }}
            </p>

            <div v-if="images.length > 0" class="selected-files">
              <p class="selected-files-title">
                已選擇 {{ images.length }} 張圖片
              </p>

              <ul class="file-list">
                <li
                  v-for="(image, index) in images"
                  :key="`${image.name}-${index}`"
                >
                  {{ image.name }}
                </li>
              </ul>
            </div>
          </div>
        </section>

        <!-- 交易資訊 -->
        <section class="create-section">
          <header class="section-heading">
            <p class="section-eyebrow">STEP 03</p>
            <h2>交易資訊</h2>
            <p>說明運送、付款與售後方式，建立清楚的交易共識。</p>
          </header>

          <div class="create-field-grid">
            <!-- 運送方式 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-shipping-method">
                運送方式
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-shipping-method"
                :ref="setFieldElement('shippingMethod')"
                v-model="selectedFormOptions.shippingMethod"
                class="form-select"
                :class="{
                  'is-invalid-field':
                    fieldErrors.shippingMethod &&
                    selectedFormOptions.shippingMethod !== CUSTOM_OPTION_VALUE,
                }"
                required
                :aria-invalid="
                  Boolean(
                    fieldErrors.shippingMethod &&
                      selectedFormOptions.shippingMethod !== CUSTOM_OPTION_VALUE,
                  )
                "
                :aria-describedby="
                  fieldErrors.shippingMethod &&
                  selectedFormOptions.shippingMethod !== CUSTOM_OPTION_VALUE
                    ? 'auction-shipping-method-error'
                    : undefined
                "
                @change="handleFormOptionChange('shippingMethod')"
              >
                <option value="">請選擇運送方式</option>
                <option
                  v-for="option in AUCTION_SHIPPING_METHOD_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
                <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
              </select>

              <p
                v-if="
                  fieldErrors.shippingMethod &&
                  selectedFormOptions.shippingMethod !== CUSTOM_OPTION_VALUE
                "
                id="auction-shipping-method-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.shippingMethod }}
              </p>

              <div
                v-if="selectedFormOptions.shippingMethod === CUSTOM_OPTION_VALUE"
                class="custom-field"
              >
                <label
                  class="custom-field-label"
                  for="auction-shipping-method-custom"
                >
                  自訂運送方式
                </label>
                <input
                  id="auction-shipping-method-custom"
                  :ref="setFieldElement('shippingMethodCustom')"
                  v-model.trim="form.shippingMethod"
                  type="text"
                  maxlength="50"
                  class="form-control"
                  :class="{
                    'is-invalid-field': fieldErrors.shippingMethod,
                  }"
                  required
                  :aria-invalid="Boolean(fieldErrors.shippingMethod)"
                  :aria-describedby="
                    fieldErrors.shippingMethod
                      ? 'auction-shipping-method-error'
                      : undefined
                  "
                  @input="revalidateErroredField('shippingMethod')"
                />

                <p
                  v-if="fieldErrors.shippingMethod"
                  id="auction-shipping-method-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.shippingMethod }}
                </p>
              </div>
            </div>

            <!-- 運費 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-shipping-fee">
                運費（NT$）
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-shipping-fee"
                :ref="setFieldElement('shippingFee')"
                v-model.number="form.shippingFee"
                type="number"
                min="0"
                step="1"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.shippingFee }"
                required
                :aria-invalid="Boolean(fieldErrors.shippingFee)"
                :aria-describedby="fieldErrors.shippingFee ? 'auction-shipping-fee-error' : undefined"
                @input="revalidateErroredField('shippingFee')"
              />

              <p
                v-if="fieldErrors.shippingFee"
                id="auction-shipping-fee-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.shippingFee }}
              </p>
            </div>

            <!-- 付款方式 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-payment-method">
                付款方式
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <select
                id="auction-payment-method"
                :ref="setFieldElement('paymentMethod')"
                v-model="selectedFormOptions.paymentMethod"
                class="form-select"
                :class="{
                  'is-invalid-field':
                    fieldErrors.paymentMethod &&
                    selectedFormOptions.paymentMethod !== CUSTOM_OPTION_VALUE,
                }"
                required
                :aria-invalid="
                  Boolean(
                    fieldErrors.paymentMethod &&
                      selectedFormOptions.paymentMethod !== CUSTOM_OPTION_VALUE,
                  )
                "
                :aria-describedby="
                  fieldErrors.paymentMethod &&
                  selectedFormOptions.paymentMethod !== CUSTOM_OPTION_VALUE
                    ? 'auction-payment-method-error'
                    : undefined
                "
                @change="handleFormOptionChange('paymentMethod')"
              >
                <option value="">請選擇付款方式</option>
                <option
                  v-for="option in AUCTION_PAYMENT_METHOD_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
                <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
              </select>

              <p
                v-if="
                  fieldErrors.paymentMethod &&
                  selectedFormOptions.paymentMethod !== CUSTOM_OPTION_VALUE
                "
                id="auction-payment-method-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.paymentMethod }}
              </p>

              <div
                v-if="selectedFormOptions.paymentMethod === CUSTOM_OPTION_VALUE"
                class="custom-field"
              >
                <label
                  class="custom-field-label"
                  for="auction-payment-method-custom"
                >
                  自訂付款方式
                </label>
                <input
                  id="auction-payment-method-custom"
                  :ref="setFieldElement('paymentMethodCustom')"
                  v-model.trim="form.paymentMethod"
                  type="text"
                  maxlength="50"
                  class="form-control"
                  :class="{
                    'is-invalid-field': fieldErrors.paymentMethod,
                  }"
                  required
                  :aria-invalid="Boolean(fieldErrors.paymentMethod)"
                  :aria-describedby="
                    fieldErrors.paymentMethod
                      ? 'auction-payment-method-error'
                      : undefined
                  "
                  @input="revalidateErroredField('paymentMethod')"
                />

                <p
                  v-if="fieldErrors.paymentMethod"
                  id="auction-payment-method-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.paymentMethod }}
                </p>
              </div>
            </div>

            <!-- 保固 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-warranty-option">
                保固／售後說明
              </label>

              <select
                id="auction-warranty-option"
                v-model="selectedFormOptions.warrantyNote"
                class="form-select"
                @change="handleFormOptionChange('warrantyNote')"
              >
                <option value="">請選擇（可不填）</option>
                <option
                  v-for="option in AUCTION_WARRANTY_NOTE_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </option>
                <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
              </select>

              <div
                v-if="selectedFormOptions.warrantyNote === CUSTOM_OPTION_VALUE"
                class="custom-field"
              >
                <label class="custom-field-label" for="auction-warranty-custom">
                  請輸入自訂內容
                </label>
                <textarea
                  id="auction-warranty-custom"
                  v-model.trim="form.warrantyNote"
                  class="form-control"
                  rows="3"
                  maxlength="500"
                ></textarea>
              </div>
            </div>

            <fieldset class="delivery-options col-12">
              <legend class="form-label">
                交付選項
                <span class="required-mark" aria-hidden="true">*</span>
              </legend>

              <div class="delivery-option-grid">
                <label
                  class="delivery-option-card"
                  :class="{
                    'is-selected':
                      deliveryOption === DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
                  }"
                  for="delivery-shipping-only"
                >
                  <input
                    id="delivery-shipping-only"
                    v-model="deliveryOption"
                    type="radio"
                    name="deliveryOption"
                    :value="DELIVERY_OPTION_VALUES.SHIPPING_ONLY"
                    class="delivery-option-radio"
                    required
                  />
                  <span>
                    <strong>僅提供寄送</strong>
                    <small>依上方選擇的運送方式與運費寄送。</small>
                  </span>
                </label>

                <label
                  class="delivery-option-card"
                  :class="{
                    'is-selected':
                      deliveryOption ===
                      DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP,
                  }"
                  for="delivery-shipping-or-meetup"
                >
                  <input
                    id="delivery-shipping-or-meetup"
                    v-model="deliveryOption"
                    type="radio"
                    name="deliveryOption"
                    :value="DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP"
                    class="delivery-option-radio"
                    required
                  />
                  <span>
                    <strong>提供寄送，也可協調面交</strong>
                    <small>
                      刊登仍保留寄送方式；若雙方同意，可另外協調面交。
                    </small>
                  </span>
                </label>
              </div>
            </fieldset>

            <div class="option-grid return-option-grid">
              <!-- 退貨 -->
              <div class="option-item">
                <input
                  id="allowReturn"
                  v-model="form.allowReturn"
                  type="checkbox"
                  class="option-checkbox"
                />

                <label for="allowReturn" class="option-label">
                  <span>允許退貨</span>
                  <small>依刊登內容向得標者說明可接受的退貨方式。</small>
                </label>
              </div>
            </div>
          </div>
        </section>

        <!-- 競標設定 -->
        <section class="create-section create-settings-section">
          <header class="section-heading">
            <p class="section-eyebrow">STEP 04</p>
            <h2>競標設定</h2>
            <p>設定起標價格、最低加價金額與本次競標期間。</p>
          </header>

          <div class="create-field-grid">
            <!-- 起標價 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-start-price">
                起標價（NT$）
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-start-price"
                :ref="setFieldElement('startPrice')"
                v-model.number="form.startPrice"
                type="number"
                min="1"
                step="1"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.startPrice }"
                required
                :aria-invalid="Boolean(fieldErrors.startPrice)"
                :aria-describedby="fieldErrors.startPrice ? 'auction-start-price-error' : undefined"
                @input="revalidateErroredField('startPrice')"
              />

              <p
                v-if="fieldErrors.startPrice"
                id="auction-start-price-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.startPrice }}
              </p>
            </div>

            <!-- 最低加價 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-min-increment">
                最低加價金額（NT$）
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-min-increment"
                :ref="setFieldElement('minIncrement')"
                v-model.number="form.minIncrement"
                type="number"
                min="1"
                step="1"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.minIncrement }"
                required
                :aria-invalid="Boolean(fieldErrors.minIncrement)"
                :aria-describedby="fieldErrors.minIncrement ? 'auction-min-increment-error' : undefined"
                @input="revalidateErroredField('minIncrement')"
              />

              <p
                v-if="fieldErrors.minIncrement"
                id="auction-min-increment-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.minIncrement }}
              </p>
            </div>

            <!-- 開始時間 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-start-at">
                開始時間
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-start-at"
                :ref="setFieldElement('startAt')"
                v-model="form.startAt"
                type="datetime-local" title="台北時間（UTC+8）"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.startAt }"
                required
                :aria-invalid="Boolean(fieldErrors.startAt)"
                :aria-describedby="fieldErrors.startAt ? 'auction-start-at-error' : undefined"
                @input="revalidateTimeFields"
              />

              <p
                v-if="fieldErrors.startAt"
                id="auction-start-at-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.startAt }}
              </p>
            </div>

            <!-- 結束時間 -->
            <div class="col-md-6">
              <label class="form-label" for="auction-end-at">
                結束時間
                <span class="required-mark" aria-hidden="true">*</span>
              </label>

              <input
                id="auction-end-at"
                :ref="setFieldElement('endAt')"
                v-model="form.endAt"
                type="datetime-local" title="台北時間（UTC+8）"
                class="form-control"
                :class="{ 'is-invalid-field': fieldErrors.endAt }"
                required
                :aria-invalid="Boolean(fieldErrors.endAt)"
                :aria-describedby="fieldErrors.endAt ? 'auction-end-at-error' : undefined"
                @input="revalidateTimeFields"
              />

              <p
                v-if="fieldErrors.endAt"
                id="auction-end-at-error"
                class="field-error"
                role="alert"
              >
                {{ fieldErrors.endAt }}
              </p>
            </div>
          </div>
        </section>

        <!-- 操作 -->
        <div class="create-actions">
          <button
            type="submit"
            class="create-submit-button"
            :disabled="submitting"
          >
            {{ submitting ? "建立中..." : "建立競標" }}
          </button>

          <button
            type="button"
            class="create-back-button"
            :disabled="submitting"
            @click="router.push('/auctions')"
          >
            返回競標列表
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  MAX_AUCTION_IMAGE_COUNT,
  TAIWAN_CITIES,
  normalizeTaiwanCity,
} from "@/constants/auctionFormOptions";
import { parseAuctionTime } from "@/utils/auctionTime";
import { getAuctionOptions, requireAuctionSession, handleAuctionAccessError } from "@/service/auctionService";
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { createAuction } from "@/service/auctionService";

// Router
const router = useRouter();

// 是否正在送出建立競標 Request
const submitting = ref(false);
const ready = ref(false);

// 頁面錯誤訊息
const errorMessage = ref("");

// 使用者目前選擇的圖片
const images = ref([]);

const brands = ref([]);
const categories = ref([]);

// 建立競標表單
const form = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  model: "",
  size: "",
  condition: "",
  description: "",
  defectNote: "",
  authenticityNote: "",
  location: "",

  shippingMethod: "",
  shippingFee: 0,
  paymentMethod: "",
  allowMeetup: false,
  allowReturn: false,
  warrantyNote: "",

  startPrice: null,
  minIncrement: null,
  startAt: "",
  endAt: "",
});

const locationMenuOpen = ref(false);
const activeLocationIndex = ref(-1);

const filteredTaiwanCities = computed(() => {
  const currentLocation =
    typeof form.location === "string" ? form.location.trim() : "";

  if (
    currentLocation === "" ||
    TAIWAN_CITIES.includes(currentLocation)
  ) {
    return TAIWAN_CITIES;
  }

  const searchValue = currentLocation.replace(/^台/, "臺");
  return TAIWAN_CITIES.filter((city) => city.includes(searchValue));
});

const activeLocationOptionId = computed(() =>
  activeLocationIndex.value >= 0
    ? `auction-create-location-option-${activeLocationIndex.value}`
    : undefined,
);

// Select 僅控制 UI；實際送出的值仍寫入原本 form 欄位。
const selectedFormOptions = reactive({
  defectNote: "",
  authenticityNote: "",
  shippingMethod: "",
  paymentMethod: "",
  warrantyNote: "",
});

const deliveryOption = computed({
  get() {
    return form.allowMeetup
      ? DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP
      : DELIVERY_OPTION_VALUES.SHIPPING_ONLY;
  },
  set(value) {
    form.allowMeetup =
      value === DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP;
  },
});

function handleFormOptionChange(field) {
  if (!Object.prototype.hasOwnProperty.call(selectedFormOptions, field)) return;

  const selectedValue = selectedFormOptions[field];
  form[field] = selectedValue === CUSTOM_OPTION_VALUE ? "" : selectedValue;
  revalidateErroredField(field);
}

/**
 * 防止保留給 UI controller 的 sentinel 被當成使用者內容送出。
 */
function clearReservedFormOptionValues() {
  for (const field of Object.keys(selectedFormOptions)) {
    if (form[field] === CUSTOM_OPTION_VALUE) {
      form[field] = "";
    }
  }
}

const validationOrder = Object.freeze([
  "brandId",
  "categoryId",
  "productName",
  "size",
  "condition",
  "description",
  "location",
  "images",
  "shippingMethod",
  "shippingFee",
  "paymentMethod",
  "startPrice",
  "minIncrement",
  "startAt",
  "endAt",
]);

const fieldErrors = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  size: "",
  condition: "",
  description: "",
  location: "",
  images: "",
  shippingMethod: "",
  shippingFee: "",
  paymentMethod: "",
  startPrice: "",
  minIncrement: "",
  startAt: "",
  endAt: "",
});

const fieldElements = new Map();

function setFieldElement(field) {
  return (element) => {
    if (element) {
      fieldElements.set(field, element);
      return;
    }

    fieldElements.delete(field);
  };
}

function isBlank(value) {
  return (
    value === null ||
    value === undefined ||
    (typeof value === "string" && value.trim() === "")
  );
}

function integerAmountError(
  value,
  { invalidMessage, minimum, minimumMessage, integerMessage },
) {
  if (isBlank(value) || !Number.isFinite(Number(value))) {
    return invalidMessage;
  }

  const amount = Number(value);

  if (amount < minimum) {
    return minimumMessage;
  }

  if (!Number.isInteger(amount)) {
    return integerMessage;
  }

  return "";
}

function getTimeErrors() {
  const timeErrors = {
    startAt: "",
    endAt: "",
  };

  if (isBlank(form.startAt)) {
    timeErrors.startAt = "請設定競標開始時間";
  }

  if (isBlank(form.endAt)) {
    timeErrors.endAt = "請設定競標結束時間";
  }

  if (timeErrors.startAt || timeErrors.endAt) {
    return timeErrors;
  }

  const startAt = parseAuctionTime(form.startAt);
  const endAt = parseAuctionTime(form.endAt);

  if (
    !Number.isFinite(startAt.getTime()) ||
    !Number.isFinite(endAt.getTime()) ||
    endAt <= startAt
  ) {
    timeErrors.endAt = "競標結束時間必須晚於開始時間";
    return timeErrors;
  }

  if (endAt <= new Date()) {
    timeErrors.endAt = "競標結束時間必須晚於目前時間";
  }

  return timeErrors;
}

function getFieldError(field) {
  switch (field) {
    case "brandId":
      return isBlank(form.brandId) ? "請選擇品牌" : "";
    case "categoryId":
      return isBlank(form.categoryId) ? "請選擇分類" : "";
    case "productName":
      return isBlank(form.productName) ? "請輸入商品名稱" : "";
    case "size":
      return AUCTION_SHOE_SIZES.includes(form.size) ? "" : "請選擇尺寸";
    case "condition":
      return isBlank(form.condition) ? "請選擇商品狀況" : "";
    case "description":
      return isBlank(form.description) ? "請輸入商品介紹" : "";
    case "location":
      return isBlank(form.location) || TAIWAN_CITIES.includes(form.location)
        ? ""
        : "請從台灣縣市選項中選擇商品所在地";
    case "images":
      if (images.value.length === 0) return "請至少選擇一張商品圖片";
      if (images.value.length > MAX_AUCTION_IMAGE_COUNT) {
        return `競標商品最多只能選擇 ${MAX_AUCTION_IMAGE_COUNT} 張圖片`;
      }
      return "";
    case "shippingMethod":
      if (
        selectedFormOptions.shippingMethod === CUSTOM_OPTION_VALUE &&
        isBlank(form.shippingMethod)
      ) {
        return "請輸入自訂運送方式";
      }
      return isBlank(form.shippingMethod) ? "請選擇運送方式" : "";
    case "shippingFee":
      return integerAmountError(form.shippingFee, {
        invalidMessage: "請輸入有效的運費",
        minimum: 0,
        minimumMessage: "運費不可小於 0",
        integerMessage: "運費必須為整數",
      });
    case "paymentMethod":
      if (
        selectedFormOptions.paymentMethod === CUSTOM_OPTION_VALUE &&
        isBlank(form.paymentMethod)
      ) {
        return "請輸入自訂付款方式";
      }
      return isBlank(form.paymentMethod) ? "請選擇付款方式" : "";
    case "startPrice":
      return integerAmountError(form.startPrice, {
        invalidMessage: "請輸入有效的起標價",
        minimum: 1,
        minimumMessage: "起標價必須大於 0",
        integerMessage: "起標價必須為整數",
      });
    case "minIncrement":
      return integerAmountError(form.minIncrement, {
        invalidMessage: "請輸入有效的最低加價金額",
        minimum: 1,
        minimumMessage: "最低加價金額必須大於 0",
        integerMessage: "最低加價金額必須為整數",
      });
    default:
      return "";
  }
}

function revalidateErroredField(field) {
  if (!fieldErrors[field]) return;
  fieldErrors[field] = getFieldError(field);
}

function resetActiveLocationOption() {
  const selectedIndex = filteredTaiwanCities.value.indexOf(form.location);
  activeLocationIndex.value =
    selectedIndex >= 0
      ? selectedIndex
      : filteredTaiwanCities.value.length > 0
        ? 0
        : -1;
}

function openLocationMenu() {
  locationMenuOpen.value = true;
  resetActiveLocationOption();
}

function closeLocationMenu() {
  locationMenuOpen.value = false;
  activeLocationIndex.value = -1;
}

function handleLocationInput() {
  locationMenuOpen.value = true;
  resetActiveLocationOption();
  revalidateErroredField("location");
}

function moveLocationOption(direction) {
  if (!locationMenuOpen.value) {
    openLocationMenu();
    return;
  }

  const optionCount = filteredTaiwanCities.value.length;
  if (optionCount === 0) {
    activeLocationIndex.value = -1;
    return;
  }

  activeLocationIndex.value =
    (activeLocationIndex.value + direction + optionCount) % optionCount;
}

function selectLocation(city) {
  if (!TAIWAN_CITIES.includes(city)) return;

  form.location = city;
  fieldErrors.location = "";
  closeLocationMenu();
}

function handleLocationEnter(event) {
  if (!locationMenuOpen.value) return;

  const city = filteredTaiwanCities.value[activeLocationIndex.value];
  if (!city) return;

  event.preventDefault();
  selectLocation(city);
}

function normalizeLocation() {
  form.location = normalizeTaiwanCity(form.location);
  revalidateErroredField("location");
}

function revalidateTimeFields() {
  if (!fieldErrors.startAt && !fieldErrors.endAt) return;

  const timeErrors = getTimeErrors();
  fieldErrors.startAt = timeErrors.startAt;
  fieldErrors.endAt = timeErrors.endAt;
}

async function focusField(field) {
  await nextTick();

  const customField =
    (field === "shippingMethod" || field === "paymentMethod") &&
    selectedFormOptions[field] === CUSTOM_OPTION_VALUE
      ? `${field}Custom`
      : field;
  const element = fieldElements.get(customField) ?? fieldElements.get(field);
  if (!element) return;

  element.scrollIntoView({
    behavior: "smooth",
    block: "center",
  });
  element.focus({
    preventScroll: true,
  });
}

async function focusFirstError() {
  const firstErrorField = validationOrder.find((field) => fieldErrors[field]);
  if (!firstErrorField) return;

  await focusField(firstErrorField);
}

/**
 * 使用者選擇商品圖片。
 */
function handleImageChange(event) {
  errorMessage.value = "";

  const selectedFiles = Array.from(event.target.files ?? []);

  if (selectedFiles.length > MAX_AUCTION_IMAGE_COUNT) {
    images.value = [];

    /*
     * 清掉 input，
     * 避免畫面看起來仍然保留超過上限的圖片。
    */
    event.target.value = "";

    fieldErrors.images = `競標商品最多只能選擇 ${MAX_AUCTION_IMAGE_COUNT} 張圖片`;
    void focusField("images");

    return;
  }

  images.value = selectedFiles;
  revalidateErroredField("images");
}

/**
 * 建立競標前的簡單前端檢查。
 *
 * 後端仍然是最終驗證者，
 * 前端檢查只用來改善操作體驗。
 */
function validateForm() {
  clearReservedFormOptionValues();
  normalizeLocation();

  for (const field of validationOrder) {
    if (field === "startAt" || field === "endAt") continue;
    fieldErrors[field] = getFieldError(field);
  }

  const timeErrors = getTimeErrors();
  fieldErrors.startAt = timeErrors.startAt;
  fieldErrors.endAt = timeErrors.endAt;

  return !validationOrder.some((field) => fieldErrors[field]);
}

/**
 * 提交建立競標表單。
 */
async function submitAuction() {
  if (!ready.value || submitting.value) return;
  errorMessage.value = "";

  if (!validateForm()) {
    await focusFirstError();
    return;
  }

  submitting.value = true;

  try {
    const response = await createAuction(
      {
        ...form,
      },
      images.value,
    );

    // 後端目前 ApiResponse.data 直接回新建立的 auctionId
    const auctionId = response.data.data;

    // 建立完成後直接進入既有競標詳細頁
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("建立競標失敗：", error);

    /*
     * 優先顯示後端 GlobalExceptionHandler /
     * ApiResponse 回傳的錯誤訊息。
     */
    errorMessage.value =
      error.response?.data?.message ?? "建立競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}
onMounted(async () => {
  try {
    await requireAuctionSession();
    const response = await getAuctionOptions();
    brands.value = response.data.data.brands;
    categories.value = response.data.data.categories;
    if (!brands.value.length || !categories.value.length) {
      errorMessage.value = "品牌或分類尚未建立，目前無法刊登競標。";
      return;
    }
    ready.value = true;
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    errorMessage.value = "無法載入競標選項，請重新整理後再試。";
  }
});
</script>

<style scoped>
.auction-create-page,
.auction-create-page * {
  box-sizing: border-box;
}

.auction-create-page {
  min-height: 100vh;
  overflow-x: hidden;
  color: #212529;
  background: #fff;
}

.create-hero {
  position: relative;
  display: flex;
  min-height: 224px;
  overflow: hidden;
  color: #fff;
  background: #111;
  border-bottom: 1px solid #212529;
}

.create-hero-inner {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  margin: 0 auto;
  padding: 3rem 1.5rem;
}

.create-hero-content {
  position: relative;
  z-index: 1;
  max-width: 680px;
}

.create-eyebrow,
.section-eyebrow {
  margin: 0 0 0.65rem;
  font-size: 0.75rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.create-eyebrow {
  color: #dee2e6;
}

.create-hero h1 {
  margin: 0;
  font-size: clamp(2.25rem, 5vw, 3.5rem);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.create-hero-copy {
  max-width: 560px;
  margin: 1rem 0 0;
  color: #dee2e6;
  font-size: 1rem;
  line-height: 1.75;
}

.create-hero-icon {
  position: absolute;
  right: 1rem;
  bottom: -2.65rem;
  color: #fff;
  font-size: clamp(7.5rem, 17vw, 11rem);
  line-height: 1;
  opacity: 0.1;
  transform: rotate(-12deg);
  user-select: none;
  pointer-events: none;
}

.create-content {
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  margin: 0 auto;
  padding: 3.25rem 1.5rem 4rem;
}

.create-alert,
.create-loading {
  width: 100%;
  margin: 0 0 2rem;
  padding: 1rem 1.15rem;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  background: #f8f9fa;
  color: #212529;
  line-height: 1.6;
}

.create-alert {
  color: #842029;
  font-weight: 700;
  background: #f8d7da;
  border-color: #f1aeb5;
  border-left: 4px solid #dc3545;
}

.create-loading {
  color: #6c757d;
}

.create-form-shell {
  width: 100%;
  min-width: 0;
}

.required-note {
  margin: 0 0 1.5rem;
  color: #6c757d;
  font-size: 0.82rem;
  line-height: 1.5;
}

.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.create-section {
  margin: 0;
  padding: 3rem 0;
  border: 0;
  border-top: 1px solid #e9ecef;
}

.create-section:first-child {
  padding-top: 0;
  border-top: 0;
}

.section-heading {
  max-width: 650px;
  margin-bottom: 2rem;
}

.section-eyebrow {
  color: #8a9096;
}

.section-heading h2 {
  margin: 0;
  color: #111;
  font-size: clamp(1.55rem, 3vw, 1.9rem);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.section-heading > p:last-child {
  margin: 0.65rem 0 0;
  color: #6c757d;
  line-height: 1.7;
}

.create-field-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 1.4rem 1.5rem;
  width: 100%;
  min-width: 0;
}

.create-field-grid > [class^="col-"],
.create-field-grid > [class*=" col-"] {
  width: auto;
  max-width: none;
  min-width: 0;
  padding: 0;
}

.create-field-grid > .col-md-8 {
  grid-column: span 8;
}

.create-field-grid > .col-md-6 {
  grid-column: span 6;
}

.create-field-grid > .col-md-4 {
  grid-column: span 4;
}

.create-field-grid > .col-12 {
  grid-column: 1 / -1;
}

.form-label,
.create-label {
  display: block;
  margin: 0 0 0.55rem;
  color: #212529;
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1.4;
}

.form-control,
.form-select,
.create-control {
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 48px;
  padding: 0.7rem 0.85rem;
  color: #212529;
  font: inherit;
  line-height: 1.4;
  background-color: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: none;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

textarea.form-control,
textarea.create-control {
  min-height: 112px;
  resize: vertical;
}

.form-control::placeholder,
.create-control::placeholder {
  color: #8a9096;
}

.form-control:focus,
.form-select:focus,
.create-control:focus {
  outline: 0;
  border-color: #111;
  box-shadow: 0 0 0 3px rgb(17 17 17 / 10%);
}

.form-control.is-invalid-field,
.form-select.is-invalid-field,
.create-control.is-invalid-field {
  border-color: #dc3545;
}

.form-control.is-invalid-field:focus,
.form-select.is-invalid-field:focus,
.create-control.is-invalid-field:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgb(220 53 69 / 15%);
}

.location-combobox {
  position: relative;
  min-width: 0;
}

.location-combobox-menu {
  position: absolute;
  z-index: 30;
  top: calc(100% + 0.35rem);
  right: 0;
  left: 0;
  max-height: 15rem;
  margin: 0;
  padding: 0.35rem;
  overflow-y: auto;
  list-style: none;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: 0 0.75rem 1.5rem rgb(17 17 17 / 10%);
}

.location-combobox-option,
.location-combobox-empty {
  padding: 0.65rem 0.75rem;
  border-radius: 0.25rem;
  font-size: 1rem;
  line-height: 1.4;
}

.location-combobox-option {
  color: #212529;
  cursor: pointer;
}

.location-combobox-option:hover,
.location-combobox-option.is-active {
  color: #111;
  background: #e9ecef;
}

.location-combobox-option[aria-selected="true"]:not(.is-active) {
  font-weight: 700;
  background: #f1f3f5;
}

.location-combobox-empty {
  color: #6c757d;
}

.field-error {
  margin: 0.4rem 0 0;
  color: #dc3545;
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.5;
}

.custom-field {
  margin-top: 0.8rem;
}

.custom-field-label {
  display: block;
  margin: 0 0 0.45rem;
  color: #6c757d;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.4;
}

.image-upload-panel {
  padding: 1.5rem;
  background: #f8f9fa;
  border: 1px dashed #ced4da;
  border-radius: 0.5rem;
}

.image-upload-panel.is-invalid-panel {
  border-color: #dc3545;
}

.create-file-input {
  height: auto;
  padding: 0.35rem;
  background: #fff;
}

.create-file-input::file-selector-button {
  min-height: 38px;
  margin-right: 0.85rem;
  padding: 0.55rem 1rem;
  color: #fff;
  font-weight: 700;
  background: #111;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.upload-help {
  margin: 0.7rem 0 0;
  color: #6c757d;
  font-size: 0.85rem;
  line-height: 1.6;
}

.selected-files {
  margin-top: 1.4rem;
  padding-top: 1.25rem;
  border-top: 1px solid #dee2e6;
}

.selected-files-title {
  margin: 0 0 0.75rem;
  color: #212529;
  font-size: 0.9rem;
  font-weight: 700;
}

.file-list {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.file-list li {
  position: relative;
  overflow-wrap: anywhere;
  padding-left: 1rem;
  color: #6c757d;
  font-size: 0.88rem;
  line-height: 1.5;
}

.file-list li::before {
  position: absolute;
  top: 0.65em;
  left: 0;
  width: 5px;
  height: 5px;
  content: "";
  background: #111;
  border-radius: 50%;
}

.delivery-options {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.delivery-options > .form-label {
  width: 100%;
}

.delivery-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.delivery-option-card {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: 1rem;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.delivery-option-card.is-selected {
  background: #f8f9fa;
  border-color: #111;
}

.delivery-option-radio {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0.15rem 0.75rem 0 0;
  accent-color: #111;
}

.delivery-option-card span {
  min-width: 0;
}

.delivery-option-card strong,
.delivery-option-card small {
  display: block;
}

.delivery-option-card strong {
  color: #212529;
  font-size: 0.9rem;
}

.delivery-option-card small {
  margin-top: 0.25rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.55;
}

.option-grid {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
  min-width: 0;
}

.return-option-grid {
  grid-template-columns: minmax(0, 1fr);
}

.option-item {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: 1rem;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
}

.option-checkbox {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0.15rem 0.75rem 0 0;
  accent-color: #111;
}

.option-label {
  min-width: 0;
  margin: 0;
  cursor: pointer;
}

.option-label span {
  display: block;
  color: #212529;
  font-size: 0.9rem;
  font-weight: 700;
}

.option-label small {
  display: block;
  margin-top: 0.25rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.55;
}

.create-settings-section {
  margin: 0 0 3rem;
  padding: 2.5rem;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 0.5rem;
}

.create-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 2rem;
  border-top: 1px solid #e9ecef;
}

.create-submit-button,
.create-back-button {
  min-height: 48px;
  padding: 0.75rem 1.6rem;
  font: inherit;
  font-weight: 700;
  line-height: 1.2;
  border: 1px solid #111;
  border-radius: 999px;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.create-submit-button {
  color: #fff;
  background: #111;
}

.create-submit-button:hover:not(:disabled) {
  background: #212529;
  border-color: #212529;
}

.create-back-button {
  color: #212529;
  background: #fff;
  border-color: #dee2e6;
}

.create-back-button:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #8a9096;
}

.create-submit-button:focus-visible,
.create-back-button:focus-visible,
.delivery-option-radio:focus-visible,
.option-checkbox:focus-visible {
  outline: 2px solid #111;
  outline-offset: 3px;
}

.create-submit-button:disabled,
.create-back-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 767.98px) {
  .create-hero {
    min-height: 190px;
  }

  .create-hero-inner {
    padding: 2.5rem 1.25rem;
  }

  .create-hero-copy {
    max-width: 80%;
  }

  .create-hero-icon {
    right: -1.5rem;
    bottom: -1.75rem;
    font-size: 8rem;
  }

  .create-content {
    padding: 2.5rem 1.25rem 3rem;
  }

  .create-section {
    padding: 2.5rem 0;
  }

  .create-field-grid > .col-md-8,
  .create-field-grid > .col-md-6,
  .create-field-grid > .col-md-4,
  .create-field-grid > .col-12 {
    grid-column: 1 / -1;
  }

  .option-grid {
    grid-template-columns: 1fr;
  }

  .delivery-option-grid {
    grid-template-columns: 1fr;
  }

  .create-settings-section {
    margin-bottom: 2.5rem;
    padding: 2rem 1.25rem;
  }

  .create-actions {
    flex-direction: column;
  }

  .create-submit-button,
  .create-back-button {
    width: 100%;
  }
}

@media (max-width: 479.98px) {
  .create-hero {
    min-height: 176px;
  }

  .create-hero-inner,
  .create-content {
    padding-right: 1rem;
    padding-left: 1rem;
  }

  .create-hero h1 {
    font-size: 2.2rem;
  }

  .create-hero-copy {
    max-width: 88%;
    font-size: 0.94rem;
  }

  .section-heading {
    margin-bottom: 1.5rem;
  }

  .create-field-grid {
    gap: 1.2rem;
  }

  .image-upload-panel,
  .create-settings-section {
    padding: 1.25rem 1rem;
  }
}
</style>
