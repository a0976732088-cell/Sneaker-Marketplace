const AUTH_API_BASE_URL = 'http://localhost:8080/api/auth'
const MEMBER_API_BASE_URL = 'http://localhost:8080/api/members'


// ================================
// 登入
// POST /api/auth/login
// ================================
export async function login(data) {

  const response = await fetch(
    `${AUTH_API_BASE_URL}/login`,
    {
      method: 'POST',

      headers: {
        'Content-Type': 'application/json'
      },

      credentials: 'include',

      body: JSON.stringify({
        account: data.account,
        password: data.password
      })
    }
  )

  if (!response.ok) {

    const error = new Error('登入失敗')

    error.status = response.status

    throw error
  }

  return await response.json()
}



// ================================
// 註冊會員
// POST /api/members
// ================================
export async function register(data) {

  const response = await fetch(
    MEMBER_API_BASE_URL,
    {
      method: 'POST',

      headers: {
        'Content-Type': 'application/json'
      },

      body: JSON.stringify({
        account: data.account,
        password: data.password,
        name: data.name,
        email: data.email,
        phone: data.phone
      })
    }
  )


  if (!response.ok) {

    const error = new Error('註冊失敗')

    error.status = response.status

    // 嘗試讀取後端錯誤內容
    try {
      error.data = await response.json()
    } catch {
      // 後端如果不是 JSON 就忽略
    }

    throw error
  }


  return await response.json()
}



// ================================
// 取得目前登入會員
// GET /api/auth/me
// ================================
export async function getCurrentMember() {

  const response = await fetch(
    `${AUTH_API_BASE_URL}/me`,
    {
      method: 'GET',

      credentials: 'include'
    }
  )

  if (!response.ok) {

    const error = new Error('尚未登入')

    error.status = response.status

    throw error
  }

  return await response.json()
}



// ================================
// 登出
// POST /api/auth/logout
// ================================
export async function logout() {

  const response = await fetch(
    `${AUTH_API_BASE_URL}/logout`,
    {
      method: 'POST',

      credentials: 'include'
    }
  )

  if (!response.ok) {

    const error = new Error('登出失敗')

    error.status = response.status

    throw error
  }
}