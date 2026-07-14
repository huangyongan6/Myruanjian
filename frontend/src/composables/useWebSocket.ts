import { ref, onUnmounted } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
// @ts-expect-error sockjs-client 缺少导出的类型定义，但运行时存在 default 导出
import SockJS from 'sockjs-client/dist/sockjs'

/**
 * WebSocket 封装（STOMP over SockJS）。
 *
 * <p>对应 CLAUDE.md §8.1：
 * <ul>
 *   <li>指数退避自动重连（最多 5 次）</li>
 *   <li>组件卸载时自动断开</li>
 *   <li>提供 connect / subscribe / send / disconnect 四个动作</li>
 * </ul>
 */
export function useWebSocket(url: string) {
  const connected = ref(false)
  const reconnecting = ref(false)
  const lastError = ref<string | null>(null)
  const subscriptions = new Map<string, { id: string; callback: (msg: IMessage) => void }>()

  let client: Client | null = null
  let retry = 0
  const MAX_RETRY = 5
  /** 未连接时积压的待订阅请求（destination → callback），连接成功后统一执行 */
  const pendingSubscriptions = new Map<string, (msg: IMessage) => void>()

  function buildClient(): Client {
    return new Client({
      webSocketFactory: () => new SockJS(url) as unknown as WebSocket,
      reconnectDelay: 0, // 我们自行管理重连
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      onConnect: () => {
        connected.value = true
        reconnecting.value = false
        retry = 0
        // 重连后恢复所有订阅
        for (const [destination, sub] of subscriptions.entries()) {
          sub.id = client!.subscribe(destination, sub.callback).id
        }
        // 立即执行所有积压的待订阅请求
        for (const [destination, callback] of pendingSubscriptions.entries()) {
          if (!subscriptions.has(destination)) {
            const sub = client!.subscribe(destination, callback)
            subscriptions.set(destination, { id: sub.id, callback })
          }
        }
        pendingSubscriptions.clear()
      },
      onWebSocketClose: () => {
        connected.value = false
        scheduleReconnect()
      },
      onStompError: (frame) => {
        lastError.value = frame.headers['message'] || 'STOMP 错误'
        scheduleReconnect()
      },
      onWebSocketError: (event) => {
        lastError.value = (event as Event).type || 'WebSocket 错误'
      }
    })
  }

  function scheduleReconnect(): void {
    if (retry >= MAX_RETRY) {
      reconnecting.value = false
      return
    }
    reconnecting.value = true
    const delay = Math.min(1000 * 2 ** retry, 16_000)
    retry += 1
    setTimeout(() => {
      if (client && !client.connected) {
        client.activate()
      }
    }, delay)
  }

  function connect(): void {
    if (client) return
    client = buildClient()
    client.activate()
  }

  function subscribe(destination: string, callback: (msg: IMessage) => void): void {
    subscriptions.set(destination, { id: '', callback })
    if (client?.connected) {
      const sub = client.subscribe(destination, callback)
      subscriptions.get(destination)!.id = sub.id
    } else {
      // 未连接时，缓存到待执行队列，连接成功后自动订阅
      pendingSubscriptions.set(destination, callback)
    }
  }

  /**
   * 取消订阅单个目的地：用于切换上下文 / 组件卸装时清理指定订阅，
   * 而非断开整个 STOMP 连接（其他组件可能仍在用）。
   *
   * <p>同时清理该目的地在待执行回调队列中的订阅请求。
   */
  function unsubscribe(destination: string): void {
    const sub = subscriptions.get(destination)
    if (sub && sub.id && client) {
      try {
        client.unsubscribe(sub.id)
      } catch {
        // 静默
      }
    }
    subscriptions.delete(destination)
    // 清理积压在 pendingSubscriptions 中的该目的地订阅请求
    pendingSubscriptions.delete(destination)
  }

  function send(destination: string, body: unknown): void {
    if (!client?.connected) return
    client.publish({
      destination,
      body: typeof body === 'string' ? body : JSON.stringify(body)
    })
  }

  function disconnect(): void {
    for (const sub of subscriptions.values()) {
      if (sub.id && client) {
        try {
          client.unsubscribe(sub.id)
        } catch {
          // 静默
        }
      }
    }
    subscriptions.clear()
    if (client) {
      client.deactivate()
      client = null
    }
    connected.value = false
  }

  onUnmounted(() => disconnect())

  return {
    connected,
    reconnecting,
    lastError,
    connect,
    subscribe,
    unsubscribe,
    send,
    disconnect
  }
}
