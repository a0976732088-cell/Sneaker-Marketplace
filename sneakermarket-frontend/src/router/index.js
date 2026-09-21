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

  {
    path: "/notifications/preferences",

    name: "notification-preferences",

    component: () => import("@/views/notification/NotificationHomeView.vue"),
  },

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
    path: "/member/auctions/bids",

    name: "auction-my-bids",

    component: () => import("@/views/auction/AuctionMyBidsView.vue"),
  },

  {
    path: "/member/auctions/listings",

    name: "auction-my-listings",

    component: () => import("@/views/auction/AuctionMyListingsView.vue"),
  },

  {
    path: "/member/auctions/orders",

    name: "auction-orders",

    component: () => import("@/views/auction/AuctionOrderListView.vue"),
  },

  {
    path: "/auctions/me/bids",

    redirect: { name: "auction-my-bids" },
  },

  {
    path: "/auctions/me/listings",

    redirect: { name: "auction-my-listings" },
  },

  {
    path: "/auction-orders",

    redirect: { name: "auction-orders" },
  },

  {
    path: "/member/auctions/orders/:orderNumber/checkout",

    name: "auction-order-checkout",

    component: () => import("@/views/auction/AuctionOrderCheckoutView.vue"),
  },

  {
    path: "/member/auctions/orders/:orderNumber",

    name: "auction-order-detail",

    component: () => import("@/views/auction/AuctionOrderDetailView.vue"),
  },

  {
    path: "/auction-orders/:orderNumber/payment",

    name: "auction-order-payment",

    redirect: (to) => ({
      name: "auction-order-checkout",
      params: { orderNumber: to.params.orderNumber },
    }),
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
        name: "admin-activity-overview",
        component: () =>
          import("@/views/admin/calendarAndNotification/AdminActivityOverviewView.vue"),
      },
      {
        path: "events/manage",
        name: "admin-events",
        component: () =>
          import("@/views/admin/calendarAndNotification/AdminActivityManagementView.vue"),
      },
      {
        path: "notifications",
        name: "admin-notifications",
        component: () =>
          import("@/views/admin/calendarAndNotification/AdminNotificationCenterView.vue"),
      },
      {
        path: "notifications/logs",
        name: "admin-notification-logs",
        component: () =>
          import("@/views/admin/calendarAndNotification/AdminNotificationLogView.vue"),
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

    if (member.role !== "ROLE_ADMIN") {

      return {
        name: "forbidden",
      };
    }

    return true;
  } catch (error) {

    return {
      name: "login",

      query: {
        redirect: to.fullPath,
      },
    };
  }
});

export default router;
