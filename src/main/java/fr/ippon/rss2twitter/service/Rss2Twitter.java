package fr.ippon.rss2twitter.service;

import fr.ippon.rss2twitter.entity.Post;
import fr.ippon.rss2twitter.repository.PostRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class Rss2Twitter {

    private Log logger = LogFactory.getLog(getClass());

    @Autowired
    private RssService rssService;

    @Autowired
    private TwitterService twitterService;

    @Autowired
    private PostRepository repository;

    @Value("${posts.maxAgeInDays}")
    private int maxAgeInDays;

    @Value("${publication.rateInMinutes}")
    private int publicationRateInMinutes;

    @Value("${publication.maxPublications}")
    private int maxPublications;

    @Value("${publication.waitBeforePublishingInMinutes}")
    private int minutesToWaitBeforePublishing;

    @Scheduled(fixedDelay = 60000)
    public void processPosts() {
        twitterService.reconcileTweets();

        List<Post> posts = rssService.readRss()
                .stream()
                .filter(post -> retainEntry(post))
                .map(post -> repository.retrievePublicationDetails(post))
                .collect(Collectors.toList());

        if (logger.isTraceEnabled()) {
            logger.trace("--- LISTING POSTS");
            posts.stream()
                    .sorted(Comparator.comparing(Post::getLastPublicationDate))
                    .forEach(post -> logger.trace(post.getId() + "\n"
                            + "publication count: " + post.getPublicationCount() + "\n"
                            + "last publication date: " + post.getLastPublicationDate()));
            logger.trace("--- LISTING POSTS (END)");
        }

        // if the latest post has never been published, we will publish it
        Optional<Post> postToPublishNow = findPostToPublishNow(posts);
        if (postToPublishNow.isPresent()) {
            logger.info("Found post to publish immediately (never published before)");
            publishPost(postToPublishNow.get());
            return;
        }

        ZonedDateTime lastPublicationDate = repository.getLastPublicationDate();
        Duration lastPublicationAge = Duration.between(lastPublicationDate, ZonedDateTime.now());
        if (lastPublicationAge.toMinutes() >= publicationRateInMinutes) {
            //if (lastPublicationAge.toMillis() >= 10000) {
            logger.info("Found post to publish (delay reached)");
            Optional<Post> postToPublish = findBestPostToPublish(posts);
            if (postToPublish.isPresent()) {
                publishPost(postToPublish.get());
                return;
            }
        }

        logger.info("Nothing to publish for now");
    }

    private boolean retainEntry(Post post) {
        Duration age = Duration.between(post.getPostDate(), ZonedDateTime.now());
        return age.toDays() <= maxAgeInDays;
    }

    // if the latest post has never been published, and was posted more than X minutes ago, we will publish it
    private Optional<Post> findPostToPublishNow(List<Post> posts) {
        return posts.stream()
                .findFirst()
                .filter(post -> post.getPublicationCount() == 0)
                .filter(post -> Duration.between(post.getPostDate(),
                        ZonedDateTime.now()).toMinutes() < minutesToWaitBeforePublishing);
    }

    private Optional<Post> findBestPostToPublish(List<Post> posts) {
        Optional<Post> res = posts.stream()
                .filter(post -> post.getPublicationCount() == 0)
                .findFirst();
        if (!res.isPresent()) {
            ZonedDateTime now = ZonedDateTime.now();
            res = posts.stream()
                    .filter(post -> post.getPublicationCount() < maxPublications)
                    .filter(post -> post.getMaxPublicationDate() == null || post.getMaxPublicationDate().isAfter(now))
                    .sorted(Comparator.comparing(Post::getLastPublicationDate))
                    .findFirst();
        }
        return res;
    }

    private void publishPost(Post post) {
        logger.info("Publishing post: " + post.getId());

        try {
            twitterService.postStatus(post);
        } catch (Exception ex) {
            logger.error("Failed publishing post", ex);
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        repository.markPostAsPublished(post, now);
        repository.setLastPublicationDate(now);
    }
}
