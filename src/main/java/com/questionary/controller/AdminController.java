package com.questionary.controller;

import com.questionary.service.QuestionService;
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
  private final QuestionService questionService;

    public AdminController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("questions", questionService.findAll());
        model.addAttribute("totalCount", questionService.countTotal());
        model.addAttribute("unansweredCount", questionService.countUnanswered());
        model.addAttribute("successCount", questionService.countSuccess());
        model.addAttribute("failedCount", questionService.countFailed());
        return "admin";
    }

    @PostMapping("/import")
    public String importFile(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttrs) {

        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Please select a file.");
            return REDIRECT_ADMIN;
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".txt")) {
            redirectAttrs.addFlashAttribute(ERROR_MESSAGE, "Only .txt files are accepted.");
            return REDIRECT_ADMIN;
        }
        try {
            int count = questionService.importFromFile(file);
            redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE,
                    "Imported " + count + " question(s) successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute(ERROR_MESSAGE,
                    "Import failed: " + e.getMessage());
        }
        return REDIRECT_ADMIN;
    }

    @PostMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttrs) {
        questionService.deleteAll();
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "All questions deleted.");
        return REDIRECT_ADMIN;
    }

    @PostMapping("/reset-statuses")
    public String resetStatuses(RedirectAttributes redirectAttrs) {
        questionService.resetAllStatuses();
        redirectAttrs.addFlashAttribute(SUCCESS_MESSAGE, "All statuses reset to unanswered.");
        return REDIRECT_ADMIN;
    }
}
