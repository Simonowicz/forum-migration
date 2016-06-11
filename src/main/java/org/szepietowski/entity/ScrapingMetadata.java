package org.szepietowski.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "scraping_meta_data")
public class ScrapingMetadata extends BaseEntity {
    @Column
    private String runner;

    @Column
    private String lastScrapedPage;

    @Column
    private boolean isComplete;

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
