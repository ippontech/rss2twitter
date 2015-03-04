package fr.ippon.rss2twitter.repository;

import fr.ippon.rss2twitter.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PostRepository {

    @Autowired
    private Jedis jedis;

    private static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public Post retrievePublicationDetails(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        Optional.ofNullable(jedis.lindex(keyDates, 0))
                .map(str -> LocalDateTime.parse(str, DATE_FORMATTER))
                .ifPresent(v -> post.setLastPublicationDate(v));

        String keyCount = "post:" + post.getId() + ":publicationCount";
        Optional.ofNullable(jedis.get(keyCount))
                .map(str -> Integer.valueOf(str))
                .ifPresent(v -> post.setPublicationCount(v));

        return post;
    }

    public Optional<LocalDateTime> getLastPublicationDate(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        return Optional.ofNullable(jedis.lindex(keyDates, 0))
                .map(str -> LocalDateTime.parse(str, DATE_FORMATTER));
    }

    public void markPostAsPublished(Post post, LocalDateTime date) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        String value = DATE_FORMATTER.format(date);
        jedis.lpush(keyDates, value);

        String keyCount = "post:" + post.getId() + ":publicationCount";
        jedis.incr(keyCount);
    }

    public LocalDateTime getLastPublicationDate() {
        String key = "lastPublicationDate";
        return Optional.ofNullable(jedis.get(key))
                .map(str -> LocalDateTime.parse(str, DATE_FORMATTER))
                .orElse(null);
    }

    public void setLastPublicationDate(LocalDateTime date) {
        String key = "lastPublicationDate";
        String value = DATE_FORMATTER.format(date);
        jedis.set(key, value);
    }

    public List<LocalDateTime> getPublicationDates(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        return jedis.lrange(keyDates, 0, -1)
                .stream()
                .map(str -> LocalDateTime.parse(str, DATE_FORMATTER))
                .collect(Collectors.toList());
    }

    public String getPublicationText(Post post) {
        String key = "post:" + post.getId() + ":publicationText";
        return jedis.get(key);
    }

    public void setPublicationText(Post post, String text) {
        String key = "post:" + post.getId() + ":publicationText";
        jedis.set(key, text);
    }
}
