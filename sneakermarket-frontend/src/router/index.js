import { createRouter, createWebHistory } from "vue-router";

import HomeView from "@/views/HomeView.vue";

import { getCurrentMember } from "@/service/authService";

const routes = [
  // =====================================================
  // Home
  // =====================================================
  {
    path: "/",

    name: "home",

    component: HomeView,
  },

  // =====================================================
  // Member
  // =====================================================
  {
    path: "/member",

    name: "member",

    component: () => import("@/views/member/MemberHomeView.vue"),
  },

  {
    path: "/member/orders",

    name: "member-orders",

    component: () => import("@/views/member/MemberOrderListView.vue"),
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

  // =====================================================
  // Coupon
  // =====================================================
  {
    path: "/coupons",

    name: "coupons",

    component: () => import("@/views/coupon/CouponView.vue"),
  },

  // =====================================================
  // Commerce
  // =====================================================
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

  // =====================================================
  // Advertisement - 前台
  // =====================================================
  {
    path: "/ads",

    name: "advertisement",

    component: () => import("@/views/advertisement/AdvertisementHomeView.vue"),
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

  // =====================================================
  // Calendar - 前台
  // =====================================================
  {
    path: "/calendar",

    name: "calendar",

    component: () => import("@/views/calendar/CalendarHomeView.vue"),
  },

  // {
  //   path: "/calendar/eventManage",

  //   name: "eventManage",

  //   component: () => import("@/views/calendar/EventManageView.vue"),
  // },

  // =====================================================
  // Auction - Member
  // =====================================================
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

  // =====================================================
  // ADMIN
  //
  // 所有 Admin 頁面共用：
  // AdminLayout + AdminSidebar
  // =====================================================
  {
    path: "/admin",

    component: () => import("@/layouts/AdminLayout.vue"),

    meta: {
      requiresAdmin: true,
    },

    children: [
      // ================================================
      // Admin Home / Member
      // ================================================
      {
        path: "",

        name: "admin",

        component: () => import("@/views/admin/AdminHomeView.vue"),
      },

      // ================================================
      // Product
      // ================================================
      {
        path: "products",

        name: "admin-products",

        component: () => import("@/views/admin/AdminProductListView.vue"),
      },

      {
        path: "products/new",

        name: "admin-product-create",

        component: () => import("@/views/admin/AdminProductFormView.vue"),
      },

      {
        path: "products/:id/edit",

        name: "admin-product-edit",

        component: () => import("@/views/admin/AdminProductFormView.vue"),
      },

      // ================================================
      // Advertisement
      // ================================================
      {
        path: "advertisements",

        name: "admin-advertisements",

        component: () =>
          import("@/views/advertisement/AdvertisementHomeView.vue"),
      },

      // ================================================
      // Calendar / Notification
      // ================================================
      {
        path: "events",

        name: "admin-events",

        component: () => import("@/views/admin/AdminCalendarNotificationView.vue"),
      },
      // ================================================
      // Notification
      // ================================================
      {
        path: "notifications",

        name: "admin-notifications",

        component: () => import("@/views/admin/AdminCalendarNotificationView.vue"),
      },

      // ================================================
      // Auction
      // ================================================
      {
        path: "auctions",

        name: "admin-auctions",

        component: () => import("@/views/admin/AdminAuctionListView.vue"),
      },

      {
        path: "auctions/:id",

        name: "admin-auction-detail",

        component: () => import("@/views/admin/AdminAuctionDetailView.vue"),
      },
    ],
  },

  // =====================================================
  // Forbidden
  // =====================================================
  {
    path: "/forbidden",

    name: "forbidden",

    component: () => import("@/views/error/ForbiddenView.vue"),
  },

  // =====================================================
  // 404
  // 一定放最後
  // =====================================================
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

// =====================================================
// Admin Route Guard
// =====================================================
router.beforeEach(async (to) => {
  // 不需要 Admin 權限
  if (!to.meta.requiresAdmin) {
    return true;
  }

  try {
    const member = await getCurrentMember();

    console.log("目前登入會員：", member);

    console.log("目前角色：", member.role);

    // =============================================
    // 不是 ADMIN
    // =============================================
    if (member.role !== "ROLE_ADMIN") {
      console.log("不是 ADMIN，禁止進入管理後台");

      return {
        name: "forbidden",
      };
    }

    // =============================================
    // ADMIN
    // =============================================
    console.log("ADMIN 驗證成功");

    return true;
  } catch (error) {
    console.log("尚未登入，導向登入頁");

    return {
      name: "login",

      query: {
        redirect: to.fullPath,
      },
    };
  }
});

export default router;
