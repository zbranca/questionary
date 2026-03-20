package com.questionary.controller;

import com.questionary.entity.AppUser;
import com.questionary.entity.Question;
import com.questionary.entity.QuestionStatus;
import com.questionary.security.AppUserDetails;
import com.questionary.service.QuestionService;
import jakarta.servlet.http.HttpSession;
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

import java.util.Optional;

@Controller
@RequestMapping("/quiz")
public class QuizController {

    private static final Logger log = LoggerFactory.getLogger(QuizController.class);

    public static final String REDIRECT_QUIZ = "redirect:/quiz/";
    public static final String REDIRECT_QUIZ_DONE = "redirect:/quiz/done";
    public static final String REDIRECT_QUIZ_NO_SLASH = "redirect:/quiz";

    private final QuestionService questionService;

    public QuizController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String quizHome(HttpSession session,
                           @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.user();
        if (questionService.countTotal(user) == 0) {
            return "redirect:/admin";
        }
        if (isFailedMode(session)) {
            return questionService.findNextFailed(user)
                    .map(q -> REDIRECT_QUIZ + q.getId())
                    .orElse(REDIRECT_QUIZ_DONE);
        }
        return questionService.findNextUnanswered(user)
                .map(q -> REDIRECT_QUIZ + q.getId())
                .orElse(REDIRECT_QUIZ_DONE);
    }

    /** Explicitly enables failed-only mode and records the initial failed count for progress tracking. */
    @PostMapping("/start-failed-mode")
    public String startFailedMode(HttpSession session,
                                  @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.user();
        session.setAttribute(AdminController.FAILED_ONLY_MODE, true);
        session.setAttribute(AdminController.FAILED_MODE_INITIAL, questionService.countFailed(user));
        return REDIRECT_QUIZ_NO_SLASH;
    }

    /** Toggles failed-only mode; records the initial failed count when turning on. */
    @PostMapping("/toggle-failed-mode")
    public String toggleFailedMode(HttpSession session,
                                   @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.user();
        boolean turningOn = !isFailedMode(session);
        session.setAttribute(AdminController.FAILED_ONLY_MODE, turningOn);
        if (turningOn) {
            session.setAttribute(AdminController.FAILED_MODE_INITIAL, questionService.countFailed(user));
        }
        log.info("User '{}' toggled failed-only mode in quiz: {}", user.getUsername(), turningOn ? "ON" : "OFF");
        return REDIRECT_QUIZ_NO_SLASH;
    }

    private boolean isFailedMode(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(AdminController.FAILED_ONLY_MODE));
    }

    @GetMapping("/done")
    public String done(Model model, HttpSession session,
                       @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.user();
        model.addAttribute("totalCount", questionService.countTotal(user));
        model.addAttribute("successCount", questionService.countSuccess(user));
        model.addAttribute("failedCount", questionService.countFailed(user));
        model.addAttribute("unansweredCount", questionService.countUnanswered(user));
        model.addAttribute(AdminController.FAILED_ONLY_MODE, isFailedMode(session));
        return "quiz-done";
    }

    @GetMapping("/{id}")
    public String showQuestion(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.user();
        Optional<Question> opt = questionService.findById(id, user);
        if (opt.isEmpty()) return REDIRECT_QUIZ_NO_SLASH;

        addQuizAttributes(model, opt.get(), false, "", session, user);
        return "quiz";
    }

    /** "Show Answer" — receives the draft text and renders the answer alongside it. */
    @PostMapping("/{id}/reveal")
    public String revealAnswer(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String draft,
            Model model,
            HttpSession session,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.user();
        Optional<Question> opt = questionService.findById(id, user);
        if (opt.isEmpty()) return REDIRECT_QUIZ_NO_SLASH;

        addQuizAttributes(model, opt.get(), true, draft, session, user);
        return "quiz";
    }

    private void addQuizAttributes(Model model, Question question, boolean showAnswer, String draft,
                                   HttpSession session, AppUser user) {
        Long initial = (Long) session.getAttribute(AdminController.FAILED_MODE_INITIAL);
        long currentFailed = questionService.countFailed(user);
        model.addAttribute("question", question);
        model.addAttribute("showAnswer", showAnswer);
        model.addAttribute("draft", draft);
        model.addAttribute(AdminController.FAILED_ONLY_MODE, isFailedMode(session));
        model.addAttribute("totalCount", questionService.countTotal(user));
        model.addAttribute("unansweredCount", questionService.countUnanswered(user));
        model.addAttribute("successCount", questionService.countSuccess(user));
        model.addAttribute("failedCount", currentFailed);
        model.addAttribute("failedModeInitial", initial != null ? initial : currentFailed);
    }

    @PostMapping("/{id}/skip")
    public String skip(@PathVariable Long id, HttpSession session,
                       @AuthenticationPrincipal AppUserDetails principal) {
        AppUser user = principal.user();
        Optional<Question> current = questionService.findById(id, user);
        if (current.isEmpty()) return REDIRECT_QUIZ_NO_SLASH;
        int sortOrder = current.get().getSortOrder();
        if (isFailedMode(session)) {
            return questionService.findNextFailedAfter(sortOrder, user)
                    .map(next -> REDIRECT_QUIZ + next.getId())
                    .orElse(REDIRECT_QUIZ_DONE);
        }
        return questionService.findNextUnansweredAfter(sortOrder, user)
                .map(next -> REDIRECT_QUIZ + next.getId())
                .orElse(REDIRECT_QUIZ_DONE);
    }

    @PostMapping("/{id}/mark")
    public String mark(
            @PathVariable Long id,
            @RequestParam QuestionStatus status,
            HttpSession session,
            @AuthenticationPrincipal AppUserDetails principal) {

        AppUser user = principal.user();
        log.info("User '{}' marking question id={} as {}", user.getUsername(), id, status);
        questionService.markStatus(id, status, user);
        if (isFailedMode(session)) {
            return questionService.findNextFailed(user)
                    .map(next -> REDIRECT_QUIZ + next.getId())
                    .orElse(REDIRECT_QUIZ_DONE);
        }
        return questionService.findNextUnanswered(user)
                .map(next -> REDIRECT_QUIZ + next.getId())
                .orElse(REDIRECT_QUIZ_DONE);
    }
}
