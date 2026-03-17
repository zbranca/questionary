package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.entity.QuestionStatus;
import com.questionary.security.AppUserDetails;
import com.questionary.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    public static final String REDIRECT_ADMIN = "redirect:/admin";
    public static final String SUCCESS_MESSAGE = "successMessage";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String FAILED_ONLY_MODE = "failedOnlyMode";
    public static final String FAILED_MODE_INITIAL = "failedModeInitialCount";

    private final QuestionService questionService;

    public AdminController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String adminPage(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statusFilter,
            Model model,
            HttpSession session,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.getUser();
        QuestionStatus statusEnum = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                statusEnum = QuestionStatus.valueOf(statusFilter);
            } catch (IllegalArgumentException e) {
                // invalid status value — treat as "no filter"
            }
        }

        model.addAttribute("questions", questionService.findFiltered(q, statusEnum, user));
        model.addAttribute("totalCount", questionService.countTotal(user));
        model.addAttribute("unansweredCount", questionService.countUnanswered(user));
        model.addAttribute("successCount", questionService.countSuccess(user));
        model.addAttribute("failedCount", questionService.countFailed(user));
        model.addAttribute(FAILED_ONLY_MODE, Boolean.TRUE.equals(session.getAttribute(FAILED_ONLY_MODE)));
        model.addAttribute("filterText", q != null ? q : "");
        model.addAttribute("filterStatus", statusEnum);
        return "admin";
    }

    @PostMapping("/toggle-failed-mode")
    public String toggleFailedMode(HttpSession session,
                                   @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.getUser();
        Boolean current = (Boolean) session.getAttribute(FAILED_ONLY_MODE);
        boolean turningOn = !Boolean.TRUE.equals(current);
        session.setAttribute(FAILED_ONLY_MODE, turningOn);
        if (turningOn) {
            session.setAttribute(FAILED_MODE_INITIAL, questionService.countFailed(user));
        }
        return REDIRECT_ADMIN;
    }

    @PostMapping("/import")
    public String importFile(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttrs,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.getUser();
        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Please select a file.");
        } else {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".txt")) {
                redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Only .txt files are accepted.");
            } else {
                try {
                    int count = questionService.importFromFile(file, user);
                    redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE,
                            "Imported " + count + " question(s) successfully.");
                } catch (Exception e) {
                    redirectAttrs.addFlashAttribute(ERROR_MESSAGE,
                            "Import failed: " + e.getMessage());
                }
            }
        }
        return REDIRECT_ADMIN;
    }

    @PostMapping("/question/{id}/update")
    public String updateQuestion(
            @PathVariable Long id,
            @RequestParam String questionText,
            @RequestParam String answerText,
            @RequestParam QuestionStatus status,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String statusFilter,
            RedirectAttributes redirectAttrs,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.getUser();
        if (questionText.isBlank() || answerText.isBlank()) {
            redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Question and answer cannot be blank.");
        } else {
            try {
                questionService.updateQuestion(id, questionText, answerText, status, user);
                redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "Question updated.");
            } catch (Exception e) {
                redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Update failed: " + e.getMessage());
            }
        }
        if (!q.isBlank()) redirectAttrs.addAttribute("q", q);
        if (!statusFilter.isBlank()) redirectAttrs.addAttribute("statusFilter", statusFilter);
        return REDIRECT_ADMIN;
    }

    @PostMapping("/question/{id}/delete")
    public String deleteQuestion(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String statusFilter,
            RedirectAttributes redirectAttrs,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.getUser();
        questionService.deleteById(id, user);
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "Question deleted.");
        if (!q.isBlank()) redirectAttrs.addAttribute("q", q);
        if (!statusFilter.isBlank()) redirectAttrs.addAttribute("statusFilter", statusFilter);
        return REDIRECT_ADMIN;
    }

    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttrs,
                            @AuthenticationPrincipal AppUserDetails principal) {
        questionService.deleteAll(principal.getUser());
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "All questions deleted.");
        return REDIRECT_ADMIN;
    }

    @PostMapping("/reset-statuses")
    public String resetStatuses(RedirectAttributes redirectAttrs,
                                @AuthenticationPrincipal AppUserDetails principal) {
        questionService.resetAllStatuses(principal.getUser());
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "All statuses reset to unanswered.");
        return REDIRECT_ADMIN;
    }
}
