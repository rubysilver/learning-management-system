package com.krzelj.lms.web.mvc;

import com.krzelj.lms.security.auth.AuthException;
import com.krzelj.lms.security.auth.AuthService;
import com.krzelj.lms.security.auth.dto.RegisterRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final AuthService authService;

    public RegisterController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        try {
            authService.register(new RegisterRequest(email, username, password));
            redirectAttributes.addFlashAttribute("registered", true);
            return "redirect:/login";
        } catch (AuthException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
