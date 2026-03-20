package com.questionary.config;

import com.questionary.entity.AppUser;
import com.questionary.repository.AppUserRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        if (userRepository.count() == 0) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(AppUser.ROLE_ADMIN);
            userRepository.save(admin);
            log.warn("=======================================================");
            log.warn("Default admin account created: username=admin password=admin");
            log.warn("CHANGE THIS PASSWORD via /admin/users after first login!");
            log.warn("=======================================================");
        }
    }
}
