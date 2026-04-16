package dev.sivalabs.ttr;

import org.springframework.boot.SpringApplication;

public class TestTalkToRepoApplication {

    public static void main(String[] args) {
        SpringApplication.from(TalkToRepoApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
