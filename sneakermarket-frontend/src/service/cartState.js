//一個全部元件共用的「購物車總件數」
import { ref } from 'vue'
import httpClient from '@/service/httpClient'

export const cartCount = ref(0)

export async function refreshCartCount() {
  try {
    const response = await httpClient.get('/cart')
    let total = 0
    for (const item of response.data) {
      total = total + item.quantity
    }
    cartCount.value = total
  } catch (error) {
    cartCount.value = 0
  }
}

export const cartIconPosition = ref({ left: 24, top: 60 })

export function setCartIconPosition(left, top) {
  cartIconPosition.value = { left, top }
}