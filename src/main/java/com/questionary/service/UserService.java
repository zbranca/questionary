package com.questionary.service;

import com.questionary.entity.AppUser;
import com.questionary.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public void createUser(String username, String rawPassword, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
        log.info("Created user '{}' with role {}", username, role);
    }

    @Transactional
    public void updateUser(Long id, String newUsername, String rawPassword, String role) {
        AppUser user = userRepository.findById(id).orElseThrow();
        boolean passwordChanged = rawPassword != null && !rawPassword.isBlank();
        log.info("Updated user id={}: username='{}', role={}, passwordChanged={}",
                id, newUsername, role, passwordChanged);
        user.setUsername(newUsername);
        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.warn("Deleted user id={}", id);
        userRepository.deleteById(id);
    }

    public long countAdmins() {
        return userRepository.countByRole(AppUser.ROLE_ADMIN);
    }
}
