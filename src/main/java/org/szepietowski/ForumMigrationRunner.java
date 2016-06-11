package org.szepietowski;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.szepietowski.reader.UserReader;

@Component
public class ForumMigrationRunner implements CommandLineRunner {

    @Autowired
    private UserReader userReader;

    @Override
    public void run(String... strings) throws Exception {
        userReader.run();
    }
}
