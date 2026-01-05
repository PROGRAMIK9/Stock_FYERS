package com.api;

public record Holding(
	    long id,
	    int portfolioId,
	    String symbol,
	    int quantity,
	    double averagePrice
	) {}