import { Client } from "@stomp/stompjs";

import {
  AUCTION_SOCKET_RECONNECT_DELAY_MS,
  resolveAuctionWebSocketUrl,
} from "./auctionSocketService.js";

export const AUCTION_PRIVATE_SOCKET_DESTINATION =
  "/user/queue/auction-events";
export const AUCTION_PRIVATE_SOCKET_RECONNECT_DELAY_MS =
  AUCTION_SOCKET_RECONNECT_DELAY_MS;

/**
 * 建立 Navbar 使用的會員私人競標通知連線。
 *
 * 會員身分只由 /ws handshake 隨附的 HttpSession cookie 決定；前端不傳送
 * member id，也不自行組合會員專屬 destination。STOMP reconnect 成功後會
 * 自動重新訂閱固定的 user destination。
 */
export function createAuctionPrivateSocketClient({
  webSocketUrl = resolveAuctionWebSocketUrl(),
  reconnectDelay = AUCTION_PRIVATE_SOCKET_RECONNECT_DELAY_MS,
  onConnect = () => {},
  onMessage = () => {},
  onDisconnect = () => {},
  onError = () => {},
  clientFactory = (configuration) => new Client(configuration),
} = {}) {
  let active = false;
  let deactivating = false;
  let subscription = null;

  function reportError(error) {
    try {
      onError(error);
    } catch (callbackError) {
      console.error("競標私人通知連線錯誤處理失敗：", callbackError);
    }
  }

  function invokeCallback(callback, value) {
    try {
      const result = callback(value);

      if (result && typeof result.catch === "function") {
        result.catch(reportError);
      }
    } catch (error) {
      reportError(error);
    }
  }

  function handleMessage(frame) {
    let message;

    try {
      message = JSON.parse(frame.body);
    } catch (error) {
      reportError(error);
      return;
    }

    invokeCallback(onMessage, message);
  }

  function unsubscribeCurrent() {
    const currentSubscription = subscription;
    subscription = null;
    currentSubscription?.unsubscribe();
  }

  function subscribePrivateEvents() {
    if (!active || !client.connected) {
      return;
    }

    unsubscribeCurrent();
    subscription = client.subscribe(
      AUCTION_PRIVATE_SOCKET_DESTINATION,
      handleMessage,
    );
  }

  function notifyDisconnected(event) {
    subscription = null;

    if (active && !deactivating) {
      invokeCallback(onDisconnect, event);
    }
  }

  const client = clientFactory({
    brokerURL: webSocketUrl,
    reconnectDelay,
    onConnect(frame) {
      if (!active) {
        return;
      }

      deactivating = false;
      subscribePrivateEvents();
      invokeCallback(onConnect, frame);
    },
    onDisconnect: notifyDisconnected,
    onWebSocketClose: notifyDisconnected,
    onStompError: reportError,
    onWebSocketError: reportError,
  });

  async function disconnect() {
    if (!active && !subscription) {
      return;
    }

    active = false;
    deactivating = true;
    let unsubscribeError = null;

    try {
      unsubscribeCurrent();
    } catch (error) {
      unsubscribeError = error;
    }

    await client.deactivate();

    if (unsubscribeError) {
      throw unsubscribeError;
    }
  }

  return {
    get active() {
      return active;
    },

    get connected() {
      return Boolean(client.connected);
    },

    get webSocketUrl() {
      return webSocketUrl;
    },

    connect() {
      if (active) {
        return;
      }

      active = true;
      deactivating = false;

      try {
        client.activate();
      } catch (error) {
        active = false;
        reportError(error);
      }
    },

    disconnect,
    deactivate: disconnect,
  };
}
