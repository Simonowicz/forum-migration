package org.szepietowski.entity;

import javax.persistence.*;

@Entity
@Table(name = "scraping_meta_data")
public class ScrapingMetadata {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String runner;

    @Column
    private String lastScrapedPage;

    @Column
    private boolean isComplete;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunner() {
        return runner;
    }

    public void setRunner(String runner) {
        this.runner = runner;
    }

    public String getLastScrapedPage() {
        return lastScrapedPage;
    }

    public void setLastScrapedPage(String lastScrapedPage) {
        this.lastScrapedPage = lastScrapedPage;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }
}
