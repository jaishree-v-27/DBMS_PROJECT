package com.bloodnetwork.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller handling public landing, privacy policy, and terms pages.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Blood Donor Network: Clinical Blood Management and Voluntary Donor Platform");
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "redirect:/#about";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("pageTitle", "Privacy Policy: Blood Donor Network");
        return "public/privacy";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("pageTitle", "Terms and Conditions: Blood Donor Network");
        return "public/terms";
    }
}
