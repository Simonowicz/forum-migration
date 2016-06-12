package org.szepietowski.reader;

import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

public abstract class CookiePropertyHolder {
    @Value("${forum.connection.user}")
    protected String userId;

    @Value("${forum.connection.sid}")
    protected String sessionId;

    @Value("${forum.connection.k}")
    protected String kValue;

    @Value("${forum.connection.user-agent}")
    protected String userAgent;
    
    protected Map<String, String> getCookieMap() {
        Map<String, String> cookieMap = new HashMap<>();
        cookieMap.put("forummanutdpl_u", userId);
        cookieMap.put("forummanutdpl_k", "");
        cookieMap.put("forummanutdpl_sid", sessionId);
        cookieMap.put("style_cookie", "printonly");
        return cookieMap;
    }
}
