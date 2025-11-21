package com.api;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.*;

@Service
public class FyersDataService {

    private final String FYERS_DATA_URL = "https://api-t1.fyers.in";
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches real-time quotes for one or more symbols.
     * @param symbols Example: "NSE:SBIN-EQ" or "NSE:SBIN-EQ,NSE:RELIANCE-EQ"
     * @return Raw JSON string from Fyers
     */
    public String getQuotes(String symbols) {
        if (AuthController.ACCESS_TOKEN == null) {
            throw new RuntimeException("Not authenticated with Fyers. Call /auth/login first.");
        }

        String quoteUrl = FYERS_DATA_URL + "/data/v3/quotes?symbols=" + symbols;
        return createAndSendRequest(quoteUrl);
    }

    /**
     * Fetches historical data (candles) for a symbol.
     * @param symbol Example: "NSE:SBIN-EQ"
     * @param resolution Example: "D" (daily), "60" (60 minute)
     * @param from_date Example: "2024-01-01"
     * @param to_date Example: "2024-11-15"
     * @return Raw JSON string from Fyers
     */
    public String getHistory(String symbol, String resolution, String from_date, String to_date) {
        if (AuthController.ACCESS_TOKEN == null) {
            throw new RuntimeException("Not authenticated with Fyers. Call /auth/login first.");
        }

        @SuppressWarnings("deprecation")
		String historyUrl = UriComponentsBuilder.fromHttpUrl(FYERS_DATA_URL + "/data/history")
            .queryParam("symbol", symbol)
            .queryParam("resolution", resolution)
            .queryParam("date_format", "1") // 1 = YYYY-MM-DD
            .queryParam("range_from", from_date)
            .queryParam("range_to", to_date)
            .queryParam("cont_flag", "1") // 1 = yes
            .toUriString();

        return createAndSendRequest(historyUrl);
    }

    /**
     * Helper method to build and send the authorized request.
     */
    private String createAndSendRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        // This is the correct v3 format we discovered
        headers.set("Authorization", AuthController.FYERS_APP_ID + ":" + AuthController.ACCESS_TOKEN);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            return response.getBody();
        } catch (Exception e) {
            // Re-throw the exception so the controller can see it
            throw new RuntimeException("Error fetching Fyers data: " + e.getMessage(), e);
        }
    }
}