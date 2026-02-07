package com.krzelj.lms.web.mvc;

import com.krzelj.lms.domain.RoleName;
import com.krzelj.lms.domain.User;
import com.krzelj.lms.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("allRoles", RoleName.values());
        return "users/new";
    }

    @PostMapping
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam(required = false) List<String> roles,
                             @RequestParam(defaultValue = "true") boolean enabled,
                             @RequestParam(defaultValue = "en") String locale,
                             RedirectAttributes redirectAttributes) {
        try {
            if (userService.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                return "redirect:/admin/users/new";
            }
            if (userService.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email already exists");
                return "redirect:/admin/users/new";
            }

            String passwordHash = passwordEncoder.encode(password);
            User user = new User(username, passwordHash, email);
            user.setEnabled(enabled);
            user.setLocale(locale);

            if (roles != null && !roles.isEmpty()) {
                Set<RoleName> roleNames = roles.stream()
                        .map(RoleName::valueOf)
                        .collect(Collectors.toSet());
                userService.createUserWithRoles(user, roleNames);
            } else {
                userService.save(user);
            }

            redirectAttributes.addFlashAttribute("success", "User created successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", RoleName.values());
        return "users/edit";
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String username,
                             @RequestParam String email,
                             @RequestParam(required = false) String password,
                             @RequestParam(required = false) List<String> roles,
                             @RequestParam(defaultValue = "true") boolean enabled,
                             @RequestParam(defaultValue = "en") String locale,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

            User existingByUsername = userService.findByUsername(username).orElse(null);
            if (existingByUsername != null && !existingByUsername.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                return "redirect:/admin/users/" + id + "/edit";
            }

            User existingByEmail = userService.findByEmail(email).orElse(null);
            if (existingByEmail != null && !existingByEmail.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists");
                return "redirect:/admin/users/" + id + "/edit";
            }

            user.setUsername(username);
            user.setEmail(email);
            user.setEnabled(enabled);
            user.setLocale(locale);

            if (password != null && !password.isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(password));
            }

            if (roles != null) {
                Set<RoleName> roleNames = roles.stream()
                        .map(RoleName::valueOf)
                        .collect(Collectors.toSet());
                userService.updateUserRoles(user, roleNames);
            }

            userService.save(user);
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
