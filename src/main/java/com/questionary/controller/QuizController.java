package com.questionary.controller;

import com.questionary.entity.Question;
import com.questionary.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/quiz")
public class QuizController {

  public static final String REDIRECT_QUIZ = "redirect:/quiz/";
  public static final String REDIRECT_QUIZ_DONE = "redirect:/quiz/done";
  public static final String REDIRECT_QUIZ_NO_SLASH = "redirect:/quiz";
  private final QuestionService questionService;

    public QuizController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String quizHome(HttpSession session) {
        if (questionService.countTotal() == 0) {
            return "redirect:/admin";
        }
        if (isFailedMode(session)) {
            return questionService.findNextFailed()
                    .map(q -> REDIRECT_QUIZ + q.getId())
                    .orElse(REDIRECT_QUIZ_DONE);
        }
        return questionService.findNextUnanswered()
                .map(q -> REDIRECT_QUIZ + q.getId())
                .orElse(REDIRECT_QUIZ_DONE);
    }

    private boolean isFailedMode(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("failedOnlyMode"));
    }

    @GetMapping("/done")
    public String done(Model model) {
        model.addAttribute("totalCount", questionService.countTotal());
        model.addAttribute("successCount", questionService.countSuccess());
        model.addAttribute("failedCount", questionService.countFailed());
        model.addAttribute("unansweredCount", questionService.countUnanswered());
        return "quiz-done";
    }

    @GetMapping("/{id}")
    public String showQuestion(
            @PathVariable Long id,
            Model model,
            HttpSession session) {

        Optional<Question> opt = questionService.findById(id);
        if (opt.isEmpty()) return REDIRECT_QUIZ_NO_SLASH;

        addQuizAttributes(model, opt.get(), false, "", isFailedMode(session));
        return "quiz";
    }

    /** "Show Answer" — receives the draft text and renders the answer alongside it. */
    @PostMapping("/{id}/reveal")
    public String revealAnswer(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String draft,
            Model model,
            HttpSession session) {

        Optional<Question> opt = questionService.findById(id);
        if (opt.isEmpty()) return REDIRECT_QUIZ_NO_SLASH;

        addQuizAttributes(model, opt.get(), true, draft, isFailedMode(session));
        return "quiz";
    }

    private void addQuizAttributes(Model model, Question question, boolean showAnswer, String draft, boolean failedOnlyMode) {
        model.addAttribute("question", question);
        model.addAttribute("showAnswer", showAnswer);
        model.addAttribute("draft", draft);
        model.addAttribute("failedOnlyMode", failedOnlyMode);
        model.addAttribute("totalCount", questionService.countTotal());
        model.addAttribute("unansweredCount", questionService.countUnanswered());
        model.addAttribute("successCount", questionService.countSuccess());
        model.addAttribute("failedCount", questionService.countFailed());
    }

    @PostMapping("/{id}/skip")
    public String skip(@PathVariable Long id, HttpSession session) {
        if (isFailedMode(session)) {
            return questionService.findNextFailedExcluding(id)
                    .map(next -> REDIRECT_QUIZ + next.getId())
                    .orElseGet(() -> questionService.findNextFailed()
                            .map(q -> REDIRECT_QUIZ + q.getId())
                            .orElse(REDIRECT_QUIZ_DONE));
        }
        return questionService.findNextUnansweredExcluding(id)
                .map(next -> REDIRECT_QUIZ + next.getId())
                .orElseGet(() -> questionService.findNextUnanswered()
                        .map(q -> REDIRECT_QUIZ + q.getId())
                        .orElse(REDIRECT_QUIZ_DONE));
    }

    @PostMapping("/{id}/mark")
    public String mark(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {

        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            questionService.markStatus(id, status);
        }
        if (isFailedMode(session)) {
            return questionService.findNextFailed()
                    .map(next -> REDIRECT_QUIZ + next.getId())
                    .orElse(REDIRECT_QUIZ_DONE);
        }
        return questionService.findNextUnanswered()
                .map(next -> REDIRECT_QUIZ + next.getId())
                .orElse(REDIRECT_QUIZ_DONE);
    }
}
