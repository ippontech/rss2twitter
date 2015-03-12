package fr.ippon.rss2twitter.repository;

import fr.ippon.rss2twitter.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PostRepository {

    @Autowired
    private Jedis jedis;

    private static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static ZonedDateTime parseDate(String dateStr) {
        return ZonedDateTime.parse(dateStr, DATE_FORMATTER);
    }

    private static String formatDate(ZonedDateTime date) {
        return date.truncatedTo(ChronoUnit.SECONDS).format(DATE_FORMATTER);
    }

    public Post retrievePublicationDetails(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        Optional.ofNullable(jedis.lindex(keyDates, 0))
                .map(str -> parseDate(str))
                .ifPresent(v -> post.setLastPublicationDate(v));

        String keyCount = "post:" + post.getId() + ":publicationCount";
        Optional.ofNullable(jedis.get(keyCount))
                .map(str -> Integer.valueOf(str))
                .ifPresent(v -> post.setPublicationCount(v));

        String keyMaxPublicationDate = "post:" + post.getId() + ":maxPublicationDate";
        Optional.ofNullable(jedis.get(keyMaxPublicationDate))
                .map(str -> parseDate(str))
                .ifPresent(v -> post.setMaxPublicationDate(v));

        return post;
    }

    public Optional<ZonedDateTime> getLastPublicationDate(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        return Optional.ofNullable(jedis.lindex(keyDates, 0))
                .map(str -> parseDate(str));
    }

    public void markPostAsPublished(Post post, ZonedDateTime date) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        String value = formatDate(date);
        jedis.lpush(keyDates, value);

        String keyCount = "post:" + post.getId() + ":publicationCount";
        jedis.incr(keyCount);
    }

    public ZonedDateTime getLastPublicationDate() {
        String key = "lastPublicationDate";
        return Optional.ofNullable(jedis.get(key))
                .map(str -> parseDate(str))
                .orElse(null);
    }

    public void setLastPublicationDate(ZonedDateTime date) {
        String key = "lastPublicationDate";
        String value = formatDate(date);
        jedis.set(key, value);
    }

    public List<ZonedDateTime> getPublicationDates(Post post) {
        String keyDates = "post:" + post.getId() + ":publicationDates";
        return jedis.lrange(keyDates, 0, -1)
                .stream()
                .map(str -> parseDate(str))
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
