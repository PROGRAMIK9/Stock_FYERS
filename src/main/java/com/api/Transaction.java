package com.api;

import java.time.OffsetTime;

public record Transaction(
	    long id,
	    int portfolioId,
	    String symbol,
	    String type, // "BUY" or "SELL"
	    int quantity,
	    double price,
	    OffsetTime timestamp // Use OffsetTime for pgsql 'TIME'
	) {}