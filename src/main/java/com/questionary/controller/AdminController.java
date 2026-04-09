package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.entity.QuestionStatus;
import com.questionary.security.AppUserDetails;
import com.questionary.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(
        AdminController.class
    );

    public static final String REDIRECT_ADMIN = "redirect:/admin";
    public static final String SUCCESS_MESSAGE = "successMessage";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String FAILED_ONLY_MODE = "failedOnlyMode";
    public static final String FAILED_MODE_INITIAL = "failedModeInitialCount";
    public static final String SELECTED_CHAPTER_IDS = "selectedChapterIds";

    private static final String PARAM_Q = "q";
    private static final String PARAM_STATUS_FILTER = "statusFilter";
    private static final String PARAM_CHAPTER_FILTER = "chapterFilter";

    private final QuestionService questionService;

    public AdminController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String adminPage(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String statusFilter,
        @RequestParam(required = false) String chapterFilter,
        Model model,
        HttpSession session,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        QuestionStatus statusEnum = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                statusEnum = QuestionStatus.valueOf(statusFilter);
            } catch (IllegalArgumentException ignored) {}
        }

        @SuppressWarnings("unchecked")
        Set<Long> selectedChapterIds = (Set<Long>) session.getAttribute(
            SELECTED_CHAPTER_IDS
        );
        if (selectedChapterIds == null) selectedChapterIds =
            new LinkedHashSet<>();

        model.addAttribute(
            "questions",
            questionService.findFiltered(q, statusEnum, chapterFilter, user)
        );
        model.addAttribute("totalCount", questionService.countTotal(user));
        model.addAttribute(
            "unansweredCount",
            questionService.countUnanswered(user)
        );
        model.addAttribute("successCount", questionService.countSuccess(user));
        model.addAttribute("failedCount", questionService.countFailed(user));
        model.addAttribute(
            FAILED_ONLY_MODE,
            Boolean.TRUE.equals(session.getAttribute(FAILED_ONLY_MODE))
        );
        model.addAttribute("filterText", q != null ? q : "");
        model.addAttribute("filterStatus", statusEnum);
        model.addAttribute(
            "filterChapter",
            chapterFilter != null ? chapterFilter : ""
        );
        model.addAttribute("chapters", questionService.findAllChapters(user));
        model.addAttribute("selectedChapterIds", selectedChapterIds);
        return "admin";
    }

    @PostMapping("/set-chapter-filter")
    public String setChapterFilter(
        @RequestParam(required = false) List<Long> chapterIds,
        HttpSession session,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        Set<Long> ids = (chapterIds != null)
            ? new LinkedHashSet<>(chapterIds)
            : new LinkedHashSet<>();
        session.setAttribute(SELECTED_CHAPTER_IDS, ids);
        log.info("User '{}' set chapter filter: {}", user.getUsername(), ids);
        return REDIRECT_ADMIN;
    }

    @PostMapping("/toggle-failed-mode")
    public String toggleFailedMode(
        HttpSession session,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        Boolean current = (Boolean) session.getAttribute(FAILED_ONLY_MODE);
        boolean turningOn = !Boolean.TRUE.equals(current);
        session.setAttribute(FAILED_ONLY_MODE, turningOn);
        if (turningOn) {
            session.setAttribute(
                FAILED_MODE_INITIAL,
                questionService.countFailed(user)
            );
        }
        log.info(
            "User '{}' toggled failed-only mode: {}",
            user.getUsername(),
            turningOn ? "ON" : "OFF"
        );
        return REDIRECT_ADMIN;
    }

    @PostMapping("/import")
    public String importFile(
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        if (file.isEmpty()) {
            log.warn(
                "User '{}' submitted an empty file for import",
                user.getUsername()
            );
            redirectAttrs.addFlashAttribute(
                ERROR_MESSAGE,
                "Please select a file."
            );
        } else {
            String originalName = file.getOriginalFilename();
            if (
                originalName == null ||
                !originalName.toLowerCase().endsWith(".txt")
            ) {
                log.warn(
                    "User '{}' submitted invalid file type: '{}'",
                    user.getUsername(),
                    originalName
                );
                redirectAttrs.addFlashAttribute(
                    ERROR_MESSAGE,
                    "Only .txt files are accepted."
                );
            } else {
                try {
                    int count = questionService.importFromFile(file, user);
                    redirectAttrs.addFlashAttribute(
                        SUCCESS_MESSAGE,
                        "Imported " + count + " question(s) successfully."
                    );
                } catch (Exception e) {
                    log.error(
                        "Import failed for user '{}', file '{}': {}",
                        user.getUsername(),
                        originalName,
                        e.getMessage(),
                        e
                    );
                    redirectAttrs.addFlashAttribute(
                        ERROR_MESSAGE,
                        "Import failed: " + e.getMessage()
                    );
                }
            }
        }
        return REDIRECT_ADMIN;
    }

    @PostMapping("/question/create")
    public String createQuestion(
        @RequestParam String questionText,
        @RequestParam String answerText,
        @RequestParam QuestionStatus status,
        @RequestParam(required = false, defaultValue = "") String q,
        @RequestParam(required = false, defaultValue = "") String statusFilter,
        @RequestParam(required = false, defaultValue = "") String chapterFilter,
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        if (questionText.isBlank() || answerText.isBlank()) {
            redirectAttrs.addFlashAttribute(
                ERROR_MESSAGE,
                "Question and answer cannot be blank."
            );
        } else {
            questionService.createQuestion(
                questionText,
                answerText,
                status,
                user
            );
            redirectAttrs.addFlashAttribute(
                SUCCESS_MESSAGE,
                "Question created."
            );
        }
        if (!q.isBlank()) redirectAttrs.addAttribute(PARAM_Q, q);
        if (!statusFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_STATUS_FILTER,
            statusFilter
        );
        if (!chapterFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_CHAPTER_FILTER,
            chapterFilter
        );
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
        @RequestParam(required = false, defaultValue = "") String chapterFilter,
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        if (questionText.isBlank() || answerText.isBlank()) {
            redirectAttrs.addFlashAttribute(
                ERROR_MESSAGE,
                "Question and answer cannot be blank."
            );
        } else {
            try {
                questionService.updateQuestion(
                    id,
                    questionText,
                    answerText,
                    status,
                    user
                );
                redirectAttrs.addFlashAttribute(
                    SUCCESS_MESSAGE,
                    "Question updated."
                );
            } catch (Exception e) {
                log.error(
                    "Update failed for question id={}: {}",
                    id,
                    e.getMessage(),
                    e
                );
                redirectAttrs.addFlashAttribute(
                    ERROR_MESSAGE,
                    "Update failed: " + e.getMessage()
                );
            }
        }
        if (!q.isBlank()) redirectAttrs.addAttribute(PARAM_Q, q);
        if (!statusFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_STATUS_FILTER,
            statusFilter
        );
        if (!chapterFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_CHAPTER_FILTER,
            chapterFilter
        );
        return REDIRECT_ADMIN;
    }

    @PostMapping("/question/{id}/delete")
    public String deleteQuestion(
        @PathVariable Long id,
        @RequestParam(required = false, defaultValue = "") String q,
        @RequestParam(required = false, defaultValue = "") String statusFilter,
        @RequestParam(required = false, defaultValue = "") String chapterFilter,
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        questionService.deleteById(id, user);
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "Question deleted.");
        if (!q.isBlank()) redirectAttrs.addAttribute(PARAM_Q, q);
        if (!statusFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_STATUS_FILTER,
            statusFilter
        );
        if (!chapterFilter.isBlank()) redirectAttrs.addAttribute(
            PARAM_CHAPTER_FILTER,
            chapterFilter
        );
        return REDIRECT_ADMIN;
    }

    @PostMapping("/delete-all")
    public String deleteAll(
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        log.warn(
            "User '{}' requested DELETE ALL questions",
            user.getUsername()
        );
        questionService.deleteAll(user);
        redirectAttrs.addFlashAttribute(
            SUCCESS_MESSAGE,
            "All questions deleted."
        );
        return REDIRECT_ADMIN;
    }

    @PostMapping("/reset-statuses")
    public String resetStatuses(
        RedirectAttributes redirectAttrs,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        AppUser user = principal.user();
        log.info("User '{}' requested reset all statuses", user.getUsername());
        questionService.resetAllStatuses(user);
        redirectAttrs.addFlashAttribute(
            SUCCESS_MESSAGE,
            "All statuses reset to unanswered."
        );
        return REDIRECT_ADMIN;
    }
}
