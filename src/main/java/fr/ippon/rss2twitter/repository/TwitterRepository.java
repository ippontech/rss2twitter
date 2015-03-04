package fr.ippon.rss2twitter.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class TwitterRepository {

    @Autowired
    private Jedis jedis;

    public String getAccessToken() {
        return jedis.get("twitter:accessToken");
    }
    public String setAccessToken(String accessToken) {
        return jedis.set("twitter:accessToken", accessToken);
    }

    public String getAccessTokenSecret() {
        return jedis.get("twitter:accessTokenSecret");
    }
    public String setAccessTokenSecret(String accessTokenSecret) {
        return jedis.set("twitter:accessTokenSecret", accessTokenSecret);
    }
}
