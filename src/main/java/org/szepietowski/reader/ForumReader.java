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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.szepietowski.entity.Forum;
import org.szepietowski.entity.Topic;
import org.szepietowski.entity.TopicType;
import org.szepietowski.repository.ForumRepository;
import org.szepietowski.repository.TopicRepository;
import org.szepietowski.repository.UserRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Component
@Scope("prototype")
public class ForumReader extends CookiePropertyHolder implements Runnable, PageAware {

    private final Logger log = LoggerFactory.getLogger(ForumReader.class);

    @Autowired
    private ForumRepository forumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TopicReader topicReader;

    @Value("${forum.baseUrl}")
    private String baseUrl;

    @Value("${forum.maxReconnectTries}")
    private int maxReconnectTries;

    @Value("${forum.ignore.forum}")
    private String ignoredForum;

    private String currentUrl;
    private Forum parentForum;

    private Connection connection;

    private boolean subForumsRead = false;

    @Override
    public void run() {
        connection = Jsoup.connect(baseUrl + currentUrl).cookies(getCookieMap()).userAgent(userAgent);
        startReading(baseUrl + currentUrl);
    }

    private void startReading(String url) {
        try {
            Document document = getDocumentWithRetries(url);
            log.info("Reading forum: " +  parentForum.getName());
            if (document != null) {
                if (!subForumsRead) {
                    readAndSaveSubForums(document);
                    subForumsRead = true;
                }
                readAndSaveTopics(document);
                findNextPageAndContinueReading(document);
            }
        } catch (IOException e) {
            log.error("Could not open url, exception: ", e);
        }
    }

    private void findNextPageAndContinueReading(Document document) {
        try {
            String url = getNextPageFromDocument(document);
            if (url != null) {
                startReading(baseUrl + url);
            }
        } catch (IndexOutOfBoundsException ignored) {}
    }

    private Document getDocumentWithRetries(String url) throws IOException {
        Document document = null;
        for (int i = 0; i < maxReconnectTries; i++) {
            try {
                document = connection.url(url).get();
                break;
            } catch (SocketTimeoutException ignored) {
                log.error("Could not connect due to timeout, retrying...");
            }
        }
        return document;
    }

    private void readAndSaveSubForums(Document document) {
        Elements forumElements = document.getElementsByClass("forums");
        if (!forumElements.isEmpty()) {
            Element forums = forumElements.get(0);
            for (Element forum : forums.children()) {
                Element dt = forum.getElementsByTag("dt").get(0);
                Forum forumEntity = new Forum();
                Element a = dt.child(0);
                String href = a.attr("href");
                if (!ignoredForum.equals(href)) {
                    forumEntity.setId(Long.valueOf(href.replaceAll(".*?f=(\\d+).*", "$1")));
                    forumEntity.setName(a.html());
                    forumEntity.setParentForum(parentForum);
                    forumEntity.setDescription(dt.html().replaceAll("(?s).*<br>\\s*(.*)", "$1"));
                    forumRepository.saveAndFlush(forumEntity);

                    createAndExecuteForumReader(href, forumEntity);
                }
            }
        }
    }

    private void readAndSaveTopics(Document document) {
        Elements topicElements = document.getElementsByClass("topics");
        if (!topicElements.isEmpty()) {
            Elements topicsAndAnnouncements = topicElements.select(".topiclist");
            for (Element topicOrAnnouncementList : topicsAndAnnouncements) {
                Elements liList = topicOrAnnouncementList.getElementsByTag("li");
                for (Element li : liList) {
                    Topic topic = new Topic();
                    topic.setTopicType(getTopicTypeFromLiClass(li));
                    Element a = li.getElementsByClass("topictitle").get(0);
                    topic.setTitle(a.html());
                    String href = a.attr("href");
                    long forumId = getForumIdFromHref(href);
                    if (forumId == parentForum.getId()) {
                        topic.setId(getTopicIdFromHref(href));
                        topic = extractCreator(topic, li);
                        topic.setForum(parentForum);
                        topicRepository.saveAndFlush(topic);
                        readTopic(topic);
                    }
                }
            }
        }
    }

    private void readTopic(Topic topic) {
        topicReader.setTopic(topic);
        topicReader.run();
    }

    private Topic extractCreator(Topic topic, Element li) {
        Element dt = li.getElementsByTag("dt").get(0);
        Element a = dt.getElementsByAttributeValueStarting("href", "./memberlist.php").get(0);
        long creatorId = getCreatorIdFromHref(a.attr("href"));
        topic.setCreator(userRepository.findOne(creatorId));
        return topic;
    }

    private long getCreatorIdFromHref(String href) {
        return Long.valueOf(href.replaceAll(".*u=(\\d+).*", "$1"));
    }

    private long getTopicIdFromHref(String href) {
        return Long.valueOf(href.replaceAll(".*t=(\\d+).*", "$1"));
    }

    private long getForumIdFromHref(String href) {
        return Long.valueOf(href.replaceAll(".*f=(\\d+).*", "$1"));
    }

    private TopicType getTopicTypeFromLiClass(Element li) {
        if (li.hasClass("sticky")) {
            return TopicType.STICKY;
        } else if (li.hasClass("announce")) {
            return TopicType.ANNOUNCEMENT;
        }
        return TopicType.STANDARD;
    }

    private void createAndExecuteForumReader(String href, Forum forumEntity) {
        ForumReader forumReader = applicationContext.getBean("forumReader", ForumReader.class);
        forumReader.setCurrentUrl(href.substring(2));
        forumReader.setParentForum(forumEntity);
        forumReader.run();
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public void setParentForum(Forum parentForum) {
        this.parentForum = parentForum;
    }
}
