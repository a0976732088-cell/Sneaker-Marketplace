import test from "node:test";
import assert from "node:assert/strict";

import {
  AUCTION_SOCKET_RECONNECT_DELAY_MS,
  createAuctionSocketClient,
  resolveAuctionWebSocketUrl,
} from "../src/service/auctionSocketService.js";

test("WebSocket URL prefers VITE_WS_URL and normalizes HTTP protocols", () => {
  assert.equal(
    resolveAuctionWebSocketUrl({
      webSocketUrl: "https://realtime.example.com/ws",
      apiBaseUrl: "http://ignored.example.com/api",
    }),
    "wss://realtime.example.com/ws",
  );
});

test("WebSocket URL is derived from local and production API base URLs", () => {
  assert.equal(
    resolveAuctionWebSocketUrl({
      webSocketUrl: "",
      apiBaseUrl: "http://localhost:8080/api",
    }),
    "ws://localhost:8080/ws",
  );
  assert.equal(
    resolveAuctionWebSocketUrl({
      webSocketUrl: "",
      apiBaseUrl: "/backend/api/",
      origin: "https://market.example.com",
    }),
    "wss://market.example.com/backend/ws",
  );
});

test("STOMP client subscribes, parses messages, reconnects and cleans up", async () => {
  let configuration;
  let activateCount = 0;
  let deactivateCount = 0;
  let connectedCount = 0;
  let disconnectedCount = 0;
  const errors = [];
  const messages = [];
  const subscriptions = [];

  const fakeClient = {
    connected: false,
    activate() {
      activateCount++;
    },
    async deactivate() {
      deactivateCount++;
      configuration.onWebSocketClose({ reason: "client deactivate" });
    },
    subscribe(destination, callback) {
      const record = {
        destination,
        callback,
        unsubscribeCount: 0,
        unsubscribe() {
          this.unsubscribeCount++;
        },
      };
      subscriptions.push(record);
      return record;
    },
  };

  const socket = createAuctionSocketClient({
    webSocketUrl: "ws://localhost:8080/ws",
    onConnected: () => {
      connectedCount++;
    },
    onDisconnected: () => {
      disconnectedCount++;
    },
    onError: (error) => {
      errors.push(error);
    },
    clientFactory: (options) => {
      configuration = options;
      return fakeClient;
    },
  });

  assert.equal(configuration.reconnectDelay, AUCTION_SOCKET_RECONNECT_DELAY_MS);
  assert.equal(configuration.brokerURL, "ws://localhost:8080/ws");

  const unsubscribe = socket.subscribeAuction(123, {
    onMessage: (message) => messages.push(message),
  });
  socket.activate();
  assert.equal(activateCount, 1);
  assert.equal(subscriptions.length, 0);

  fakeClient.connected = true;
  configuration.onConnect({});
  assert.equal(connectedCount, 1);
  assert.equal(subscriptions[0].destination, "/topic/auctions/123");

  subscriptions[0].callback({
    body: JSON.stringify({
      eventType: "BID_PLACED",
      data: { auctionId: 123, currentPrice: 6500 },
    }),
  });
  assert.equal(messages.length, 1);
  assert.equal(messages[0].data.currentPrice, 6500);

  subscriptions[0].callback({ body: "{invalid json" });
  assert.equal(errors.length, 1);

  fakeClient.connected = false;
  configuration.onWebSocketClose({ reason: "network" });
  assert.equal(disconnectedCount, 1);

  fakeClient.connected = true;
  configuration.onConnect({});
  assert.equal(connectedCount, 2);
  assert.equal(subscriptions.length, 2);
  assert.equal(subscriptions[1].destination, "/topic/auctions/123");

  unsubscribe();
  assert.equal(subscriptions[1].unsubscribeCount, 1);

  await socket.deactivate();
  assert.equal(deactivateCount, 1);
  assert.equal(disconnectedCount, 1);
});

test("STOMP transport and async handler failures reach the error callback", async () => {
  let configuration;
  const errors = [];
  const fakeClient = {
    connected: true,
    activate() {},
    deactivate: async () => {},
    subscribe(_destination, callback) {
      this.callback = callback;
      return { unsubscribe() {} };
    },
  };

  const socket = createAuctionSocketClient({
    onError: (error) => errors.push(error),
    clientFactory: (options) => {
      configuration = options;
      return fakeClient;
    },
  });

  socket.subscribeAuction(9, {
    onMessage: async () => {
      throw new Error("message handler failed");
    },
  });
  configuration.onConnect({});
  fakeClient.callback({ body: JSON.stringify({ eventType: "BID_PLACED" }) });
  configuration.onStompError({ body: "broker failed" });
  configuration.onWebSocketError(new Error("transport failed"));

  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(errors.length, 3);
});

test("underlying STOMP deactivates even when subscription unsubscribe throws", async () => {
  let underlyingDeactivateCount = 0;
  const unsubscribeError = new Error("unsubscribe failed");
  const fakeClient = {
    connected: true,
    activate() {},
    async deactivate() {
      underlyingDeactivateCount++;
    },
    subscribe() {
      return {
        unsubscribe() {
          throw unsubscribeError;
        },
      };
    },
  };

  const socket = createAuctionSocketClient({
    clientFactory: () => fakeClient,
  });
  socket.subscribeAuction(10, { onMessage() {} });

  await assert.rejects(socket.deactivate(), unsubscribeError);
  assert.equal(underlyingDeactivateCount, 1);
});
