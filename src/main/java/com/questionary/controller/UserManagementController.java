package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.security.AppUserDetails;
import com.questionary.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private static final String REDIRECT_USERS = "redirect:/admin/users";

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin-users";
    }

    @PostMapping("/save")
    public String saveUser(
            @RequestParam(required = false) Long id,
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam String role,
            RedirectAttributes redirectAttrs) {

        if (username.isBlank()) {
            redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Username cannot be blank.");
            return REDIRECT_USERS;
        }
        if (!role.equals(AppUser.ROLE_ADMIN) && !role.equals(AppUser.ROLE_USER)) {
            redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Invalid role.");
            return REDIRECT_USERS;
        }

        if (id == null) {
            // Create
            if (password.isBlank()) {
                redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Password cannot be blank.");
                return REDIRECT_USERS;
            }
            if (userService.existsByUsername(username)) {
                redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Username already taken.");
                return REDIRECT_USERS;
            }
            userService.createUser(username, password, role);
            redirectAttrs.addFlashAttribute(AdminController.SUCCESS_MESSAGE, "User '" + username + "' created.");
        } else {
            // Update
            Optional<AppUser> existing = userService.findById(id);
            if (existing.isEmpty()) {
                redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "User not found.");
                return REDIRECT_USERS;
            }
            if (!existing.get().getUsername().equals(username) && userService.existsByUsername(username)) {
                redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Username already taken.");
                return REDIRECT_USERS;
            }
            userService.updateUser(id, username, password, role);
            redirectAttrs.addFlashAttribute(AdminController.SUCCESS_MESSAGE, "User '" + username + "' updated.");
        }
        return REDIRECT_USERS;
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal,
            RedirectAttributes redirectAttrs) {

        if (principal.user().getId().equals(id)) {
            redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "You cannot delete your own account.");
            return REDIRECT_USERS;
        }
        if (userService.findById(id).isPresent() && userService.countAdmins() <= 1) {
            boolean isTargetAdmin = userService.findById(id)
                    .map(u -> AppUser.ROLE_ADMIN.equals(u.getRole()))
                    .orElse(false);
            if (isTargetAdmin) {
                redirectAttrs.addFlashAttribute(AdminController.ERROR_MESSAGE, "Cannot delete the last admin account.");
                return REDIRECT_USERS;
            }
        }
        userService.deleteUser(id);
        redirectAttrs.addFlashAttribute(AdminController.SUCCESS_MESSAGE, "User deleted.");
        return REDIRECT_USERS;
    }
}
