package fr.ippon.rss2twitter.entity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class Post {

    private String id;
    private String link;
    private String title;
    private String author;
    private ZonedDateTime postDate;

    private int publicationCount;
    private ZonedDateTime lastPublicationDate;
    private ZonedDateTime maxPublicationDate;
    private String publicationText;

    private Post(String id, String link, String title, String author, ZonedDateTime postDate) {
        this.id = id;
        this.link = link;
        this.title = title;
        this.author = author;
        this.postDate = postDate;
    }

    public String getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public ZonedDateTime getPostDate() {
        return postDate;
    }

    public int getPublicationCount() {
        return publicationCount;
    }

    public void setPublicationCount(int publicationCount) {
        this.publicationCount = publicationCount;
    }

    public ZonedDateTime getLastPublicationDate() {
        return lastPublicationDate;
    }

    public void setLastPublicationDate(ZonedDateTime lastPublicationDate) {
        this.lastPublicationDate = lastPublicationDate;
    }

    public String getPublicationText() {
        return publicationText;
    }

    public void setPublicationText(String publicationText) {
        this.publicationText = publicationText;
    }

    public ZonedDateTime getMaxPublicationDate() {
        return maxPublicationDate;
    }

    public void setMaxPublicationDate(ZonedDateTime maxPublicationDate) {
        this.maxPublicationDate = maxPublicationDate;
    }

    public static class PostBuilder {

        private String link;
        private String title;
        private String author;
        private ZonedDateTime postDate;
        private String publicationText;

        public PostBuilder(String link) {
            this.link = link;
        }

        public PostBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public PostBuilder setAuthor(String author) {
            this.author = author;
            return this;
        }

        public PostBuilder setPostDate(ZonedDateTime postDate) {
            this.postDate = postDate;
            return this;
        }

        public PostBuilder setPostDate(Date postDate) {
            Instant instant = Instant.ofEpochMilli(postDate.getTime());
            this.postDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
            return this;
        }

        public PostBuilder setPublicationText(String publicationText) {
            this.publicationText = this.publicationText;
            return this;
        }

        public Post build() {
            String id = link.substring(link.indexOf('/', link.indexOf('/', link.indexOf('/') + 1) + 1));
            Post post = new Post(id, link, title, author, postDate);
            post.setPublicationText(publicationText);
            return post;
        }
    }

}
