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
    path: "/products",
    name: "commerce",
    component: () => import("@/views/commerce/CommerceHomeView.vue"),
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
    path: "/notifications",
    name: "notification",
    component: () => import("@/views/notification/NotificationHomeView.vue"),
  },
  {
    path: "/auctions",
    name: "auction",
    component: () => import("@/views/auction/AuctionListView.vue"),
  },
  {
    path: "/auctions/:id",
    name: "auction-detail",
    component: () => import("@/views/auction/AuctionDetailView.vue"),
  },
  {
    path: "/admin",
    name: "admin",
    component: () => import("@/views/admin/AdminHomeView.vue"),
  },
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

export default router;
