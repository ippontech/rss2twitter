package fr.ippon.rss2twitter;

import java.time.LocalDateTime;

public class Post {

    private String id;
    private String link;
    private String title;
    private LocalDateTime publicationDate;

    public Post(String id, String link, String title, LocalDateTime publicationDate) {
        this.id = id;
        this.link = link;
        this.title = title;
        this.publicationDate = publicationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }
}
