package com.hezhong.hezhongskywars.utils.cross;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class RedisMessageManager {
    private final String host;
    private final int port;
    private final String password;
    private final String channel;
    private final Logger logger;

    private JedisPool pool;
    private volatile boolean running = false;
    private Thread subThread;
    private Consumer<String> onMessage;

    public RedisMessageManager(String host, int port, String password, String channel, Logger logger) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.channel = channel;
        this.logger = logger;
    }

    public void start(Consumer<String> onMessage) {
        this.onMessage = onMessage;
        running = true;
        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .password(password == null || password.isEmpty() ? null : password)
                .ssl(true) // Redis Cloud 等云端实例需要TLS
                .connectionTimeoutMillis(5000)
                .socketTimeoutMillis(5000)
                .build();
        pool = new JedisPool(new HostAndPort(host, port), config);
        subThread = new Thread(this::subscribeLoop, "HSW-Redis-Sub");
        subThread.setDaemon(true);
        subThread.start();
    }

    public void publish(String message) {
        if (pool == null) return;
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            logger.warning("HSW Redis publish failed: " + e.getMessage());
        }
    }

    // 订阅是阻塞的，断线后自动重连
    private void subscribeLoop() {
        while (running) {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String ch, String message) {
                        Consumer<String> handler = RedisMessageManager.this.onMessage;
                        if (handler != null) {
                            try {
                                handler.accept(message);
                            } catch (Exception e) {
                                logger.warning("HSW Redis onMessage error: " + e.getMessage());
                            }
                        }
                    }
                }, channel);
            } catch (Exception e) {
                if (running) {
                    logger.warning("HSW Redis disconnected, retrying in 3s... " + e.getMessage());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    public void stop() {
        running = false;
        if (subThread != null) subThread.interrupt();
        if (pool != null) pool.close();
    }
}
