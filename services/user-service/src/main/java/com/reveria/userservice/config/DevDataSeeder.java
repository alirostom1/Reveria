package com.reveria.userservice.config;

import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.ModeratorRole;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.repository.ModeratorRepository;
import com.reveria.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ModeratorRepository moderatorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String encodedPassword = passwordEncoder.encode("password123");

        seedUsers(encodedPassword);
        seedModerators(encodedPassword);
    }

    private void seedUsers(String encodedPassword) {
        seedUser("john@example.com", "john", "John Doe", UserStatus.ACTIVE, true, encodedPassword);
        seedUser("jane@example.com", "jane", "Jane Smith", UserStatus.ACTIVE, true, encodedPassword);
        seedUser("alex@example.com", "alex", "Alex Brown", UserStatus.PENDING_VERIFICATION, false, encodedPassword);
        seedUser("sam@example.com", "sam", "Sam Wilson", UserStatus.DEACTIVATED, true, encodedPassword);
        seedUser("pat@example.com", "pat", "Pat Lee", UserStatus.ACTIVE, true, encodedPassword);
    }

    private void seedUser(String email, String username, String displayName,
                          UserStatus status, boolean emailVerified, String encodedPassword) {
        if (userRepository.existsByEmail(email)) {
            log.info("Dev user '{}' already exists, skipping", username);
            return;
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .displayName(displayName)
                .passwordHash(encodedPassword)
                .status(status)
                .emailVerified(emailVerified)
                .build();

        userRepository.save(user);
        log.info("Seeded dev user '{}'", username);
    }

    private void seedModerators(String encodedPassword) {
        seedModerator("superadmin", "Super Admin", ModeratorRole.SUPER_ADMIN, true, encodedPassword);
        seedModerator("mod_alice", "Alice Mod", ModeratorRole.MODERATOR, true, encodedPassword);
        seedModerator("mod_bob", "Bob Mod", ModeratorRole.MODERATOR, true, encodedPassword);
        seedModerator("mod_charlie", "Charlie Mod", ModeratorRole.MODERATOR, false, encodedPassword);
    }

    private void seedModerator(String username, String displayName,
                               ModeratorRole role, boolean active, String encodedPassword) {
        if (moderatorRepository.existsByUsername(username)) {
            log.info("Dev moderator '{}' already exists, skipping", username);
            return;
        }

        Moderator moderator = Moderator.builder()
                .username(username)
                .displayName(displayName)
                .passwordHash(encodedPassword)
                .role(role)
                .active(active)
                .build();

        moderatorRepository.save(moderator);
        log.info("Seeded dev moderator '{}' (role: {})", username, role);
    }
}
