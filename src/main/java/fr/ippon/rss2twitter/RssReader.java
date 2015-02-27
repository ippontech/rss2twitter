package fr.ippon.rss2twitter;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class RssReader {

    private Log logger = LogFactory.getLog(RssReader.class);

    @Value("${rss.url}")
    private URL feedUrl;

    @Value("${posts.maxAgeInDays}")
    private int maxAgeInDays;

    @Value("${publication.rateInMinutes}")
    private int publicationRateInMinutes;

    @Autowired
    @Resource(name = "redisTemplate")
    private ListOperations<String, LocalDateTime> template;

    @Scheduled(fixedDelay = 10000)
    public void readRss() {
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            feed.getEntries().stream()
                    .map(e -> createPost(e))
                    .filter(post -> retainEntry(post))
                    .findFirst()
                    .ifPresent(post -> publishPost(post));
            feed.getEntries().stream()
                    .map(e -> createPost(e))
                    .filter(post -> retainEntry(post))
                    .forEach(post -> doSomething(post));
        } catch (FeedException | IOException ex) {
            logger.error("Failed reading RSS feed from " + feedUrl, ex);
            throw new RuntimeException(ex);
        }
    }

    private void publishPost(Post post) {
        String key = "post:" + post.getId() + ":publicationDates";
        Long aLong = template.leftPush(key, LocalDateTime.now());
        System.out.println(aLong);
    }

    private Post createPost(SyndEntry e) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(e.getLink())
                .replaceQueryParam("utm_source", null)
                .replaceQueryParam("utm_medium", null)
                .replaceQueryParam("utm_campaign", null);
        String link = uri.toUriString();
        String id = link.substring(link.indexOf('/', link.indexOf('/', link.indexOf('/') + 1) + 1));

        Instant instant = Instant.ofEpochMilli(e.getPublishedDate().getTime());
        LocalDateTime publicationDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        return new Post(id, link, e.getTitle(), publicationDate);
    }

    private void doSomething(Post post) {
        System.out.println(post.getTitle() + " -- " + post.getLink());

        String key = "post:" + post.getId() + ":publicationDates";
        LocalDateTime lastPublicationDate = template.rightPop(key);
        System.out.println(lastPublicationDate);
    }

    private boolean retainEntry(Post post) {
        Duration age = Duration.between(post.getPublicationDate(), LocalDateTime.now());
        return age.toDays() <= maxAgeInDays;
    }

    private String cleanUrl(String url) {
        return UriComponentsBuilder.fromUriString(url)
                .replaceQueryParam("utm_source", null)
                .replaceQueryParam("utm_medium", null)
                .replaceQueryParam("utm_campaign", null)
                .toUriString();
    }

}
