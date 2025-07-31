package dev.reet.goal_forge.controller;

import dev.reet.goal_forge.model.User;
import dev.reet.goal_forge.repository.UserRepository;
import dev.reet.goal_forge.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;

    // POST /auth/google { accessToken: string }
    @PostMapping(value = "/google", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        try {
            // Use Google People API to get user info from access token
            java.net.URL url = new java.net.URL("https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google access token"));
            }
            java.io.InputStream is = conn.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            s.close();
            is.close();
            conn.disconnect();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> userInfo = mapper.readValue(result, Map.class);
            String googleId = (String) userInfo.get("sub");
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            if (googleId == null || email == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Failed to get user info from Google"));
            }
            Optional<User> userOpt = userRepository.findByGoogleId(googleId);
            User user = userOpt.orElseGet(() -> {
                User u = new User();
                u.setGoogleId(googleId);
                u.setEmail(email);
                u.setName(name);
                return userRepository.save(u);
            });
            String token = jwtService.generateToken(user.getId(), user.getEmail());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Google token verification failed", "details", e.getMessage()));
        }
    }
}
