package com.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

// --- Add these imports for SHA-256 hashing ---
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

@RestController
public class AuthController {
    
    // --- Your credentials ---
    public static final String FYERS_APP_ID = "HNZM6GB724-100";
    private final String FYERS_SECRET_ID = "THZ99396N9";
    private final String FYERS_REDIRECT_URL = "http://127.0.0.1:8080/auth/callback"; // or 127.0.0.1

    // --- Store the token ---
    public static String ACCESS_TOKEN = null;

    
    /**
     * STEP 1: Redirect user to Fyers login page.
     * (URL changed from /v2 to /v3)
     */
    @GetMapping("/auth/login")
    public void fyersLogin(HttpServletResponse response) throws java.io.IOException {
        // --- UPDATED URL ---
        String fyersLoginUrl = "https://api-t1.fyers.in/api/v3/generate-authcode?client_id=" + FYERS_APP_ID
                             + "&redirect_uri=" + FYERS_REDIRECT_URL
                             + "&response_type=code"
                             + "&state=your_custom_state";
        
        response.sendRedirect(fyersLoginUrl);
    }

    /**
     * STEP 2: Fyers redirects back here with an auth_code.
     * We exchange it for an access_token.
     * (This is heavily modified for v3)
     */
    @GetMapping("/auth/callback")
    public String getAccessToken(@RequestParam("auth_code") String authCode) {
        
        RestTemplate restTemplate = new RestTemplate();
        // --- UPDATED URL ---
        String fyersTokenUrl = "https://api-t1.fyers.in/api/v3/validate-authcode";
        
        // 1. Create the new v3 appIdHash
        String appIdHash = getSha256Hash(FYERS_APP_ID + ":" + FYERS_SECRET_ID);

        // 2. CREATE THE NEW v3 JSON BODY
        String requestBody = "{"
                           + "\"grant_type\":\"authorization_code\","
                           + "\"code\":\"" + authCode + "\","
                           + "\"appIdHash\":\"" + appIdHash + "\""
                           + "}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 3. MAKE THE POST REQUEST
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(fyersTokenUrl, entity, JsonNode.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                ACCESS_TOKEN = response.getBody().get("access_token").asText();
                return "SUCCESS! V3 Access Token received: " + ACCESS_TOKEN;
            } else {
                return "Error: Fyers API returned status " + response.getStatusCode() + " | Body: " + response.getBody();
            }
        } catch (Exception e) {
            return "Error during token validation: " + e.getMessage();
        }
    }

    /**
     * Helper method to create the SHA-256 hash required by Fyers API v3.
     */
    private String getSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Convert byte array to hex string
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String hex = formatter.toString();
            formatter.close();
            return hex;
        } catch (Exception e) {
            // In a real app, handle this exception properly
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}