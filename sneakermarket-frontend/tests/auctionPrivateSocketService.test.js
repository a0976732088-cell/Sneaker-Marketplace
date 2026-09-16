import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  AUCTION_PRIVATE_SOCKET_DESTINATION,
  AUCTION_PRIVATE_SOCKET_RECONNECT_DELAY_MS,
  createAuctionPrivateSocketClient,
} from "../src/service/auctionPrivateSocketService.js";

test("private STOMP client is idempotent, reconnects and uses only the fixed user destination", async () => {
  let configuration;
  let activateCount = 0;
  let deactivateCount = 0;
  let connectedCount = 0;
  let disconnectedCount = 0;
  const messages = [];
  const errors = [];
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

  const socket = createAuctionPrivateSocketClient({
    webSocketUrl: "ws://localhost:8080/ws",
    onConnect: () => {
      connectedCount++;
    },
    onMessage: (message) => messages.push(message),
    onDisconnect: () => {
      disconnectedCount++;
    },
    onError: (error) => errors.push(error),
    clientFactory: (options) => {
      configuration = options;
      return fakeClient;
    },
  });

  assert.equal(
    AUCTION_PRIVATE_SOCKET_DESTINATION,
    "/user/queue/auction-events",
  );
  assert.equal(AUCTION_PRIVATE_SOCKET_RECONNECT_DELAY_MS, 5000);
  assert.equal(configuration.brokerURL, "ws://localhost:8080/ws");
  assert.equal(configuration.reconnectDelay, 5000);

  socket.connect();
  socket.connect();
  assert.equal(activateCount, 1);
  assert.equal(socket.active, true);
  assert.equal(subscriptions.length, 0);

  fakeClient.connected = true;
  configuration.onConnect({});
  assert.equal(connectedCount, 1);
  assert.equal(subscriptions.length, 1);
  assert.equal(
    subscriptions[0].destination,
    "/user/queue/auction-events",
  );

  subscriptions[0].callback({
    body: JSON.stringify({
      eventType: "AUCTION_OUTBID",
      auctionId: 123,
      notificationId: 987,
    }),
  });
  assert.deepEqual(messages, [
    {
      eventType: "AUCTION_OUTBID",
      auctionId: 123,
      notificationId: 987,
    },
  ]);

  fakeClient.connected = false;
  configuration.onWebSocketClose({ reason: "network" });
  assert.equal(disconnectedCount, 1);

  // STOMP 自己負責 reconnect；route/member 重讀不得再次 activate。
  socket.connect();
  assert.equal(activateCount, 1);

  fakeClient.connected = true;
  configuration.onConnect({});
  assert.equal(connectedCount, 2);
  assert.equal(subscriptions.length, 2);
  assert.equal(
    subscriptions[1].destination,
    "/user/queue/auction-events",
  );

  await socket.disconnect();
  assert.equal(subscriptions[1].unsubscribeCount, 1);
  assert.equal(deactivateCount, 1);
  assert.equal(disconnectedCount, 1);
  assert.equal(socket.active, false);

  await socket.deactivate();
  assert.equal(deactivateCount, 1);
  assert.equal(errors.length, 0);
});

test("private STOMP parsing, transport and async callback failures reach onError", async () => {
  let configuration;
  const errors = [];
  const fakeClient = {
    connected: true,
    activate() {},
    async deactivate() {},
    subscribe(_destination, callback) {
      this.callback = callback;
      return { unsubscribe() {} };
    },
  };

  const socket = createAuctionPrivateSocketClient({
    onMessage: async () => {
      throw new Error("notification refresh failed");
    },
    onError: (error) => errors.push(error),
    clientFactory: (options) => {
      configuration = options;
      return fakeClient;
    },
  });

  socket.connect();
  configuration.onConnect({});
  fakeClient.callback({ body: "{invalid json" });
  fakeClient.callback({ body: JSON.stringify({ eventType: "AUCTION_WON" }) });
  configuration.onStompError({ body: "broker failed" });
  configuration.onWebSocketError(new Error("transport failed"));

  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(errors.length, 4);
});

test("private socket always deactivates when subscription cleanup throws", async () => {
  let deactivateCount = 0;
  const unsubscribeError = new Error("unsubscribe failed");
  const fakeClient = {
    connected: true,
    activate() {},
    async deactivate() {
      deactivateCount++;
    },
    subscribe() {
      return {
        unsubscribe() {
          throw unsubscribeError;
        },
      };
    },
  };

  let configuration;
  const socket = createAuctionPrivateSocketClient({
    clientFactory: (options) => {
      configuration = options;
      return fakeClient;
    },
  });
  socket.connect();
  configuration.onConnect({});

  await assert.rejects(socket.disconnect(), unsubscribeError);
  assert.equal(deactivateCount, 1);
});

test("private socket source contains no client-selected member destination or identity header", async () => {
  const source = await readFile(
    new URL("../src/service/auctionPrivateSocketService.js", import.meta.url),
    "utf8",
  );

  assert.doesNotMatch(source, /\/user\/\$\{/);
  assert.doesNotMatch(source, /memberId|recipientId/);
  assert.doesNotMatch(source, /connectHeaders|subscribeHeaders/);
});
