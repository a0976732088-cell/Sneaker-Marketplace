import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, nextTick, reactive, ref, watch } from "vue";

const navbarSource = await readFile(
  new URL("../src/components/layout/AppNavbar.vue", import.meta.url),
  "utf8",
);
const navbarScript = navbarSource
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "");

function createHarness({ anonymous = false } = {}) {
  const route = reactive({ fullPath: "/", path: "/" });
  let mountedCallback;
  let unmountedCallback;
  let socketOptions;
  let socketActive = false;
  let socketFactoryCount = 0;
  let socketConnectCalls = 0;
  let socketActivations = 0;
  let socketDeactivations = 0;
  let memberRequests = 0;
  let notificationRequests = 0;
  let logoutRequests = 0;
  let nextTimerId = 0;
  const timers = new Map();
  const clearedTimers = [];
  const focusListeners = new Set();
  const routerDestinations = [];

  const privateSocket = {
    connect() {
      socketConnectCalls++;

      if (!socketActive) {
        socketActive = true;
        socketActivations++;
      }
    },
    async disconnect() {
      if (socketActive) {
        socketActive = false;
        socketDeactivations++;
      }
    },
  };

  const context = {
    computed,
    ref,
    watch,
    console: {
      ...console,
      log() {},
      error() {},
    },
    useRoute: () => route,
    useRouter: () => ({
      push: async (destination) => {
        routerDestinations.push(destination);
      },
    }),
    onMounted: (callback) => {
      mountedCallback = callback;
    },
    onUnmounted: (callback) => {
      unmountedCallback = callback;
    },
    getCurrentMember: async () => {
      memberRequests++;

      if (anonymous) {
        throw new Error("not logged in");
      }

      // 每次 route 重讀都回傳新物件，模擬真實 response JSON。
      return { memberId: 7, account: "member-a", role: "ROLE_MEMBER" };
    },
    logout: async () => {
      logoutRequests++;
    },
    httpClient: {
      async get(path) {
        assert.equal(path, "/notification/noticeList");
        notificationRequests++;
        return {
          data: [
            {
              notificationId: notificationRequests,
              title: "競標通知",
              isRead: false,
            },
          ],
        };
      },
      async put() {},
    },
    createAuctionPrivateSocketClient: (options) => {
      socketFactoryCount++;
      socketOptions = options;
      return privateSocket;
    },
    setInterval: (callback, delay) => {
      const id = ++nextTimerId;
      timers.set(id, { callback, delay });
      return id;
    },
    clearInterval: (id) => {
      clearedTimers.push(id);
      timers.delete(id);
    },
    window: {
      addEventListener(event, callback) {
        assert.equal(event, "focus");
        focusListeners.add(callback);
      },
      removeEventListener(event, callback) {
        assert.equal(event, "focus");
        focusListeners.delete(callback);
      },
    },
    alert() {},
  };

  vm.runInNewContext(
    navbarScript +
      `
      globalThis.subject = {
        currentMember, notifications, unreadCount, fetchNotifications,
        handleLogout
      };
    `,
    context,
  );

  async function flush() {
    for (let index = 0; index < 5; index++) {
      await Promise.resolve();
      await nextTick();
    }
  }

  return {
    subject: context.subject,
    route,
    flush,
    mount: () => mountedCallback(),
    unmount: () => unmountedCallback(),
    socketOptions: () => socketOptions,
    socketFactoryCount: () => socketFactoryCount,
    socketConnectCalls: () => socketConnectCalls,
    socketActivations: () => socketActivations,
    socketDeactivations: () => socketDeactivations,
    memberRequests: () => memberRequests,
    notificationRequests: () => notificationRequests,
    logoutRequests: () => logoutRequests,
    timers,
    clearedTimers,
    focusListeners,
    routerDestinations,
  };
}

test("logged-in Navbar creates one idempotent private connection across route member reloads", async () => {
  const h = createHarness();
  await h.flush();

  assert.equal(h.memberRequests(), 1);
  assert.equal(h.socketFactoryCount(), 1);
  assert.equal(h.socketActivations(), 1);
  assert.equal(h.notificationRequests(), 1);

  h.route.fullPath = "/auctions";
  h.route.path = "/auctions";
  await h.flush();

  assert.equal(h.memberRequests(), 2);
  assert.equal(h.socketFactoryCount(), 1);
  assert.equal(h.socketConnectCalls(), 2);
  assert.equal(h.socketActivations(), 1);
});

test("anonymous Navbar does not create or connect a private socket", async () => {
  const h = createHarness({ anonymous: true });
  await h.flush();

  assert.equal(h.memberRequests(), 1);
  assert.equal(h.subject.currentMember.value, null);
  assert.equal(h.socketFactoryCount(), 0);
  assert.equal(h.socketActivations(), 0);
});

test("private message and every socket connect or reconnect refresh notifications through REST", async () => {
  const h = createHarness();
  await h.flush();
  const initialRequests = h.notificationRequests();

  await h.socketOptions().onConnect({});
  assert.equal(h.notificationRequests(), initialRequests + 1);

  await h.socketOptions().onMessage({ eventType: "AUCTION_OUTBID" });
  assert.equal(h.notificationRequests(), initialRequests + 2);

  await h.socketOptions().onConnect({ reconnect: true });
  assert.equal(h.notificationRequests(), initialRequests + 3);
  assert.equal(h.subject.unreadCount.value, 1);
});

test("logout disconnects the private socket and clears Navbar notification state", async () => {
  const h = createHarness();
  await h.flush();
  assert.equal(h.subject.notifications.value.length, 1);

  await h.subject.handleLogout();
  await h.flush();

  assert.equal(h.logoutRequests(), 1);
  assert.equal(h.socketDeactivations(), 1);
  assert.equal(h.subject.currentMember.value, null);
  assert.deepEqual([...h.subject.notifications.value], []);
  assert.deepEqual(h.routerDestinations, ["/"]);
});

test("Navbar keeps 60-second polling and focus refresh, then disconnects on unmount", async () => {
  const h = createHarness();
  await h.flush();
  h.mount();

  assert.equal(h.timers.size, 1);
  const [{ callback, delay }] = h.timers.values();
  assert.equal(delay, 60000);
  assert.equal(h.focusListeners.size, 1);

  const initialRequests = h.notificationRequests();
  await callback();
  assert.equal(h.notificationRequests(), initialRequests + 1);

  const [focusRefresh] = h.focusListeners;
  await focusRefresh();
  assert.equal(h.notificationRequests(), initialRequests + 2);

  h.unmount();
  await h.flush();
  assert.equal(h.timers.size, 0);
  assert.equal(h.clearedTimers.length, 1);
  assert.equal(h.focusListeners.size, 0);
  assert.equal(h.socketDeactivations(), 1);
});

test("Navbar private integration delegates identity to the fixed socket service", () => {
  assert.match(
    navbarSource,
    /createAuctionPrivateSocketClient\(\{[\s\S]*onConnect:\s*fetchNotifications,[\s\S]*onMessage:\s*fetchNotifications/,
  );
  assert.doesNotMatch(navbarSource, /\/user\/\$\{/);
  assert.doesNotMatch(
    navbarSource,
    /createAuctionPrivateSocketClient\([^)]*(memberId|recipientId)/,
  );
});
