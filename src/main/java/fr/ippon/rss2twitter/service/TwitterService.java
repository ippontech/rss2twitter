package fr.ippon.rss2twitter.service;

import fr.ippon.rss2twitter.entity.Post;
import fr.ippon.rss2twitter.repository.PostRepository;
import fr.ippon.rss2twitter.repository.TwitterRepository;
import fr.ippon.rss2twitter.repository.UrlRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TwitterService {

    private Log logger = LogFactory.getLog(getClass());

    private static final Pattern URL_PATTERN = Pattern.compile(".*(http://t.co/[A-Za-z0-9]+).*", Pattern.DOTALL);

    @Value("${twitter.consumerKey}")
    private String consumerKey;

    @Value("${twitter.consumerSecret}")
    private String consumerSecret;

    @Value("${blog.url}")
    private String blogUrl;

    @Value("${publication.textFormat}")
    private String publicationTextFormat;

    @Autowired
    private TwitterRepository repository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UrlRepository urlRepository;

    private Twitter twitter;

    @PostConstruct
    public void init() throws TwitterException {
        String accessToken = repository.getAccessToken();
        String accessTokenSecret = repository.getAccessTokenSecret();

        if (accessToken == null || accessTokenSecret == null) {
            AccessToken accessTokenObj = requestAccessToken();

            accessToken = accessTokenObj.getToken();
            accessTokenSecret = accessTokenObj.getTokenSecret();

            repository.setAccessToken(accessToken);
            repository.setAccessTokenSecret(accessTokenSecret);
        }

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);
        twitter = new TwitterFactory(cb.build()).getInstance();
    }

    public void reconcileTweets() {
        logger.debug("Reconciling tweets");
        try {
            twitter.getUserTimeline(new Paging(1, 100))
                    .stream()
                    .filter(s -> s.isRetweet() == false && s.getText().startsWith("RT ") == false)
                    .forEach(s -> reconcileTweet(s));
        } catch (TwitterException ex) {
            logger.warn("Failed reconciling tweets", ex);
        }
        logger.debug("Done reconciling tweets");
    }

    private void reconcileTweet(Status s) {
        String url = extractUrl(s);
        if (url == null || url.startsWith(blogUrl) == false)
            return;

        Post post = new Post.PostBuilder(url)
                .setPostDate(s.getCreatedAt())
                .setPublicationText(s.getText())
                .build();

        Optional<ZonedDateTime> lastPublicationDateOpt = postRepository.getLastPublicationDate(post);
        // only reconcile if the tweet is more recent than the last publication (with a small delta)
        if(lastPublicationDateOpt.isPresent() == false
                || post.getPostDate().isAfter(lastPublicationDateOpt.get().plusMinutes(1))) {
            postRepository.setPublicationText(post, s.getText());
            postRepository.markPostAsPublished(post, post.getPostDate());
        }
    }

    private String extractUrl(Status s) {
        try {
            Matcher matcher = URL_PATTERN.matcher(s.getText());
            if (matcher.matches() == false)
                return null;
            String minifiedUrl = matcher.group(1);
            return resolveMinifiedUrl(minifiedUrl);
        } catch (Exception ex) {
            logger.error("Failed extracting URL from " + s.getText(), ex);
            throw new RuntimeException("Failed extracting URL from status", ex);
        }
    }

    private String resolveMinifiedUrl(String minifiedUrl) throws IOException {
        String expandedUrl = urlRepository.getExpandedUrl(minifiedUrl);
        if (expandedUrl == null) {
            HttpURLConnection con = (HttpURLConnection) (new URL(minifiedUrl).openConnection());
            con.setInstanceFollowRedirects(false);
            con.connect();
            expandedUrl = con.getHeaderField("Location");
            con.disconnect();

            urlRepository.setExpandedUrl(minifiedUrl, expandedUrl);
        }
        return expandedUrl;
    }

    public void postStatus(Post post) {
        logger.info("Posting message for post: " + post.getId());

        String text = postRepository.getPublicationText(post);
        if (text == null) {
            text = preparePublicationText(post);
            postRepository.setPublicationText(post, text);
        }

        try {
            Status status = twitter.updateStatus(text);
            logger.info("Posted status with ID " + status.getId());
        } catch (TwitterException e) {
            logger.warn("Failed posting status: " + text, e);
            throw new RuntimeException("Failed posting status");
        }
    }

    private String preparePublicationText(Post post) {
        return publicationTextFormat
                .replaceAll("\\{title\\}", post.getTitle())
                .replaceAll("\\{link\\}", post.getLink())
                .replaceAll("\\{author\\}", post.getAuthor())
                .replaceAll("\\n", "\n");
    }

    private AccessToken requestAccessToken() {
        try {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret);
            Twitter twitter = new TwitterFactory(cb.build()).getInstance();

            RequestToken requestToken = twitter.getOAuthRequestToken();
            logger.info("Got request token.");
            logger.info("Request token: " + requestToken.getToken());
            logger.info("Request token secret: " + requestToken.getTokenSecret());

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("Open the following URL and grant access to your account:");
                System.out.println(requestToken.getAuthorizationURL());
                try {
                    Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()));
                } catch (UnsupportedOperationException ignore) {
                } catch (IOException ignore) {
                } catch (URISyntaxException e) {
                    throw new AssertionError(e);
                }
                System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                String pin = br.readLine();
                try {
                    AccessToken accessToken;
                    if (pin.length() > 0) {
                        accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                    } else {
                        accessToken = twitter.getOAuthAccessToken(requestToken);
                    }
                    logger.info("Got access token.");
                    logger.info("Access token: " + accessToken.getToken());
                    logger.info("Access token secret: " + accessToken.getTokenSecret());
                    return accessToken;
                } catch (TwitterException te) {
                    if (401 == te.getStatusCode()) {
                        logger.warn("Unable to get the access token.");
                    } else {
                        te.printStackTrace();
                    }
                }
            }
        } catch (TwitterException te) {
            logger.error("Failed to get accessToken", te);
            throw new RuntimeException("Failed to get accessToken", te);
        } catch (IOException ioe) {
            logger.error("Failed to read the system input", ioe);
            throw new RuntimeException("Failed to read the system input", ioe);
        }
    }

}
