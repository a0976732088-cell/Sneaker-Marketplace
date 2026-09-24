<template>
  <div class="container py-4">
    <h1 class="mb-4">編輯競標</h1>

    <!-- 資料載入中 -->
    <div v-if="loading">載入中...</div>

    <!-- API / 表單錯誤 -->
    <div
      v-else-if="errorMessage"
      class="alert alert-danger global-error-alert"
      role="alert"
    >
      {{ errorMessage }}
    </div>

    <template v-else>
      <!-- 目前競標狀態提示 -->
      <div class="alert alert-info">
        目前狀態：{{ formatStatus(auctionStatus) }} ／ 出價次數：{{ bidCount }}
      </div>

      <!-- 不允許編輯 -->
      <div v-if="!editable" class="alert alert-warning">
        {{ editableMessage }}
      </div>

      <form novalidate @submit.prevent="submitUpdate">
        <p class="required-note">
          <span class="required-mark" aria-hidden="true">*</span>
          為必填欄位
        </p>

        <!-- 商品基本資料 -->
        <div class="card mb-4">
          <div class="card-header">商品基本資料</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 品牌 -->
              <div class="col-md-6" data-validation-field="brandId">
                <label for="auction-edit-brand" class="form-label">
                  品牌
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-brand"
                  v-model.number="form.brandId"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.brandId }"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                  :aria-invalid="Boolean(fieldErrors.brandId)"
                  :aria-describedby="
                    fieldErrors.brandId ? 'auction-edit-brand-error' : undefined
                  "
                  @change="handleBrandChange"
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
                  id="auction-edit-brand-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.brandId }}
                </p>

                <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
                  競標開始後不可修改品牌／分類。
                </div>
              </div>

              <!-- 分類 -->
              <div class="col-md-6" data-validation-field="categoryId">
                <label for="auction-edit-category" class="form-label">
                  分類
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-category"
                  v-model.number="form.categoryId"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.categoryId }"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                  :aria-invalid="Boolean(fieldErrors.categoryId)"
                  :aria-describedby="
                    fieldErrors.categoryId
                      ? 'auction-edit-category-error'
                      : undefined
                  "
                  @change="revalidateField('categoryId')"
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
                  id="auction-edit-category-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.categoryId }}
                </p>
              </div>

              <!-- 商品名稱 -->
              <div class="col-md-8" data-validation-field="productName">
                <label for="auction-edit-product-name" class="form-label">
                  商品名稱
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-product-name"
                  v-model.trim="form.productName"
                  type="text"
                  maxlength="150"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.productName }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.productName)"
                  :aria-describedby="
                    fieldErrors.productName
                      ? 'auction-edit-product-name-error'
                      : undefined
                  "
                  @input="revalidateField('productName')"
                />

                <p
                  v-if="fieldErrors.productName"
                  id="auction-edit-product-name-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.productName }}
                </p>
              </div>

              <!-- 型號 -->
              <div class="col-md-4">
                <label class="form-label" for="auction-edit-model-option">
                  型號／系列
                </label>

                <select
                  id="auction-edit-model-option"
                  v-model="selectedModelOption"
                  class="form-select"
                  :disabled="!editable || !form.brandId"
                  @change="handleModelOptionChange"
                >
                  <option value="">
                    {{ form.brandId ? "請選擇型號（可不填）" : "請先選擇品牌" }}
                  </option>
                  <option
                    v-for="option in availableModelOptions"
                    :key="option"
                    :value="option"
                  >
                    {{ option }}
                  </option>
                  <option :value="CUSTOM_OPTION_VALUE">其他（自行填寫）</option>
                </select>

                <div
                  v-if="selectedModelOption === CUSTOM_OPTION_VALUE"
                  class="custom-option-field"
                >
                  <label
                    class="custom-option-label"
                    for="auction-edit-model-custom"
                  >
                    自訂型號／系列
                  </label>
                  <input
                    id="auction-edit-model-custom"
                    v-model.trim="form.model"
                    type="text"
                    maxlength="100"
                    class="form-control"
                    :disabled="!editable"
                    placeholder="例如：聯名款名稱或特殊型號"
                  />
                </div>
              </div>

              <!-- 尺寸 -->
              <div class="col-md-6" data-validation-field="size">
                <label for="auction-edit-size" class="form-label">
                  尺寸
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-size"
                  v-model.trim="form.size"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.size }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.size)"
                  :aria-describedby="
                    fieldErrors.size ? 'auction-edit-size-error' : undefined
                  "
                  @change="revalidateField('size')"
                >
                  <option disabled value="">請選擇尺寸</option>
                  <option
                    v-for="sizeOption in AUCTION_SHOE_SIZES"
                    :key="sizeOption"
                    :value="sizeOption"
                  >
                    {{ sizeOption }}
                  </option>
                </select>

                <p
                  v-if="fieldErrors.size"
                  id="auction-edit-size-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.size }}
                </p>
              </div>

              <!-- 商品狀況 -->
              <div class="col-md-6" data-validation-field="condition">
                <label for="auction-edit-condition" class="form-label">
                  商品狀況
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-condition"
                  v-model="form.condition"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.condition }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.condition)"
                  :aria-describedby="
                    fieldErrors.condition
                      ? 'auction-edit-condition-error'
                      : undefined
                  "
                  @change="revalidateField('condition')"
                >
                  <option value="NEW">全新</option>

                  <option value="LIKE_NEW">近全新</option>

                  <option value="USED">二手</option>
                </select>

                <p
                  v-if="fieldErrors.condition"
                  id="auction-edit-condition-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.condition }}
                </p>
              </div>

              <!-- 商品介紹 -->
              <div class="col-12" data-validation-field="description">
                <label for="auction-edit-description" class="form-label">
                  商品介紹
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <div class="description-composer">
                  <div class="description-template-header">
                    <div>
                      <p class="description-template-title">快速套用介紹範本</p>
                      <p class="description-template-hint">
                        選擇後可直接修改，補充實際鞋況與配件內容。
                      </p>
                    </div>

                    <button
                      type="button"
                      class="description-custom-button"
                      :class="{
                        'is-active':
                          selectedDescriptionOption === CUSTOM_OPTION_VALUE,
                      }"
                      :aria-pressed="
                        selectedDescriptionOption === CUSTOM_OPTION_VALUE
                      "
                      :disabled="!editable"
                      @click="startCustomDescription"
                    >
                      自行撰寫
                    </button>
                  </div>

                  <div
                    class="description-template-grid"
                    aria-label="商品介紹範本"
                  >
                    <button
                      v-for="(option, index) in AUCTION_DESCRIPTION_OPTIONS"
                      :key="option"
                      type="button"
                      class="description-template-card"
                      :class="{
                        'is-active': selectedDescriptionOption === option,
                      }"
                      :aria-pressed="selectedDescriptionOption === option"
                      :disabled="!editable"
                      @click="applyDescriptionTemplate(option)"
                    >
                      <span class="description-template-index">
                        範本 {{ index + 1 }}
                      </span>
                      <span class="description-template-copy">{{ option }}</span>
                    </button>
                  </div>

                  <textarea
                    id="auction-edit-description"
                    v-model.trim="form.description"
                    class="form-control description-editor"
                    :class="{ 'is-invalid-field': fieldErrors.description }"
                    rows="5"
                    required
                    :disabled="!editable"
                    :aria-invalid="Boolean(fieldErrors.description)"
                    :aria-describedby="
                      fieldErrors.description
                        ? 'auction-edit-description-error'
                        : undefined
                    "
                    placeholder="請描述商品狀況、使用情形、盒裝與配件內容"
                    @input="handleDescriptionInput"
                  ></textarea>
                </div>

                <p
                  v-if="fieldErrors.description"
                  id="auction-edit-description-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.description }}
                </p>
              </div>

              <!-- 瑕疵說明 -->
              <div class="col-md-6">
                <label for="auction-edit-defect-note-option" class="form-label">
                  瑕疵說明
                </label>

                <select
                  id="auction-edit-defect-note-option"
                  v-model="selectedFormOptions.defectNote"
                  class="form-select"
                  :disabled="!editable"
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
                  class="custom-option-field"
                >
                  <label for="auction-edit-defect-note-custom" class="custom-option-label">
                    請輸入自訂瑕疵說明
                  </label>
                  <textarea
                    id="auction-edit-defect-note-custom"
                    v-model.trim="form.defectNote"
                    class="form-control"
                    rows="3"
                    maxlength="1000"
                    data-custom-input
                    :disabled="!editable"
                  ></textarea>
                </div>
              </div>

              <!-- 真偽來源 -->
              <div class="col-md-6">
                <label
                  for="auction-edit-authenticity-note-option"
                  class="form-label"
                >
                  真偽／來源說明
                </label>

                <select
                  id="auction-edit-authenticity-note-option"
                  v-model="selectedFormOptions.authenticityNote"
                  class="form-select"
                  :disabled="!editable"
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
                  v-if="
                    selectedFormOptions.authenticityNote ===
                    CUSTOM_OPTION_VALUE
                  "
                  class="custom-option-field"
                >
                  <label
                    for="auction-edit-authenticity-note-custom"
                    class="custom-option-label"
                  >
                    請輸入自訂真偽／來源說明
                  </label>
                  <textarea
                    id="auction-edit-authenticity-note-custom"
                    v-model.trim="form.authenticityNote"
                    class="form-control"
                    rows="3"
                    maxlength="1000"
                    data-custom-input
                    :disabled="!editable"
                  ></textarea>
                </div>
              </div>

              <!-- 商品所在地 -->
              <div class="col-md-6" data-validation-field="location">
                <label class="form-label" for="auction-edit-location">
                  商品所在地
                </label>

                <select
                  id="auction-edit-location"
                  v-model.trim="form.location"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.location }"
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.location)"
                  :aria-describedby="
                    fieldErrors.location
                      ? 'auction-edit-location-error'
                      : undefined
                  "
                  @change="revalidateField('location')"
                >
                  <option value="">請選擇縣市（可不填）</option>
                  <option
                    v-for="city in TAIWAN_CITIES"
                    :key="city"
                    :value="city"
                  >
                    {{ city }}
                  </option>
                  <option
                    v-if="legacyLocationOption"
                    :value="legacyLocationOption"
                  >
                    {{ legacyLocationOption }}（舊資料）
                  </option>
                </select>

                <p
                  v-if="fieldErrors.location"
                  id="auction-edit-location-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.location }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 圖片 -->
        <div class="card mb-4">
          <div class="card-header">商品圖片</div>

          <div class="card-body">
            <p class="image-section-label">目前刊登圖片</p>
            <div class="edit-image-gallery">
              <AuctionImageGallery
                :image-urls="currentImageUrls"
                :product-name="form.productName || '競標商品'"
              />
            </div>
            <p class="image-readonly-note">
              商品圖片會沿用目前刊登內容，儲存本次修改時不會更動圖片。
            </p>
          </div>
        </div>

        <!-- 交易資訊 -->
        <div class="card mb-4">
          <div class="card-header">交易資訊</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 運送方式 -->
              <div class="col-md-6" data-validation-field="shippingMethod">
                <label
                  for="auction-edit-shipping-method-option"
                  class="form-label"
                >
                  運送方式
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-shipping-method-option"
                  v-model="selectedFormOptions.shippingMethod"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.shippingMethod }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.shippingMethod)"
                  :aria-describedby="fieldErrors.shippingMethod ? 'auction-edit-shipping-method-error' : undefined"
                  @change="handleFormOptionChange('shippingMethod')"
                >
                  <option disabled value="">請選擇運送方式</option>
                  <option
                    v-for="option in AUCTION_SHIPPING_METHOD_OPTIONS"
                    :key="option"
                    :value="option"
                  >
                    {{ option }}
                  </option>
                  <option
                    v-if="legacyShippingMethodOption"
                    :value="legacyShippingMethodOption"
                  >
                    {{ legacyShippingMethodOption }}（舊資料）
                  </option>
                </select>


                <p
                  v-if="fieldErrors.shippingMethod"
                  id="auction-edit-shipping-method-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.shippingMethod }}
                </p>
              </div>

              <!-- 運費 -->
              <div class="col-md-6" data-validation-field="shippingFee">
                <label for="auction-edit-shipping-fee" class="form-label">
                  運費（NT$）
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-shipping-fee"
                  v-model.number="form.shippingFee"
                  type="number"
                  min="0"
                  step="1"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.shippingFee }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.shippingFee)"
                  :aria-describedby="
                    fieldErrors.shippingFee
                      ? 'auction-edit-shipping-fee-error'
                      : undefined
                  "
                  @input="revalidateField('shippingFee')"
                />

                <p
                  v-if="fieldErrors.shippingFee"
                  id="auction-edit-shipping-fee-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.shippingFee }}
                </p>
              </div>

              <!-- 付款方式 -->
              <div class="col-md-6" data-validation-field="paymentMethod">
                <label
                  for="auction-edit-payment-method-option"
                  class="form-label"
                >
                  付款方式
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <select
                  id="auction-edit-payment-method-option"
                  v-model="selectedFormOptions.paymentMethod"
                  class="form-select"
                  :class="{ 'is-invalid-field': fieldErrors.paymentMethod }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.paymentMethod)"
                  :aria-describedby="fieldErrors.paymentMethod ? 'auction-edit-payment-method-error' : undefined"
                  @change="handleFormOptionChange('paymentMethod')"
                >
                  <option disabled value="">請選擇付款方式</option>
                  <option
                    v-for="option in AUCTION_PAYMENT_METHOD_OPTIONS"
                    :key="option"
                    :value="option"
                  >
                    {{ option }}
                  </option>
                  <option
                    v-if="legacyPaymentMethodOption"
                    :value="legacyPaymentMethodOption"
                  >
                    {{ legacyPaymentMethodOption }}（舊資料）
                  </option>
                </select>


                <p
                  v-if="fieldErrors.paymentMethod"
                  id="auction-edit-payment-method-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.paymentMethod }}
                </p>
              </div>

              <!-- 交付選項 -->
              <div class="col-12">
                <fieldset class="delivery-options">
                  <legend id="auction-edit-delivery-option-label" class="form-label">
                    交付選項
                    <span class="required-mark" aria-hidden="true">*</span>
                  </legend>

                  <div
                    class="delivery-option-grid"
                    role="radiogroup"
                    aria-labelledby="auction-edit-delivery-option-label"
                    aria-required="true"
                  >
                    <label
                      class="delivery-option-card"
                      :class="{
                        selected:
                          deliveryOption ===
                          DELIVERY_OPTION_VALUES.SHIPPING_ONLY,
                      }"
                    >
                      <input
                        v-model="deliveryOption"
                        type="radio"
                        name="auction-edit-delivery-option"
                        :value="DELIVERY_OPTION_VALUES.SHIPPING_ONLY"
                        :disabled="!editable"
                      />
                      <span>
                        <strong>僅提供寄送</strong>
                        <small>依上方選擇的運送方式與運費寄送。</small>
                      </span>
                    </label>

                    <label
                      class="delivery-option-card"
                      :class="{
                        selected:
                          deliveryOption ===
                          DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP,
                      }"
                    >
                      <input
                        v-model="deliveryOption"
                        type="radio"
                        name="auction-edit-delivery-option"
                        :value="DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP"
                        :disabled="!editable"
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
              </div>

              <!-- 退貨 -->
              <div class="col-md-3">
                <div class="form-check">
                  <input
                    id="allowReturn"
                    v-model="form.allowReturn"
                    type="checkbox"
                    class="form-check-input"
                    :disabled="!editable"
                  />

                  <label for="allowReturn" class="form-check-label">
                    允許退貨
                  </label>
                </div>
              </div>

              <!-- 保固 -->
              <div class="col-12">
                <label for="auction-edit-warranty-note-option" class="form-label">
                  保固／售後說明
                </label>

                <select
                  id="auction-edit-warranty-note-option"
                  v-model="selectedFormOptions.warrantyNote"
                  class="form-select"
                  :disabled="!editable"
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
                  class="custom-option-field"
                >
                  <label
                    for="auction-edit-warranty-note-custom"
                    class="custom-option-label"
                  >
                    請輸入自訂保固／售後說明
                  </label>
                  <textarea
                    id="auction-edit-warranty-note-custom"
                    v-model.trim="form.warrantyNote"
                    class="form-control"
                    rows="2"
                    maxlength="500"
                    data-custom-input
                    :disabled="!editable"
                  ></textarea>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 競標設定 -->
        <div class="card mb-4">
          <div class="card-header">競標設定</div>

          <div class="card-body">
            <div class="row g-3">
              <!-- 起標價 -->
              <div class="col-md-6" data-validation-field="startPrice">
                <label for="auction-edit-start-price" class="form-label">
                  起標價（NT$）
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-start-price"
                  v-model.number="form.startPrice"
                  type="number"
                  min="1"
                  step="1"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.startPrice }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.startPrice)"
                  :aria-describedby="
                    fieldErrors.startPrice
                      ? 'auction-edit-start-price-error'
                      : undefined
                  "
                  @input="revalidateField('startPrice')"
                />

                <p
                  v-if="fieldErrors.startPrice"
                  id="auction-edit-start-price-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.startPrice }}
                </p>
              </div>

              <!-- 最低加價 -->
              <div class="col-md-6" data-validation-field="minIncrement">
                <label for="auction-edit-min-increment" class="form-label">
                  最低加價（NT$）
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-min-increment"
                  v-model.number="form.minIncrement"
                  type="number"
                  min="1"
                  step="1"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.minIncrement }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.minIncrement)"
                  :aria-describedby="
                    fieldErrors.minIncrement
                      ? 'auction-edit-min-increment-error'
                      : undefined
                  "
                  @input="revalidateField('minIncrement')"
                />

                <p
                  v-if="fieldErrors.minIncrement"
                  id="auction-edit-min-increment-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.minIncrement }}
                </p>
              </div>

              <!-- 開始時間 -->
              <div class="col-md-6" data-validation-field="startAt">
                <label for="auction-edit-start-at" class="form-label">
                  開始時間
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-start-at"
                  v-model="form.startAt"
                  type="datetime-local"
                  title="台北時間（UTC+8）"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.startAt }"
                  required
                  :disabled="!editable || auctionStatus === 'ACTIVE'"
                  :aria-invalid="Boolean(fieldErrors.startAt)"
                  :aria-describedby="
                    fieldErrors.startAt
                      ? 'auction-edit-start-at-error'
                      : undefined
                  "
                  @input="revalidateField('startAt')"
                />

                <p
                  v-if="fieldErrors.startAt"
                  id="auction-edit-start-at-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.startAt }}
                </p>

                <div v-if="auctionStatus === 'ACTIVE'" class="form-text">
                  競標開始後不可修改開始時間。
                </div>
              </div>

              <!-- 結束時間 -->
              <div class="col-md-6" data-validation-field="endAt">
                <label for="auction-edit-end-at" class="form-label">
                  結束時間
                  <span class="required-mark" aria-hidden="true">*</span>
                </label>

                <input
                  id="auction-edit-end-at"
                  v-model="form.endAt"
                  type="datetime-local"
                  title="台北時間（UTC+8）"
                  class="form-control"
                  :class="{ 'is-invalid-field': fieldErrors.endAt }"
                  required
                  :disabled="!editable"
                  :aria-invalid="Boolean(fieldErrors.endAt)"
                  :aria-describedby="
                    fieldErrors.endAt ? 'auction-edit-end-at-error' : undefined
                  "
                  @input="revalidateField('endAt')"
                />

                <p
                  v-if="fieldErrors.endAt"
                  id="auction-edit-end-at-error"
                  class="field-error"
                  role="alert"
                >
                  {{ fieldErrors.endAt }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="d-flex gap-2">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="submitting || !editable"
          >
            {{ submitting ? "儲存中..." : "儲存修改" }}
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="submitting"
            @click="goBackToDetail"
          >
            返回競標詳細頁
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import AuctionImageGallery from "@/components/auction/AuctionImageGallery.vue";
import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DESCRIPTION_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_MODEL_OPTIONS_BY_BRAND,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  TAIWAN_CITIES,
} from "@/constants/auctionFormOptions";
import { parseAuctionTime } from "@/utils/auctionTime";
import {
  getAuctionOptions,
  requireAuctionSession,
  handleAuctionAccessError,
} from "@/service/auctionService";
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { getAuctionDetail, updateAuction } from "@/service/auctionService";

// Router
const route = useRoute();
const router = useRouter();

// 資料載入狀態
const loading = ref(true);
const canManage = ref(false);

// 是否正在送出修改 Request
const submitting = ref(false);

// 頁面錯誤
const errorMessage = ref("");

// 目前競標狀態
const auctionStatus = ref("");

// 目前成功出價次數
const bidCount = ref(0);

// 目前瀏覽會員是否為此競標的賣家本人
const isOwner = ref(false);

/*
 * 保存後端原始 startAt。
 *
 * ACTIVE 競標的 startAt 不允許修改，
 * 因此送 PUT 時要使用後端原始值，
 * 不依賴 datetime-local 轉換後的字串。
 */
const originalStartAt = ref("");
const originalLocation = ref("");

const brands = ref([]);
const categories = ref([]);
const currentImageUrls = ref([]);

// 編輯表單
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

const selectedModelOption = ref("");
const selectedDescriptionOption = ref("");

const selectedBrandName = computed(() => {
  const selectedBrand = brands.value.find(
    (brand) => String(brand.id) === String(form.brandId),
  );
  return selectedBrand?.name ?? "";
});

const availableModelOptions = computed(() => {
  const brandName = selectedBrandName.value.trim().toLowerCase();
  if (brandName.includes("jordan")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Jordan;
  if (brandName.includes("nike")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Nike;
  if (brandName.includes("adidas")) return AUCTION_MODEL_OPTIONS_BY_BRAND.adidas;
  if (brandName.replace(/[\s-]+/g, "").includes("newbalance")) {
    return AUCTION_MODEL_OPTIONS_BY_BRAND["New Balance"];
  }
  if (brandName.includes("vans")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Vans;
  return [];
});

const legacyLocationOption = computed(() => {
  const value = originalLocation.value;

  return !isBlank(value) && !TAIWAN_CITIES.includes(value)
    ? value
    : "";
});

// 純 UI selector state；不屬於 AuctionUpdateRequest。
const selectedFormOptions = reactive({
  defectNote: "",
  authenticityNote: "",
  shippingMethod: "",
  paymentMethod: "",
  warrantyNote: "",
});

const formOptionPresets = Object.freeze({
  defectNote: AUCTION_DEFECT_NOTE_OPTIONS,
  authenticityNote: AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  shippingMethod: AUCTION_SHIPPING_METHOD_OPTIONS,
  paymentMethod: AUCTION_PAYMENT_METHOD_OPTIONS,
  warrantyNote: AUCTION_WARRANTY_NOTE_OPTIONS,
});

const FIXED_FORM_OPTION_FIELDS = new Set([
  "shippingMethod",
  "paymentMethod",
]);

function getLegacyFormOption(fieldName) {
  const value = form[fieldName];
  return !isBlank(value) && !formOptionPresets[fieldName].includes(value)
    ? value
    : "";
}

const legacyShippingMethodOption = computed(() =>
  getLegacyFormOption("shippingMethod"),
);
const legacyPaymentMethodOption = computed(() =>
  getLegacyFormOption("paymentMethod"),
);

// 交付選項只控制既有 allowMeetup Boolean，不會新增 request 欄位。
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

// 僅供前端欄位錯誤呈現，不會放進 update request。
const fieldErrors = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  size: "",
  condition: "",
  description: "",
  location: "",
  shippingMethod: "",
  shippingFee: "",
  paymentMethod: "",
  startPrice: "",
  minIncrement: "",
  startAt: "",
  endAt: "",
});

// 固定依畫面由上而下的順序決定第一個要捲動的錯誤。
const validationOrder = [
  "brandId",
  "categoryId",
  "productName",
  "size",
  "condition",
  "description",
  "location",
  "shippingMethod",
  "shippingFee",
  "paymentMethod",
  "startPrice",
  "minIncrement",
  "startAt",
  "endAt",
];

/**
 * 判斷目前畫面是否允許操作編輯表單。
 *
 * 前端只負責 UX；
 * 真正權限仍由後端 updateAuction()
 * 在 Transaction 中重新驗證。
 */
const editable = computed(() => {
  // 不是刊登者本人，不提供編輯操作
  if (!isOwner.value || !canManage.value) {
    return false;
  }

  // 已經有人成功出價，不可修改
  if (bidCount.value > 0) {
    return false;
  }

  // 目前只允許 SCHEDULED / ACTIVE 編輯
  return (
    auctionStatus.value === "SCHEDULED" || auctionStatus.value === "ACTIVE"
  );
});

/**
 * 不允許編輯時顯示的原因。
 */
const editableMessage = computed(() => {
  if (!isOwner.value) {
    return "你無權修改此競標";
  }

  if (bidCount.value > 0) {
    return "此競標已有出價紀錄，無法修改";
  }

  if (
    ["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auctionStatus.value)
  ) {
    return "目前競標狀態不允許修改";
  }

  return "目前競標無法修改";
});

/**
 * 將後端 LocalDateTime 轉成 datetime-local 可使用的格式。
 *
 * 例如：
 * 2026-09-07T10:00:00
 * →
 * 2026-09-07T10:00
 */
function toDateTimeLocal(value) {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

function initializeModelOption(value) {
  form.model = value ?? "";

  if (isBlank(form.model)) {
    selectedModelOption.value = "";
    return;
  }

  selectedModelOption.value = availableModelOptions.value.includes(form.model)
    ? form.model
    : CUSTOM_OPTION_VALUE;
}

function handleBrandChange() {
  selectedModelOption.value = "";
  form.model = "";
  revalidateField("brandId");
}

function handleModelOptionChange() {
  form.model =
    selectedModelOption.value === CUSTOM_OPTION_VALUE
      ? ""
      : selectedModelOption.value;
}

function initializeDescriptionOption(value) {
  form.description = value ?? "";
  selectedDescriptionOption.value = AUCTION_DESCRIPTION_OPTIONS.includes(
    form.description,
  )
    ? form.description
    : isBlank(form.description)
      ? ""
      : CUSTOM_OPTION_VALUE;
}

function applyDescriptionTemplate(option) {
  if (!AUCTION_DESCRIPTION_OPTIONS.includes(option)) return;

  selectedDescriptionOption.value = option;
  form.description = option;
  revalidateField("description");
}

async function startCustomDescription() {
  selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  await nextTick();

  if (typeof document !== "undefined") {
    document.getElementById?.("auction-edit-description")?.focus();
  }
}

function handleDescriptionInput() {
  if (selectedDescriptionOption.value !== form.description) {
    selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  }

  revalidateField("description");
}

/**
 * 依既有後端字串初始化 UI selector。
 *
 * 可自訂欄位展開 Custom input；固定欄位則用動態 legacy option 保留舊值；
 * UI sentinel 永遠不會寫進 form。
 */
function initializeFormOption(fieldName, value) {
  if (value === CUSTOM_OPTION_VALUE) {
    selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    form[fieldName] = "";
    return;
  }

  const formValue = value ?? "";
  form[fieldName] = formValue;

  if (isBlank(formValue)) {
    selectedFormOptions[fieldName] = "";
    return;
  }

  if (FIXED_FORM_OPTION_FIELDS.has(fieldName)) {
    selectedFormOptions[fieldName] = formValue;
    return;
  }

  selectedFormOptions[fieldName] = formOptionPresets[fieldName].includes(
    formValue,
  )
    ? formValue
    : CUSTOM_OPTION_VALUE;
}

/**
 * Select 只是 UI controller；form 永遠只保存 Backend 真正接收的字串。
 */
function handleFormOptionChange(fieldName) {
  const selectedValue = selectedFormOptions[fieldName];

  form[fieldName] =
    selectedValue === CUSTOM_OPTION_VALUE ? "" : selectedValue;
  revalidateField(fieldName);
}

/**
 * 防止保留給 UI controller 的 sentinel 被當成使用者內容送出。
 */
function clearReservedFormOptionValues() {
  for (const fieldName of Object.keys(selectedFormOptions)) {
    if (form[fieldName] === CUSTOM_OPTION_VALUE) {
      form[fieldName] = "";
    }
  }
}

/**
 * 將詳細資料填入編輯表單。
 */
function fillForm(auction) {
  form.brandId = auction.brandId;
  form.categoryId = auction.categoryId;
  form.productName = auction.productName;
  initializeModelOption(auction.model);
  form.size = auction.size;
  form.condition = auction.condition;
  initializeDescriptionOption(auction.description);
  initializeFormOption("defectNote", auction.defectNote);
  initializeFormOption("authenticityNote", auction.authenticityNote);
  originalLocation.value = auction.location ?? "";
  form.location = originalLocation.value;

  initializeFormOption("shippingMethod", auction.shippingMethod);
  form.shippingFee = auction.shippingFee;
  initializeFormOption("paymentMethod", auction.paymentMethod);
  form.allowMeetup = auction.allowMeetup === true;
  form.allowReturn = auction.allowReturn;
  initializeFormOption("warrantyNote", auction.warrantyNote);

  currentImageUrls.value = Array.isArray(auction.imageUrls)
    ? [...auction.imageUrls]
    : [];

  form.startPrice = auction.startPrice;
  form.minIncrement = auction.minIncrement;
  form.startAt = toDateTimeLocal(auction.startAt);
  form.endAt = toDateTimeLocal(auction.endAt);

  canManage.value = auction.canManage;
  auctionStatus.value =
    auction.hasStarted && auction.status === "SCHEDULED"
      ? "ACTIVE"
      : auction.status;
  bidCount.value = auction.bidCount;
  isOwner.value = auction.isOwner;

  // 保存未轉換過的後端原始開始時間
  originalStartAt.value = auction.startAt;
}

/**
 * 讀取目前競標資料。
 *
 * 非賣家本人不可進入競標編輯畫面，
 * 取得詳細資料確認 isOwner=false 後，
 * 直接導向共用 403 權限不足頁。
 */
async function loadAuction() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const auctionId = route.params.id;

    await requireAuctionSession();
    const [response, optionsResponse] = await Promise.all([
      getAuctionDetail(auctionId),
      getAuctionOptions(),
    ]);
    brands.value = optionsResponse.data.data.brands;
    categories.value = optionsResponse.data.data.categories;

    const auction = response.data.data;

    /*
     * 編輯頁屬於賣家本人操作區。
     *
     * 非本人即使直接輸入 /auctions/{id}/edit，
     * 也不顯示編輯表單，而是導向權限不足頁。
     *
     * 後端 PUT API 仍會再次驗證 sellerId，
     * 這裡只是前端的頁面存取控制。
     */
    if (!auction.isOwner) {
      await router.replace({
        name: "forbidden",
      });

      return;
    }

    fillForm(auction);
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("取得競標編輯資料失敗：", error);

    if (error.response?.status === 404) {
      errorMessage.value = "找不到指定的競標資料";
    } else {
      errorMessage.value = "無法取得競標資料";
    }
  } finally {
    loading.value = false;
  }
}

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === "";
}

function isIntegerAtLeast(value, minimum) {
  if (isBlank(value)) {
    return false;
  }

  const numberValue = Number(value);

  return (
    Number.isFinite(numberValue) &&
    Number.isInteger(numberValue) &&
    numberValue >= minimum
  );
}

function getEffectiveStartAtValue() {
  return auctionStatus.value === "ACTIVE"
    ? originalStartAt.value
    : form.startAt;
}

/**
 * 回傳單一欄位的前端輸入錯誤。
 *
 * 這些規則只重現既有 required / min / step 與時間檢查；
 * 後端仍是最終商業規則判定者。
 */
function getFieldError(fieldName) {
  switch (fieldName) {
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
      return isBlank(form.location) ||
        TAIWAN_CITIES.includes(form.location) ||
        form.location === legacyLocationOption.value
        ? ""
        : "請從台灣縣市選項中選擇商品所在地";
    case "shippingMethod":
      return isBlank(form.shippingMethod) ? "請選擇運送方式" : "";
    case "shippingFee":
      if (isBlank(form.shippingFee)) {
        return "請輸入運費";
      }

      return isIntegerAtLeast(form.shippingFee, 0)
        ? ""
        : "運費必須是 0 以上的整數";
    case "paymentMethod":
      return isBlank(form.paymentMethod) ? "請選擇付款方式" : "";
    case "startPrice":
      if (isBlank(form.startPrice)) {
        return "請輸入起標價";
      }

      return isIntegerAtLeast(form.startPrice, 1)
        ? ""
        : "起標價必須是 1 以上的整數";
    case "minIncrement":
      if (isBlank(form.minIncrement)) {
        return "請輸入最低加價金額";
      }

      return isIntegerAtLeast(form.minIncrement, 1)
        ? ""
        : "最低加價金額必須是 1 以上的整數";
    case "startAt": {
      const startAtValue = getEffectiveStartAtValue();

      if (isBlank(startAtValue)) {
        return "請設定競標開始時間";
      }

      return Number.isFinite(parseAuctionTime(startAtValue).getTime())
        ? ""
        : "請輸入有效的競標開始時間";
    }
    case "endAt": {
      if (isBlank(form.endAt)) {
        return "請設定競標結束時間";
      }

      const startAt = parseAuctionTime(getEffectiveStartAtValue());
      const endAt = parseAuctionTime(form.endAt);

      if (!Number.isFinite(endAt.getTime())) {
        return "請輸入有效的競標結束時間";
      }

      if (!Number.isFinite(startAt.getTime())) {
        return "";
      }

      if (endAt <= startAt) {
        return "競標結束時間必須晚於開始時間";
      }

      if (endAt <= new Date()) {
        return "競標結束時間必須晚於目前時間";
      }

      return "";
    }
    default:
      return "";
  }
}

/**
 * 已顯示錯誤的欄位在使用者修改後立即重新驗證。
 */
function revalidateField(fieldName) {
  if (fieldErrors[fieldName]) {
    fieldErrors[fieldName] = getFieldError(fieldName);
  }

  // 開始時間改變時，既有的時間先後錯誤也要同步重新判斷。
  if (fieldName === "startAt" && fieldErrors.endAt) {
    fieldErrors.endAt = getFieldError("endAt");
  }
}


/**
 * 一次顯示全部欄位錯誤，回傳表單是否有效。
 */
function validateForm() {
  clearReservedFormOptionValues();

  validationOrder.forEach((fieldName) => {
    fieldErrors[fieldName] = getFieldError(fieldName);
  });

  return validationOrder.every((fieldName) => !fieldErrors[fieldName]);
}

/**
 * 依畫面順序將第一個錯誤移到視窗中央，並聚焦可操作的控制項。
 */
async function focusFirstValidationError() {
  const firstFieldName = validationOrder.find(
    (fieldName) => fieldErrors[fieldName],
  );

  if (!firstFieldName) {
    return;
  }

  await nextTick();

  if (typeof document === "undefined") {
    return;
  }

  const fieldContainer = document.querySelector(
    `[data-validation-field="${firstFieldName}"]`,
  );

  if (!fieldContainer) {
    return;
  }

  fieldContainer.scrollIntoView({
    behavior: "smooth",
    block: "center",
  });

  const customInput = fieldContainer.querySelector("[data-custom-input]");
  const focusableField =
    customInput && !customInput.disabled
      ? customInput
      : fieldContainer.querySelector("input, select, textarea");

  if (
    focusableField &&
    !focusableField.disabled &&
    typeof focusableField.focus === "function"
  ) {
    focusableField.focus({
      preventScroll: true,
    });
  }
}

/**
 * 提交修改。
 */
async function submitUpdate() {
  errorMessage.value = "";

  if (!editable.value || submitting.value) {
    return;
  }

  if (!validateForm()) {
    await focusFirstValidationError();

    return;
  }

  submitting.value = true;

  try {
    const auctionId = route.params.id;

    /*
     * ACTIVE 的 startAt 必須使用原始後端值，
     * 確保前端不會意外改動已經發生的開始時間。
     */
    const request = {
      ...form,

      startAt:
        auctionStatus.value === "ACTIVE" ? originalStartAt.value : form.startAt,
    };

    await updateAuction(auctionId, request);

    // 修改成功後回詳細頁重新讀取最新資料
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("修改競標失敗：", error);

    /*
     * 例如：
     * - 剛好有人先完成出價
     * - 非本人
     * - 狀態已改變
     * - 時間規則錯誤
     *
     * 都以後端最新判定為準。
     */
    errorMessage.value =
      error.response?.data?.message ?? "修改競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}

/**
 * 返回目前競標詳細頁。
 */
function goBackToDetail() {
  router.push({
    name: "auction-detail",
    params: {
      id: route.params.id,
    },
  });
}

/**
 * 競標狀態中文顯示。
 */
function formatStatus(status) {
  const statusMap = {
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已結標",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "廢標",
  };

  return statusMap[status] ?? status;
}

// 頁面載入後讀取目前競標
onMounted(() => {
  loadAuction();
});
</script>

<style scoped>
.required-note {
  margin: 0 0 1rem;
  color: #6c757d;
  font-size: 0.875rem;
}

.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.field-error {
  margin-top: 0.4rem;
  margin-bottom: 0;
  color: #dc3545;
  font-size: 0.875rem;
  font-weight: 600;
}

.form-control.is-invalid-field,
.form-select.is-invalid-field {
  border-color: #dc3545;
}

.form-control.is-invalid-field:focus,
.form-select.is-invalid-field:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.16);
}

.description-composer {
  padding: 1rem;
  background: #f8f9fa;
  border: 1px solid #e1e5e9;
  border-radius: 0.5rem;
}

.description-template-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.description-template-title,
.description-template-hint {
  margin: 0;
}

.description-template-title {
  color: #212529;
  font-size: 0.9rem;
  font-weight: 800;
  line-height: 1.4;
}

.description-template-hint {
  margin-top: 0.18rem;
  color: #6c757d;
  font-size: 0.8rem;
  line-height: 1.5;
}

.description-custom-button {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 0.35rem 0.75rem;
  color: #212529;
  font: inherit;
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
  background: #fff;
  border: 1px solid #ced4da;
  border-radius: 999px;
}

.description-custom-button:hover:not(:disabled),
.description-custom-button:focus-visible,
.description-custom-button.is-active {
  color: #fff;
  outline: 0;
  background: #212529;
  border-color: #212529;
}

.description-template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 0.65rem;
}

.description-template-card {
  display: flex;
  gap: 0.65rem;
  align-items: flex-start;
  min-width: 0;
  padding: 0.75rem;
  color: #343a40;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
}

.description-template-card:hover:not(:disabled),
.description-template-card:focus-visible {
  outline: 0;
  border-color: #6c757d;
  box-shadow: 0 0.35rem 0.8rem rgb(17 17 17 / 8%);
}

.description-template-card.is-active {
  border-color: #212529;
  box-shadow: inset 3px 0 0 #212529;
}

.description-template-index {
  flex: 0 0 auto;
  padding: 0.18rem 0.4rem;
  color: #495057;
  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1.4;
  white-space: nowrap;
  background: #e9ecef;
  border-radius: 0.25rem;
}

.description-template-card.is-active .description-template-index {
  color: #fff;
  background: #212529;
}

.description-template-copy {
  min-width: 0;
  font-size: 0.82rem;
  line-height: 1.55;
}

.description-editor.form-control {
  min-height: 140px;
  margin-top: 0.9rem;
  background: #fff;
}

.description-custom-button:disabled,
.description-template-card:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}


.custom-option-field {
  margin-top: 0.75rem;
}

.custom-option-label,
.image-section-label {
  display: block;
  margin-bottom: 0.4rem;
  color: #6c757d;
  font-size: 0.82rem;
  font-weight: 700;
}

.edit-image-gallery {
  max-width: 32rem;
}

.image-readonly-note {
  margin: 0.75rem 0 0;
  color: #6c757d;
  font-size: 0.875rem;
}

.delivery-options {
  margin: 0;
  padding: 0;
  border: 0;
}

.delivery-option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.delivery-option-card {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  background: #fff;
  cursor: pointer;
}

.delivery-option-card.selected {
  border-color: #111;
  box-shadow: inset 0 0 0 1px #111;
}

.delivery-option-card input {
  margin-top: 0.2rem;
  accent-color: #111;
}

.delivery-option-card strong,
.delivery-option-card small {
  display: block;
}

.delivery-option-card strong {
  color: #111;
}

.delivery-option-card small {
  margin-top: 0.25rem;
  color: #6c757d;
  line-height: 1.55;
}

.delivery-option-card:has(input:disabled) {
  cursor: not-allowed;
  opacity: 0.7;
}

.global-error-alert {
  color: #dc3545;
  background: #f8d7da;
  border-color: rgba(220, 53, 69, 0.35);
}

@media (max-width: 767.98px) {
  .delivery-option-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
