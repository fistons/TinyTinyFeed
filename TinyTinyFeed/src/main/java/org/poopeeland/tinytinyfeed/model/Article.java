package org.poopeeland.tinytinyfeed.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by setdemr on 27/09/2016.
 */
public class Article implements Serializable {

    private static final DateFormat SDF = DateFormat.getDateTimeInstance();

    private String alwaysDisplayAttachments;
    private String author;
    private int commentsCount;
    private String commentsLink;
    private String excerpt;
    private int feedId;
    private String feedTitle;
    private String guid;
    private int id;
    private boolean isUpdated;
    //    private String[] labels;
    private String lang;
    private String link;
    private boolean marked;
    private String note;
    private boolean published;
    private int score;
    private String[] tags;
    private String title;
    private boolean unread;
    private long updated;

    public String getAlwaysDisplayAttachments() {
        return alwaysDisplayAttachments;
    }

    public void setAlwaysDisplayAttachments(String alwaysDisplayAttachments) {
        this.alwaysDisplayAttachments = alwaysDisplayAttachments;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getCommentsLink() {
        return commentsLink;
    }

    public void setCommentsLink(String commentsLink) {
        this.commentsLink = commentsLink;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt.trim();
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public String getDate() {
        return SDF.format(new Date(this.updated * 1000));
    }

    @Override
    public String toString() {
        return "NewArticle{" +
                "alwaysDisplayAttachments='" + alwaysDisplayAttachments + '\'' +
                ", author='" + author + '\'' +
                ", commentsCount=" + commentsCount +
                ", commentsLink='" + commentsLink + '\'' +
                ", excerpt='" + excerpt + '\'' +
                ", feedId=" + feedId +
                ", feedTitle='" + feedTitle + '\'' +
                ", guid='" + guid + '\'' +
                ", id=" + id +
                ", isUpdated=" + isUpdated +
                ", lang='" + lang + '\'' +
                ", link='" + link + '\'' +
                ", marked=" + marked +
                ", note='" + note + '\'' +
                ", published=" + published +
                ", score=" + score +
                ", tags=" + Arrays.toString(tags) +
                ", title='" + title + '\'' +
                ", unread=" + unread +
                ", updated=" + updated +
                ", getDate=" + getDate() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Article that = (Article) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
