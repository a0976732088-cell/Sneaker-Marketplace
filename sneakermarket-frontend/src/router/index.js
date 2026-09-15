import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import { getCurrentMember } from "@/service/authService";

const routes = [
  {
    path: "/",
    name: "home",
    component: HomeView,
  },
  {
    path: "/member",
    name: "member",
    component: () => import("@/views/member/MemberHomeView.vue"),
  },
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/member/LoginView.vue"),
  },
  {
    path: "/register",
    name: "register",
    component: () => import("@/views/member/RegisterView.vue"),
  },
  {
    path: "/coupons",
    name: "coupons",
    component: () =>
      import("@/views/coupon/CouponView.vue"),
  },
  {
    path: "/products",
    name: "commerce",
    component: () => import("@/views/commerce/CommerceHomeView.vue"),
  },
  {
    path: "/products/:id",
    name: "product-detail",
    component: () => import("@/views/commerce/ProductDetailView.vue"),
  },
  {
    path: "/cart",
    name: "cart",
    component: () => import("@/views/commerce/CartView.vue"),
  },
  {
    path: "/checkout",
    name: "checkout",
    component: () => import("@/views/commerce/CheckoutView.vue"),
  },
  {
    path: "/orders/:id",
    name: "order-confirmation",
    component: () => import("@/views/commerce/OrderConfirmationView.vue"),
  },
  {
    path: "/ads",
    name: "advertisement",
    component: () => import("@/views/advertisement/AdvertisementHomeView.vue"),
  },
  {
    path: "/calendar",
    name: "calendar",
    component: () => import("@/views/calendar/CalendarHomeView.vue"),
  },
  {
    path: "/calendar/eventManage",
    name: "eventManage",
    component: () => import("@/views/calendar/EventManageView.vue"),
  },
  {
    path: "/auctions",
    name: "auction",
    component: () => import("@/views/auction/AuctionListView.vue"),
  },
  {
    path: "/auctions/create",
    name: "auction-create",
    component: () => import("@/views/auction/AuctionCreateView.vue"),
  },
  {
    path: "/auctions/me/bids",
    name: "auction-my-bids",
    component: () => import("@/views/auction/AuctionMyBidsView.vue"),
  },
  {
    path: "/auctions/me/listings",
    name: "auction-my-listings",
    component: () => import("@/views/auction/AuctionMyListingsView.vue"),
  },
  {
    path: "/auction-orders",
    name: "auction-orders",
    component: () => import("@/views/auction/AuctionOrderListView.vue"),
  },
  {
    path: "/auction-orders/:orderNumber/payment",
    name: "auction-order-payment",
    component: () => import("@/views/auction/AuctionOrderPaymentView.vue"),
  },
  {
    path: "/auctions/:id",
    name: "auction-detail",
    component: () => import("@/views/auction/AuctionDetailView.vue"),
  },
  {
    path: "/auctions/:id/edit",
    name: "auction-edit",
    component: () => import("@/views/auction/AuctionEditView.vue"),
  },

  // =========================
  // Admin 管理後台
  // =========================
  {
    path: "/admin",
    name: "admin",
    component: () => import("@/views/admin/AdminHomeView.vue"),

    // 只有 ROLE_ADMIN 才能進入
    meta: {
      requiresAdmin: true,
    },
  },
  {
    path: "/admin/auctions",
    name: "admin-auctions",
    component: () => import("@/views/admin/AdminAuctionListView.vue"),
    meta: {
      requiresAdmin: true,
    },
  },

  {
    path: "/admin/auctions/:id",
    name: "admin-auction-detail",
    component: () => import("@/views/admin/AdminAuctionDetailView.vue"),
    meta: {
      requiresAdmin: true,
    },
  },
  
  {
    path: "/admin/products",
    name: "admin-products",
    component: () => import("@/views/admin/AdminProductListView.vue"),
    meta: {
      requiresAdmin: true,
    },
  },

    {
    path: "/admin/products/new",
    name: "admin-product-create",
    component: () => import("@/views/admin/AdminProductFormView.vue"),
    meta: {
      requiresAdmin: true,
    },
  },
  
  {
    path: "/admin/products/:id/edit",
    name: "admin-product-edit",
    component: () => import("@/views/admin/AdminProductFormView.vue"),
    meta: {
      requiresAdmin: true,
    },
  },

  {
    path: "/ads/storefront",
    name: "advertisement-storefront",
    component: () => import("@/views/advertisement/StorefrontHomeView.vue"),
  },
  {
    path: "/ads/preview",
    name: "advertisement-preview",
    component: () =>
      import("@/views/advertisement/AdvertisementPreviewView.vue"),
  },

  // =========================
  // 權限不足頁面
  // =========================
  {
    path: "/forbidden",
    name: "forbidden",
    component: () => import("@/views/error/ForbiddenView.vue"),
  },

  // =========================
  // 404
  // 放最後面
  // =========================
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/views/NotFoundView.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// ======================================================
// Admin Route Guard
// ======================================================

router.beforeEach(async (to) => {
  // 如果這個頁面不需要 ADMIN 權限
  // 直接讓使用者進入
  if (!to.meta.requiresAdmin) {
    return true;
  }

  try {
    // 向後端確認目前 Session 登入的是誰
    const member = await getCurrentMember();

    console.log("目前登入會員：", member);
    console.log("目前角色：", member.role);

    // =========================
    // 有登入，但是不是 ADMIN
    // =========================
    if (member.role !== "ROLE_ADMIN") {
      console.log("不是 ADMIN，禁止進入管理後台");

      return {
        name: "forbidden",
      };
    }

    // =========================
    // 是 ADMIN
    // =========================
    console.log("ADMIN 驗證成功");

    return true;
  } catch (error) {
    // =========================
    // 尚未登入
    // =========================
    console.log("尚未登入，導向登入頁");

    return {
      name: "login",

      query: {
        // 記錄原本想去的頁面
        // 登入成功後未來可以導回去
        redirect: to.fullPath,
      },
    };
  }
});

export default router;
