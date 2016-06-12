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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.szepietowski.entity.ScrapingMetadata;
import org.szepietowski.entity.User;
import org.szepietowski.repository.ScrapingMetadataRepository;
import org.szepietowski.repository.UserRepository;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class UserReader extends CookiePropertyHolder implements Runnable {

    private Logger log = LoggerFactory.getLogger(UserReader.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScrapingMetadataRepository scrapingMetadataRepository;

    @Value("${forum.baseUrl}")
    private String baseUrl;

    @Value("${forum.member.startPage}")
    private String startPage;

    private Connection connection;

    private int offset = 0;

    @PostConstruct
    public void setupConnection() {
        connection = Jsoup.connect(baseUrl + startPage)
                .cookies(getCookieMap())
                .userAgent(userAgent);
    }

    @Override
    public void run() {
        try {
            ScrapingMetadata scrapingMetadata = scrapingMetadataRepository.findByRunner(this.getClass().getName());
            if (scrapingMetadata != null) {
                offset = Integer.valueOf(scrapingMetadata.getLastScrapedPage().replaceAll(".*?start=(\\d+)", "$1"));
            }
            boolean continueScraping;
            do {
                Document document = connection.url(baseUrl + startPage.replaceAll("start=\\d+", "start=" + offset)).get();
                Elements memberList = document.select("#memberlist").get(0).getElementsByTag("tbody");
                continueScraping = extractAndSaveUserInformation(memberList.get(0));
                if (continueScraping) {
                    offset += 25;
                }
            } while (continueScraping);
            saveScrapingMetadata(true);
        } catch (Exception e) {
            log.error("Exception encountered: ", e);
            saveScrapingMetadata(false);
        }
    }

    private void saveScrapingMetadata(boolean isComplete) {
        ScrapingMetadata scrapingMetadata = scrapingMetadataRepository.findByRunner(this.getClass().getName());
        if (scrapingMetadata == null) {
            scrapingMetadata = new ScrapingMetadata();
        }
        scrapingMetadata.setLastScrapedPage(baseUrl + startPage.replaceAll("start=\\d+", "start=" + (offset - 25)));
        scrapingMetadata.setRunner(this.getClass().getName());
        scrapingMetadata.setComplete(isComplete);
        scrapingMetadataRepository.saveAndFlush(scrapingMetadata);
    }

    private boolean extractAndSaveUserInformation(Element memberList) {
        Elements tableRows = memberList.getElementsByTag("tr");
        for (Element element : tableRows) {
            if (element.child(0).html().contains("Nie znaleziono u\u017Cytkownik\u00F3w pasuj\u0105cych do tych kryteri\u00F3w")) {
                return false;
            }
            User user = extractAndSetUserIdAndName(element.child(0));
            user.setLocation(extractLocation(element.child(2)));
            log.info("Saving user: " + user);
            try {
                userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                log.error("Constraint violated for user: " + user, e);
            }
        }
        return true;
    }

    private User extractAndSetUserIdAndName(Element userIdAndName) {
        User user = new User();
        Element aTag = userIdAndName.child(1);
        String hrefValue = aTag.attr("href");
        user.setId(Long.parseLong(hrefValue.replaceAll(".*u=(\\d+)", "$1")));
        user.setName(aTag.html());
        user = visitDetailsAndExtractAvatarAndStatus(user, hrefValue);
        return user;
    }

    private String extractLocation(Element location) {
        for (Element potentialLocation : location.children()) {
            if (potentialLocation.children().size() == 0) {
                return potentialLocation.html();
            }
        }

        return null;
    }

    private User visitDetailsAndExtractAvatarAndStatus(User user, String profilePage) {
        try {
            Document document = connection.url(baseUrl + profilePage.substring(2)).get();
            Elements avatarDiv = document.getElementsByClass("user-avatar");
            String avatarDivStyle = avatarDiv.attr("style");
            String avatarAddress = avatarDivStyle.replaceAll(".*?url\\((.*?)\\).*", "$1");
            if (!avatarAddress.isEmpty()) {
                user.setAvatar(avatarAddress);
            }
            Elements userDetails = document.getElementsByClass("left-box").select(".details");
            if (!userDetails.isEmpty()) {
                Element userDetail = userDetails.get(0);
                boolean isStatusFound = false;
                for (Element child : userDetail.children()) {
                    if ("Status:".equals(child.html())) {
                        isStatusFound = true;
                        continue;
                    }
                    if (isStatusFound) {
                        user.setStatus(child.html());
                        break;
                    }
                }
            }

        } catch (IOException e) {
            log.error("Could not get profile details of: " + profilePage+ ", exception: ", e);
        }

        return user;
    }
}
