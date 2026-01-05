package com.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetTime; // <-- Add this import
import org.springframework.dao.EmptyResultDataAccessException; 

@Service
public class DataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- RowMapper for mapping database rows to Java objects ---
    // This is a helper for converting "id", "symbl" to a Holding object
    private final RowMapper<Holding> holdingMapper = (rs, rowNum) -> new Holding(
        rs.getLong("id"),
        rs.getInt("portfolio_id"),
        rs.getString("symbl"),
        rs.getInt("qty"),
        rs.getDouble("avg_price") // Fixed your "avg_ rice" typo
    );

    // --- ADD THIS NEW RowMapper for Transaction ---
    private final RowMapper<Transaction> transactionMapper = (rs, rowNum) -> new Transaction(
        rs.getLong("id"),
        rs.getInt("portfolio_id"),
        rs.getString("symbl"),
        rs.getString("type"),
        rs.getInt("qty"),
        rs.getDouble("price"),
        rs.getObject("timestamp", OffsetTime.class) // How to read TIMESTAMPTZ
    );

    // This runs the init code once, right after the app starts
    @PostConstruct
    public void initializeTables() {
        // --- I have fixed several critical typos from your file ---
        // 1. "portfolio" (singular)
        // 2. "avg_price" (fixed "avg_ rice")
        // 3. "TIMESTAMPTZ" (safer for 'transactions' than 'TIME')
        // 4. "stock" table removed (it seemed to be a duplicate of 'holdings')
        
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users("
            + "id SERIAL PRIMARY KEY,"
            + "username VARCHAR(50) NOT NULL,"
            + "email VARCHAR(100) NOT NULL,"
            + "password VARCHAR(100) NOT NULL)");
        
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS portfolio("
            + "id SERIAL PRIMARY KEY,"
            + "user_id INTEGER NOT NULL,"
            + "cash_acc NUMERIC(12, 2) NOT NULL, " // Using NUMERIC for money
            + "FOREIGN KEY (user_id) REFERENCES users (id))");
        
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS holdings("
            + "id SERIAL PRIMARY KEY,"
            + "portfolio_id INTEGER NOT NULL,"
            + "symbl VARCHAR(20) NOT NULL,"
            + "qty INTEGER NOT NULL,"
            + "avg_price NUMERIC(12, 2) NOT NULL," // Fixed typo & type
            + "FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),"
            + "UNIQUE(portfolio_id, symbl))"); // Prevents duplicate stocks
        
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions("
            + "id SERIAL PRIMARY KEY,"
            + "portfolio_id INTEGER NOT NULL,"
            + "symbl VARCHAR(20) NOT NULL,"
            + "type VARCHAR(10) NOT NULL,"
            + "qty INTEGER NOT NULL,"
            + "price NUMERIC(12, 2) NOT NULL,"
            + "timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," // Better type
            + "FOREIGN KEY (portfolio_id) REFERENCES portfolio(id))");
    }

    /**
     * Fetches all holdings for a given portfolio
     */
    public List<Holding> getHoldings(int portfolioId) {
        String sql = "SELECT * FROM holdings WHERE portfolio_id = ?";
        return jdbcTemplate.query(sql, holdingMapper, portfolioId);
    }

    /**
     * This is the "dummy transaction" logic.
     * @Transactional means "all of this succeeds, or none of it does."
     */
    @Transactional
    public void buyStock(int portfolioId, String symbol, int quantity, double price) {
        // 1. Get the current holding (if it exists)
        String sql = "SELECT * FROM holdings WHERE portfolio_id = ? AND symbl = ?";
        List<Holding> existing = jdbcTemplate.query(sql, holdingMapper, portfolioId, symbol);
        
        if (existing.isEmpty()) {
            // New holding: INSERT
            String insertSql = "INSERT INTO holdings(portfolio_id, symbl, qty, avg_price) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, portfolioId, symbol, quantity, price);
        } else {
            // Existing holding: UPDATE
            Holding old = existing.get(0);
            int newQty = old.quantity() + quantity;
            
            // --- I fixed the math from your file ---
            double newAvgPrice = ((old.averagePrice() * old.quantity()) + (price * quantity)) / newQty;

            String updateSql = "UPDATE holdings SET qty = ?, avg_price = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newQty, newAvgPrice, old.id());
        }

        // 2. Log the transaction
        String logSql = "INSERT INTO transactions(portfolio_id, symbl, type, qty, price) VALUES (?, ?, 'BUY', ?, ?)";
        jdbcTemplate.update(logSql, portfolioId, symbol, quantity, price);
        
        // 3. Update cash (You would add this)
        // String updateCashSql = "UPDATE portfolio SET cash_acc = cash_acc - ? WHERE id = ?";
        // jdbcTemplate.update(updateCashSql, (quantity * price), portfolioId);
    }
}