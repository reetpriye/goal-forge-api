package dev.reet.goal_forge.controller;

import dev.reet.goal_forge.model.User;
import dev.reet.goal_forge.repository.UserRepository;
import dev.reet.goal_forge.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Optional;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.util.Collections;

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

    // POST /auth/google { idToken: string }
    @PostMapping(value = "/google", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body) {
        String idTokenString = body.get("idToken");
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId)) // Use injected client ID
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google ID token"));
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

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
