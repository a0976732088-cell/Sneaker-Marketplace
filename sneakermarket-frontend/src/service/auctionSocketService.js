import { Client } from "@stomp/stompjs";

export const AUCTION_SOCKET_RECONNECT_DELAY_MS = 5000;

const DEFAULT_API_BASE_URL = "http://localhost:8080/api";

function asWebSocketUrl(value, origin) {
  const url = new URL(value, origin);

  if (url.protocol === "http:") {
    url.protocol = "ws:";
  } else if (url.protocol === "https:") {
    url.protocol = "wss:";
  }

  return url;
}

/**
 * 優先使用 VITE_WS_URL；未設定時由 REST API Base URL 推導 Backend /ws。
 * 相對路徑部署時會沿用目前頁面的 host 與 http/https 協定。
 */
export function resolveAuctionWebSocketUrl({
  webSocketUrl = import.meta.env?.VITE_WS_URL,
  apiBaseUrl = import.meta.env?.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
  origin = globalThis.location?.origin || "http://localhost:5173",
} = {}) {
  const configuredUrl = String(webSocketUrl || "").trim();

  if (configuredUrl) {
    return asWebSocketUrl(configuredUrl, origin).toString();
  }

  const url = asWebSocketUrl(apiBaseUrl, origin);
  const backendPath = url.pathname.replace(/\/api\/?$/, "").replace(/\/$/, "");

  url.pathname = `${backendPath}/ws`;
  url.search = "";
  url.hash = "";

  return url.toString();
}

/**
 * 建立單一競標詳細頁使用的 STOMP client。
 *
 * subscribeAuction 會記住訂閱目標，STOMP reconnect 成功後自動重新訂閱。
 * deactivate 則會停止 reconnect 並清除目前訂閱。
 */
export function createAuctionSocketClient({
  webSocketUrl = resolveAuctionWebSocketUrl(),
  reconnectDelay = AUCTION_SOCKET_RECONNECT_DELAY_MS,
  onConnected = () => {},
  onDisconnected = () => {},
  onError = () => {},
  clientFactory = (configuration) => new Client(configuration),
} = {}) {
  let subscribedAuctionId = null;
  let subscription = null;
  let messageHandler = null;
  let deactivating = false;

  function reportError(error) {
    try {
      onError(error);
    } catch (callbackError) {
      console.error("競標即時連線錯誤處理失敗：", callbackError);
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

    try {
      const result = messageHandler?.(message, frame);

      if (result && typeof result.catch === "function") {
        result.catch(reportError);
      }
    } catch (error) {
      reportError(error);
    }
  }

  function unsubscribeCurrent() {
    const currentSubscription = subscription;
    subscription = null;
    currentSubscription?.unsubscribe();
  }

  function subscribeCurrentAuction() {
    if (!client.connected || subscribedAuctionId === null) {
      return;
    }

    unsubscribeCurrent();
    subscription = client.subscribe(
      `/topic/auctions/${encodeURIComponent(subscribedAuctionId)}`,
      handleMessage,
    );
  }

  function notifyDisconnected(event) {
    subscription = null;

    if (!deactivating) {
      invokeCallback(onDisconnected, event);
    }
  }

  const client = clientFactory({
    brokerURL: webSocketUrl,
    reconnectDelay,
    onConnect(frame) {
      deactivating = false;
      subscribeCurrentAuction();
      invokeCallback(onConnected, frame);
    },
    onDisconnect: notifyDisconnected,
    onWebSocketClose: notifyDisconnected,
    onStompError: reportError,
    onWebSocketError: reportError,
  });

  return {
    get connected() {
      return Boolean(client.connected);
    },

    get webSocketUrl() {
      return webSocketUrl;
    },

    activate() {
      deactivating = false;
      client.activate();
    },

    subscribeAuction(auctionId, { onMessage } = {}) {
      if (auctionId === null || auctionId === undefined || auctionId === "") {
        throw new TypeError("auctionId is required");
      }

      subscribedAuctionId = String(auctionId);
      messageHandler = onMessage;
      subscribeCurrentAuction();

      let active = true;

      return () => {
        if (!active) {
          return;
        }

        active = false;
        subscribedAuctionId = null;
        messageHandler = null;
        unsubscribeCurrent();
      };
    },

    async deactivate() {
      deactivating = true;
      let unsubscribeError = null;

      try {
        unsubscribeCurrent();
      } catch (error) {
        unsubscribeError = error;
      } finally {
        subscribedAuctionId = null;
        messageHandler = null;
      }

      await client.deactivate();

      if (unsubscribeError) {
        throw unsubscribeError;
      }
    },
  };
}
