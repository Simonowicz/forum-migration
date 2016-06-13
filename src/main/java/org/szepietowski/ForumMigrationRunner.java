package org.szepietowski;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.szepietowski.entity.Forum;
import org.szepietowski.reader.ForumReader;
import org.szepietowski.reader.MainPageReader;
import org.szepietowski.reader.UserReader;
import org.szepietowski.repository.ForumRepository;

@Component
public class ForumMigrationRunner implements CommandLineRunner {

    @Autowired
    private UserReader userReader;

    @Autowired
    private MainPageReader mainPageReader;

    @Autowired
    private ForumReader forumReader;

    @Autowired
    private ForumRepository forumRepository;

    @Override
    @Transactional
    public void run(String... strings) throws Exception {
        //userReader.run();
        mainPageReader.run();
/*        Forum archiwum = forumRepository.getOne(17L);
        forumReader.setParentForum(archiwum);
        forumReader.setCurrentUrl("viewforum.php?f=17");
        forumReader.run();*/
    }
}
