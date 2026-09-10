import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";

const routes = [
  {
    path: "/",
    name: "home",
    component: HomeView,
  },
  {
    path: "/system-check",
    name: "system-check",
    component: () => import("@/views/SystemCheckView.vue"),
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
  { path: '/checkout', 
    name: 'checkout', 
    component: () => import('@/views/commerce/CheckoutView.vue'),
  },
  { path: '/orders/:id', 
    name: 'order-confirmation', 
    component: () => import('@/views/commerce/OrderConfirmationView.vue'),
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
    path: "/auctions/:id",
    name: "auction-detail",
    component: () => import("@/views/auction/AuctionDetailView.vue"),
  },
  {
    path: "/auctions/:id/edit",
    name: "auction-edit",
    component: () => import("@/views/auction/AuctionEditView.vue"),
  },
  {
    path: "/admin",
    name: "admin",
    component: () => import("@/views/admin/AdminHomeView.vue"),
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
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/views/NotFoundView.vue"),
  },
  {
    path: "/forbidden",
    name: "forbidden",
    component: () => import("@/views/error/ForbiddenView.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
