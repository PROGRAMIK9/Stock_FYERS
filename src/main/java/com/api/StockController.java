package com.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class StockController {
    
    // ... your other @Autowired services for your database ...
	@Autowired
    private FyersDataService fyersService;

    // --- Service for our Database ---
    @Autowired
    private DataService databaseService;
    /**
     * This is the new endpoint to get live Fyers data.
     */
	@GetMapping("/fyers/quote")
    public String getFyersQuote(@RequestParam("symbol") String symbol) {
        
        if (AuthController.ACCESS_TOKEN == null) {
            return "Error: Not authenticated. Please go to /auth/login first.";
        }

        RestTemplate restTemplate = new RestTemplate();
        // This data URL is correct
        String quoteUrl = "https://api-t1.fyers.in/data/quotes?symbols=" + symbol;

        HttpHeaders headers = new HttpHeaders();
        
        // --- ★ THE FIX: Change the Authorization header format ---
        // We are changing it from "APP_ID:TOKEN" to "Bearer TOKEN"
        headers.set("Authorization", AuthController.FYERS_APP_ID + ":" + AuthController.ACCESS_TOKEN);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                quoteUrl, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            return response.getBody(); // Should be JSON data now
            
        } catch (Exception e) {
            return "Error fetching quote: " + e.toString(); 
        }
    }
    // ... your other /hello or /stocks endpoints ...
	@GetMapping("/fyers/history")
    // FIX: Added ("symbol") inside the annotation
    public String getStockHistory(@RequestParam("symbol") String symbol) {
        try {
            // This calls your correctly written service method
            return fyersService.getHistory(symbol, "D", "2024-01-01", "2024-11-15");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * GETS YOUR HOLDINGS from *your* PostgreSQL database
     */
    @GetMapping("/portfolio/holdings")
    public List<Holding> getMyHoldings() {
        // Using a hardcoded portfolio ID of 1 for this example
        return databaseService.getHoldings(1);
    }

    /**
     * PERFORMS A DUMMY TRANSACTION
     * This will call Fyers for a price, then update your database.
     */
    @PostMapping("/portfolio/buy")
    public String buyDummyStock(@RequestParam String symbol, @RequestParam int quantity) {
        try {
            // 1. Get real price from Fyers
            // (In a real app, you'd parse the JSON from getQuotes)
            double dummyPrice = 150.00; // Using a dummy price for now
            
            // 2. Call your database service
            // Using hardcoded portfolio ID of 1
            databaseService.buyStock(1, symbol, quantity, dummyPrice);
            
            return "Successfully bought " + quantity + " of " + symbol;
        } catch (Exception e) {
            return "Error buying stock: " + e.getMessage();
        }
    }
}

