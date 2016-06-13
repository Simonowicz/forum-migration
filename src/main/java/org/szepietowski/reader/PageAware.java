package org.szepietowski.reader;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public interface PageAware {
    default String getNextPageFromDocument(Document document) {
        Element pagination = document.getElementsByClass("pagination").get(0);
        Element paginationSpan = pagination.getElementsByTag("span").get(0);
        Element currentIndex = paginationSpan.getElementsByTag("strong").get(0);
        if (currentIndex.nextElementSibling() != null && currentIndex.nextElementSibling().hasClass("page-sep")) {
            Element aTag = currentIndex.nextElementSibling().nextElementSibling();
            String href = aTag.attr("href");
            href = href.replaceAll("&amp;", "&");
            href = href.replaceAll("&sid=.*?&", "&");
            return href.substring(2);
        }
        return null;
    }
}
