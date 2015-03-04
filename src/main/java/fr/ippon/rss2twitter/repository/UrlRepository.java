package fr.ippon.rss2twitter.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class UrlRepository {

    // one month
    private static final int EXPIRATION_IN_SECONDS = 30 * 24 * 60 * 60;

    @Autowired
    private Jedis jedis;

    public String getExpandedUrl(String minifiedUrl) {
        return jedis.get("url:" + minifiedUrl);
    }

    public void setExpandedUrl(String minifiedUrl, String expandedUrl) {
        String key = "url:" + minifiedUrl;
        jedis.set(key, expandedUrl);
        jedis.expire(key, EXPIRATION_IN_SECONDS);
    }
}
