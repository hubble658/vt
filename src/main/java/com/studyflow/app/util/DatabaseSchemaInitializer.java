package com.studyflow.app.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Veritabanı şemasını başlangıçta oluşturur.
 * View, Function, Trigger, Index ve Constraint'leri yükler.
 * DatabasePopulator'dan önce çalışmalı (Order = 1).
 */
@Component
@Order(1)
public class DatabaseSchemaInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("   VERITABANI SEMASI BASLATILIYOR");
        System.out.println("========================================");

        try {
            // schema.sql dosyasini oku
            ClassPathResource resource = new ClassPathResource("schema.sql");
            
            if (!resource.exists()) {
                System.out.println("[WARN] schema.sql dosyasi bulunamadi. Sema olusturma atlandi.");
                return;
            }

            String sqlScript;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                sqlScript = reader.lines().collect(Collectors.joining("\n"));
            }

            // SQL scriptini çalıştır
            // Her bir statement'ı ayrı ayrı çalıştır
            String[] statements = splitSqlStatements(sqlScript);
            
            int successCount = 0;
            int errorCount = 0;
            
            for (String statement : statements) {
                statement = statement.trim();
                if (statement.isEmpty() || statement.startsWith("--")) {
                    continue;
                }
                
                try {
                    jdbcTemplate.execute(statement);
                    successCount++;
                } catch (Exception e) {
                    // Bazi hatalar beklenen durumlar olabilir (zaten var olan objeler vs.)
                    if (!e.getMessage().contains("already exists") && 
                        !e.getMessage().contains("duplicate key") &&
                        !e.getMessage().contains("does not exist")) {
                        System.out.println("[WARN] SQL Hata: " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
                        errorCount++;
                    }
                }
            }
            
            System.out.println("========================================");
            System.out.println("   [OK] SEMA BASLATMA TAMAMLANDI");
            System.out.println("   Basarili: " + successCount + " | Hata: " + errorCount);
            System.out.println("========================================");
            
            // Sema ozetini yazdir
            printSchemaInfo();
            
        } catch (Exception e) {
            System.err.println("[ERROR] SEMA BASLATMA HATASI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * SQL scriptini statement'lara boler.
     * $$ ile sinirlanmis fonksiyonlari korur.
     */
    private String[] splitSqlStatements(String script) {
        // Fonksiyon ve trigger bloklari icin ozel islem
        // $$ arasindaki kisimlari koruyarak bol
        java.util.List<String> statements = new java.util.ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inDollarQuote = false;
        
        String[] lines = script.split("\n");
        for (String line : lines) {
            // Yorum satirlarini atla (tek satirlik)
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("--") && !inDollarQuote) {
                continue;
            }
            
            currentStatement.append(line).append("\n");
            
            // $$ isaretini say
            int dollarCount = countOccurrences(line, "$$");
            if (dollarCount % 2 == 1) {
                inDollarQuote = !inDollarQuote;
            }
            
            // Statement'in sonu mu kontrol et
            // Dollar quote disindayken ve satirın sonunda ; varsa
            if (!inDollarQuote && trimmedLine.endsWith(";")) {
                String stmt = currentStatement.toString().trim();
                if (!stmt.isEmpty() && !stmt.equals(";")) {
                    // Bos yorum satirlarini temizle
                    String cleanStmt = stmt.replaceAll("(?m)^\\s*--.*$", "").trim();
                    if (!cleanStmt.isEmpty() && !cleanStmt.equals(";")) {
                        statements.add(stmt);
                    }
                }
                currentStatement = new StringBuilder();
            }
        }
        
        // Kalan statement varsa ekle
        String remaining = currentStatement.toString().trim();
        if (!remaining.isEmpty() && !remaining.equals(";")) {
            statements.add(remaining);
        }
        
        return statements.toArray(new String[0]);
    }
    
    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
    
    /**
     * Olusturulan sema objelerinin ozetini yazdirir.
     */
    private void printSchemaInfo() {
        try {
            System.out.println("\n[INFO] OLUSTURULAN VERITABANI OBJELERI:");
            
            // View sayisi
            Integer viewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'public' AND table_name LIKE 'vw_%'",
                Integer.class);
            System.out.println("   Views: " + (viewCount != null ? viewCount : 0));
            
            // Function sayisi
            Integer funcCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = 'public' AND routine_name LIKE 'fn_%'",
                Integer.class);
            System.out.println("   Functions: " + (funcCount != null ? funcCount : 0));
            
            // Trigger sayisi
            Integer triggerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = 'public' AND trigger_name LIKE 'trg_%'",
                Integer.class);
            System.out.println("   Triggers: " + (triggerCount != null ? triggerCount : 0));
            
            // Index sayisi
            Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx_%'",
                Integer.class);
            System.out.println("   Indexes: " + (indexCount != null ? indexCount : 0));
            
            // Sequence sayisi
            Integer seqCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema = 'public'",
                Integer.class);
            System.out.println("   Sequences: " + (seqCount != null ? seqCount : 0));
            
            System.out.println("");
            
        } catch (Exception e) {
            System.out.println("   [WARN] Sema bilgisi alinamadi: " + e.getMessage());
        }
    }
}
