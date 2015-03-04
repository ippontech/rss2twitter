package fr.ippon.rss2twitter.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import fr.ippon.rss2twitter.entity.Post;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RssService {

    private Log logger = LogFactory.getLog(getClass());

    @Value("${rss.url}")
    private URL feedUrl;

    public List<Post> readRss() {
        logger.debug("Reading RSS feed");

        SyndFeed feed;
        try {
            SyndFeedInput input = new SyndFeedInput();
            feed = input.build(new XmlReader(feedUrl));
        } catch (FeedException | IOException ex) {
            logger.error("Failed reading RSS feed from " + feedUrl, ex);
            throw new RuntimeException(ex);
        }

        List<Post> posts = feed.getEntries().stream()
                .map(e -> createPost(e))
                .collect(Collectors.toList());

        logger.debug("Done reading RSS feed");
        return posts;
    }

    private Post createPost(SyndEntry e) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(e.getLink())
                .replaceQueryParam("utm_source", null)
                .replaceQueryParam("utm_medium", null)
                .replaceQueryParam("utm_campaign", null);
        return new Post.PostBuilder(uri.toUriString())
                .setTitle(e.getTitle())
                .setAuthor(e.getAuthor())
                .setPostDate(e.getPublishedDate())
                .build();
    }

}
