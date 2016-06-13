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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.szepietowski.entity.Post;
import org.szepietowski.entity.ScrapingMetadata;
import org.szepietowski.entity.Topic;
import org.szepietowski.entity.User;
import org.szepietowski.repository.PostRepository;
import org.szepietowski.repository.ScrapingMetadataRepository;
import org.szepietowski.repository.UserRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class TopicReader extends CookiePropertyHolder implements Runnable, PageAware {

    private final SimpleDateFormat FORUM_DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy, HH:mm");
    private final DateTimeFormatter TO_FORUM_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DateTimeFormatter TO_FORUM_DATE_WITH_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy, HH:mm");
    private  final Map<String, String> MONTHS_MAP;

    {
        MONTHS_MAP = new HashMap<>();
        MONTHS_MAP.put("\\d+ minut\\(y\\) temu", LocalDate.now().format(TO_FORUM_DATE) + ", 20:00");
        MONTHS_MAP.put("Dzisiaj", LocalDate.now().format(TO_FORUM_DATE));
        MONTHS_MAP.put("Wczoraj", LocalDate.now().minusDays(1).format(TO_FORUM_DATE));
        MONTHS_MAP.put(" stycznia ", "-01-");
        MONTHS_MAP.put(" lutego ", "-02-");
        MONTHS_MAP.put(" marca ", "-03-");
        MONTHS_MAP.put(" kwietnia ", "-04-");
        MONTHS_MAP.put(" Maj ", "-05-");
        MONTHS_MAP.put(" czerwca ", "-06-");
        MONTHS_MAP.put(" lipca ", "-07-");
        MONTHS_MAP.put(" sierpnia ", "-08-");
        MONTHS_MAP.put(" wrze\u015Bnia ", "-09-");
        MONTHS_MAP.put(" pa\u017Adziernika ", "-10-");
        MONTHS_MAP.put(" listopada ", "-11-");
        MONTHS_MAP.put(" grudnia ", "-12-");
    }

    private final Logger log = LoggerFactory.getLogger(TopicReader.class);

    private Topic topic;

    private Connection connection;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ScrapingMetadataRepository scrapingMetadataRepository;

    @Value("${forum.maxReconnectTries}")
    private int maxReconnectTries;

    @Value("${forum.baseUrl}")
    private String baseUrl;

    @Value("${forum.topic.urlFormat}")
    private String urlFormat;

    @Override
    public void run() {
        try {
            log.info("Reading topic: " + topic.getTitle());
            String url = baseUrl + String.format(urlFormat, topic.getForum().getId(), topic.getId());
            connection = Jsoup.connect(url).cookies(getCookieMap()).userAgent(userAgent);
            ScrapingMetadata scrapingMetadata = scrapingMetadataRepository.findByRunner(getScrapingMetadataRunnerName());
            if (scrapingMetadata != null) {
                url = scrapingMetadata.getLastScrapedPage();
            }
            startReading(url);
        } catch (IndexOutOfBoundsException ignored) {}
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
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


    private void startReading(String url) {
        try {
            Document document = getDocumentWithRetries(url);
            readAndSavePosts(document);
            saveScrapingMetadata(url);
            findNextPageAndContinueReading(document);
        } catch (IOException e) {
            log.error("Could not get document, exception: ", e);
        }
    }

    private void findNextPageAndContinueReading(Document document) {
        String url = getNextPageFromDocument(document);
        if (url != null) {
            startReading(baseUrl + url);
        }
    }

    private void saveScrapingMetadata(String url) {
        ScrapingMetadata metadata;
        metadata = scrapingMetadataRepository.findByRunner(getScrapingMetadataRunnerName());
        if (metadata == null) {
            metadata = new ScrapingMetadata();
        }
        metadata.setComplete(false);
        metadata.setRunner(getScrapingMetadataRunnerName());
        metadata.setLastScrapedPage(url);
        scrapingMetadataRepository.saveAndFlush(metadata);
    }

    private String getScrapingMetadataRunnerName() {
        return this.getClass().getName() + "f" + topic.getForum().getId() + "t" + topic.getId();
    }


    private void readAndSavePosts(Document document) {
        Elements postBodies = document.getElementsByClass("postbody");
        for (Element postBody : postBodies) {
            try {
                Post post = new Post();
                Element authorParagraph = postBody.getElementsByClass("author").get(0);
                Element a = authorParagraph.getElementsByAttributeValueStarting("href", "./memberlist.php").get(0);
                User author = userRepository.findOne(Long.valueOf(a.attr("href").replaceAll(".*u=(\\d+).*", "$1")));
                post.setId(extractId(authorParagraph));
                post.setAuthor(author);
                post.setTopic(topic);
                post.setContent(postBody.getElementsByClass("content").get(0).html());
                post.setCreated(extractCreatedDate(authorParagraph));

                postRepository.saveAndFlush(post);
            } catch (Exception e) {
                log.error("Error reading posts in topic: " + topic.getTitle(), e);
            }
        }
    }

    private Long extractId(Element authorParagraph) {
        Element a = authorParagraph.getElementsByAttributeValueStarting("href", "./viewtopic.php").get(0);
        return Long.valueOf(a.attr("href").replaceAll(".*p=(\\d+).*", "$1"));
    }

    private Date extractCreatedDate(Element authorParagraph) {
        String weirdDate = authorParagraph.html().replaceAll("(?s).*»\\s*(.*)", "$1");
        for (Map.Entry<String, String> entry : MONTHS_MAP.entrySet()) {
            if (weirdDate.contains(entry.getKey()) || weirdDate.matches(entry.getKey())) {
                weirdDate = weirdDate.replaceAll(entry.getKey(), entry.getValue());
                break;
            }
        }
        try {
            return FORUM_DATE_FORMATTER.parse(weirdDate);
        } catch (ParseException e) {
            log.error("Could not parse weirdDate: " + weirdDate, e);
        }

        return null;
    }
}
