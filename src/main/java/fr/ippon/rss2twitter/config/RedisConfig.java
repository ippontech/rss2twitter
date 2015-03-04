package fr.ippon.rss2twitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

@Configuration
public class RedisConfig {

    @Bean
    public Jedis jedis(@Value("${redis.host}") String host, @Value("${redis.port}") int port) {
        return new Jedis(host, port);
    }
}
