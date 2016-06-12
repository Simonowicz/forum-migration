package org.szepietowski.reader;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.szepietowski.entity.Forum;
import org.szepietowski.repository.ForumRepository;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class MainPageReader extends CookiePropertyHolder implements Runnable {

    private Logger log = LoggerFactory.getLogger(MainPageReader.class);

    @Value("${forum.baseUrl}")
    private String baseUrl;

    @Autowired
    private ForumRepository forumRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ThreadPoolTaskExecutor executor;

    private Connection connection;

    @PostConstruct
    public void postConstruct() {
        connection = Jsoup.connect(baseUrl)
                .cookies(getCookieMap())
                .userAgent(userAgent);
    }

    @Override
    public void run() {
        try {
            Document document = connection.get();
            Elements topiclist = document.getElementsByClass("topiclist");
            for (Element topic : topiclist) {
                Elements headers = topic.getElementsByClass("header");
                if (!headers.isEmpty()) {
                    Element header = headers.get(0);
                    Element aTag = header.getElementsByTag("a").get(0);
                    Forum forum = extractAndSaveParentForum(aTag);
                    createAndExecuteForumReader(aTag, forum);
                }
            }
            executor.shutdown();
        } catch (IOException e) {
            log.error("Could not connect, exception: ", e);
        }
    }

    private Forum extractAndSaveParentForum(Element aTag) {
        Long forumId = Long.valueOf(aTag.attr("href").replaceAll(".*?f=(\\d+).*", "$1"));
        Forum forum = forumRepository.findOne(forumId);
        if (forum == null) {
            forum = new Forum();
            forum.setId(forumId);
        }
        forum.setName(aTag.html());
        forumRepository.saveAndFlush(forum);
        return forum;
    }


    private void createAndExecuteForumReader(Element aTag, Forum parentForum) {
        ForumReader forumReader = applicationContext.getBean("forumReader", ForumReader.class);
        forumReader.setCurrentUrl(aTag.attr("href").substring(2));
        forumReader.setParentForum(parentForum);
        executor.execute(forumReader);
    }
}
