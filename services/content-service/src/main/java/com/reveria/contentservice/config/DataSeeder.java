package com.reveria.contentservice.config;

import com.reveria.contentservice.model.entity.VideoCategory;
import com.reveria.contentservice.model.entity.VideoTag;
import com.reveria.contentservice.repository.VideoCategoryRepository;
import com.reveria.contentservice.repository.VideoTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final VideoCategoryRepository categoryRepository;
    private final VideoTagRepository tagRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedCategories();
            seedTags();
        };
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;

        List<VideoCategory> categories = List.of(
                category("Gaming", "gaming", 1),
                category("Music", "music", 2),
                category("Education", "education", 3),
                category("Entertainment", "entertainment", 4),
                category("Sports", "sports", 5),
                category("News", "news", 6),
                category("Technology", "technology", 7),
                category("Science", "science", 8),
                category("Travel", "travel", 9),
                category("Food", "food", 10),
                category("Film", "film", 11),
                category("Comedy", "comedy", 12),
                category("Vlogs", "vlogs", 13),
                category("How-to & DIY", "how-to-diy", 14),
                category("Fitness", "fitness", 15)
        );

        categoryRepository.saveAll(categories);
        log.info("Seeded {} categories", categories.size());
    }

    private void seedTags() {
        if (tagRepository.count() > 0) return;

        List<VideoTag> tags = List.of(
                tag("tutorial", "tutorial"),
                tag("gameplay", "gameplay"),
                tag("review", "review"),
                tag("vlog", "vlog"),
                tag("music video", "music-video"),
                tag("live performance", "live-performance"),
                tag("coding", "coding"),
                tag("unboxing", "unboxing"),
                tag("reaction", "reaction"),
                tag("podcast", "podcast"),
                tag("short film", "short-film"),
                tag("documentary", "documentary"),
                tag("highlights", "highlights"),
                tag("tips & tricks", "tips-tricks"),
                tag("behind the scenes", "behind-the-scenes"),
                tag("challenge", "challenge"),
                tag("interview", "interview"),
                tag("animation", "animation"),
                tag("cover", "cover"),
                tag("news update", "news-update")
        );

        tagRepository.saveAll(tags);
        log.info("Seeded {} tags", tags.size());
    }

    private static VideoCategory category(String name, String slug, int order) {
        return VideoCategory.builder()
                .name(name)
                .slug(slug)
                .displayOrder(order)
                .build();
    }

    private static VideoTag tag(String name, String slug) {
        return VideoTag.builder()
                .name(name)
                .slug(slug)
                .usageCount(0L)
                .build();
    }
}
