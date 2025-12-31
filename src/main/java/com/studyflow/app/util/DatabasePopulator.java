package com.studyflow.app.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Random;

/**
 * DatabasePopulator - Gerçekçi kütüphane yapısı oluşturan veri doldurucu
 * 
 * YAPI:
 * - Her kütüphanede 2 blok (A ve B Blok)
 * - Her blokta 3-4 masa
 * - Her masada 2 veya 4 sandalye
 * 
 * Order(2): DatabaseSchemaInitializer'dan sonra çalışır
 */
@Component
@Order(2)
public class DatabasePopulator implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Random random = new Random();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================================");
        System.out.println("       STUDYFLOW VERITABANI DOLDURMA BASLADI");
        System.out.println("========================================================");

        // Veritabaninin bos olup olmadigini kontrol et
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) {
            System.out.println("[WARN] Veritabaninda zaten veri var. Islem iptal edildi.");
            System.out.println("       Tablolari temizlemek icin: DELETE FROM table_name");
            return;
        }

        try {
            populateUsers();
            populateFacilities();
            populateWeeklyCalendars();
            populateFacilityBlocks();
            populateDesks();
            populateSeats();
            populateReservations();
            populateLibrarianFacilities();

            printSummary();

        } catch (Exception e) {
            System.err.println("[ERROR] HATA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 1. USERS Tablosu - 36 kullanici (15 USER, 18 LIBRARIAN, 3 ADMIN)
     */
    private void populateUsers() {
        System.out.println("\n[INFO] USERS tablosu dolduruluyor...");
        
        String[] firstNames = {
            "Ahmet", "Mehmet", "Ayşe", "Fatma", "Ali",
            "Zeynep", "Mustafa", "Elif", "Can", "Deniz",
            "Burak", "Selin", "Emre", "Ceren", "Furkan",
            "Yusuf", "Esra", "Onur", "Merve", "Kerem",
            "Büşra", "Barış", "Gamze", "Cem", "Tuğba",
            "Oğuz", "Ebru", "Tolga", "Cansu", "Serkan",
            "Naz", "Kaan", "Pınar", "Murat", "Gizem", "Berk"
        };
        
        String[] lastNames = {
            "Yılmaz", "Kaya", "Demir", "Şahin", "Çelik",
            "Yıldız", "Aydın", "Öztürk", "Arslan", "Doğan",
            "Kılıç", "Aslan", "Çetin", "Koç", "Türk",
            "Şen", "Güler", "Yurt", "Polat", "Tan",
            "Kurt", "Uzun", "Özkan", "Aktaş", "Başar",
            "Tekin", "Özdemir", "Erdem", "Ateş", "Kara",
            "Beyaz", "Akın", "Yalçın", "Duman", "Erdoğan", "Koçak"
        };
        
        String[] roles = {
            // 15 USER
            "USER", "USER", "USER", "USER", "USER",
            "USER", "USER", "USER", "USER", "USER",
            "USER", "USER", "USER", "USER", "USER",
            // 18 LIBRARIAN (her kütüphane için en az 1, bazılarında 2)
            "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN",
            "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN",
            "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN", "LIBRARIAN",
            "LIBRARIAN", "LIBRARIAN", "LIBRARIAN",
            // 3 ADMIN
            "ADMIN", "ADMIN", "ADMIN"
        };

        for (int i = 0; i < 36; i++) {
            String firstName = firstNames[i];
            String lastName = lastNames[i];
            String email = firstName.toLowerCase().replace("ş", "s").replace("ç", "c")
                         .replace("ğ", "g").replace("ü", "u").replace("ö", "o").replace("ı", "i")
                         + "." + lastName.toLowerCase().replace("ş", "s").replace("ç", "c")
                         .replace("ğ", "g").replace("ü", "u").replace("ö", "o").replace("ı", "i")
                         + "@studyflow.com";
            String password = passwordEncoder.encode("password123");
            String role = roles[i];

            String sql = "INSERT INTO users (email, password, first_name, last_name, user_role) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql, email, password, firstName, lastName, role);
        }
        
        System.out.println("   [OK] 36 kullanici eklendi");
        System.out.println("        -> 15 USER | 18 LIBRARIAN | 3 ADMIN");
    }

    /**
     * 2. FACILITIES Tablosu - 15 tesis (İstanbul'daki ünlü kütüphaneler)
     */
    private void populateFacilities() {
        System.out.println("\n[INFO] FACILITIES tablosu dolduruluyor...");
        
        String[][] facilities = {
            {"Beyazıt Devlet Kütüphanesi", "Beyazıt Meydanı, Fatih/İstanbul", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800"},
            {"Atatürk Kitaplığı", "Taksim, Beyoğlu/İstanbul", "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=800"},
            {"İBB Merkez Kütüphanesi", "Fatih Sultan Mehmet Bulvarı, Ümraniye/İstanbul", "https://images.unsplash.com/photo-1568667256549-094345857637?w=800"},
            {"Kadıköy Halk Kütüphanesi", "Caferağa Mahallesi, Kadıköy/İstanbul", "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800"},
            {"İstanbul Tıp Fakültesi Kütüphanesi", "Çapa, Fatih/İstanbul", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=800"},
            {"Boğaziçi Üniversitesi Kütüphanesi", "Bebek, Beşiktaş/İstanbul", "https://images.unsplash.com/photo-1519682337058-a94d519337bc?w=800"},
            {"İTÜ Mustafa İnan Kütüphanesi", "Maslak, Sarıyer/İstanbul", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800"},
            {"Sabancı Üniversitesi Bilgi Merkezi", "Tuzla/İstanbul", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=800"},
            {"MSGSÜ Merkez Kütüphanesi", "Fındıklı, Beyoğlu/İstanbul", "https://images.unsplash.com/photo-1529007196863-d07650a3f0ea?w=800"},
            {"Medeniyet Üniversitesi Kütüphanesi", "Göztepe, Kadıköy/İstanbul", "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800"},
            {"Marmara Üniversitesi Kütüphanesi", "Göztepe, Kadıköy/İstanbul", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=800"},
            {"YTÜ Merkez Kütüphanesi", "Beşiktaş/İstanbul", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800"},
            {"Koç Üniversitesi Suna Kıraç Kütüphanesi", "Sarıyer/İstanbul", "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=800"},
            {"Bilgi Üniversitesi Kütüphanesi", "Eyüp/İstanbul", "https://images.unsplash.com/photo-1468779036391-52341f60b55d?w=800"},
            {"Bahçeşehir Üniversitesi Kütüphanesi", "Beşiktaş/İstanbul", "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800"}
        };

        for (String[] facility : facilities) {
            String sql = "INSERT INTO facilities (name, address, image_url) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, facility[0], facility[1], facility[2]);
        }
        
        System.out.println("   [OK] 15 kutuphane eklendi");
    }

    /**
     * 3. FACILITY_CALENDARS Tablosu - Her tesis için haftalık takvim
     */
    private void populateWeeklyCalendars() {
        System.out.println("\n[INFO] FACILITY_CALENDARS tablosu dolduruluyor...");
        
        for (long facilityId = 1; facilityId <= 15; facilityId++) {
            String sql = "INSERT INTO facility_calendars (facility_id) VALUES (?)";
            jdbcTemplate.update(sql, facilityId);
        }
        
        System.out.println("   [OK] 15 haftalik takvim eklendi");
        
        populateDailySchedules();
    }

    /**
     * 4. FACILITY_CALENDAR_DAYS Tablosu - Günlük çalışma saatleri
     */
    private void populateDailySchedules() {
        System.out.println("\n[INFO] FACILITY_CALENDAR_DAYS tablosu dolduruluyor...");
        
        String[] daysOfWeek = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        
        for (long calendarId = 1; calendarId <= 15; calendarId++) {
            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
                String day = daysOfWeek[dayIndex];
                boolean isClosed = day.equals("SUNDAY");
                LocalTime openTime = isClosed ? null : LocalTime.of(8, 0);
                LocalTime closeTime = isClosed ? null : (day.equals("SATURDAY") ? LocalTime.of(18, 0) : LocalTime.of(22, 0));
                
                String sql = "INSERT INTO facility_calendar_days (calendar_id, day_of_week, open_time, close_time, is_closed) " +
                           "VALUES (?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, calendarId, day, openTime, closeTime, isClosed);
            }
        }
        
        System.out.println("   [OK] 105 gunluk calisma saati eklendi (15 tesis x 7 gun)");
    }

    /**
     * 5. FACILITY_BLOCKS Tablosu - Her kütüphanede tam olarak 2 blok
     */
    private void populateFacilityBlocks() {
        System.out.println("\n[INFO] FACILITY_BLOCKS tablosu dolduruluyor...");
        
        String[][] blockConfigs = {
            {"A Blok", "#4A90E2", "#E3F2FD"},  // Mavi tema
            {"B Blok", "#27AE60", "#E8F5E9"}   // Yeşil tema
        };
        
        int totalBlocks = 0;
        for (long facilityId = 1; facilityId <= 15; facilityId++) {
            // Her kütüphaneye tam olarak 2 blok
            for (int i = 0; i < 2; i++) {
                String blockName = blockConfigs[i][0];
                String color = blockConfigs[i][1];
                
                // Konumlandırma - yan yana 2 blok
                double x = 50 + (i * 450);
                double y = 80;
                double width = 400;
                double height = 500;
                
                String sql = "INSERT INTO facility_blocks (name, facility_id, current_id_index, pos_x, pos_y, width, height, color_hex) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, blockName, facilityId, 0, x, y, width, height, color);
                totalBlocks++;
            }
        }
        
        System.out.println("   [OK] " + totalBlocks + " blok eklendi (her kutuphanede 2 blok)");
    }

    /**
     * 6. DESKS Tablosu - Her blokta 3-4 masa
     */
    private void populateDesks() {
        System.out.println("\n[INFO] DESKS tablosu dolduruluyor...");
        
        String[] deskColors = {"#FAFAFA", "#F5F5F5", "#EEEEEE", "#E0E0E0"};
        
        Integer blockCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facility_blocks", Integer.class);
        
        int totalDesks = 0;
        for (long blockId = 1; blockId <= blockCount; blockId++) {
            // Her blokta 3-4 masa
            int deskCount = 3 + random.nextInt(2);
            
            for (int i = 0; i < deskCount; i++) {
                // SADECE 2 veya 4 sandalye
                int size = random.nextBoolean() ? 2 : 4;
                
                String idRange = "D" + blockId + "-" + (i + 1);
                String color = deskColors[random.nextInt(deskColors.length)];
                
                // Grid düzeninde konumlandırma
                int row = i / 2;
                int col = i % 2;
                double x = 30 + (col * 180);
                double y = 60 + (row * 160);
                double width = 150;
                double height = 120;
                
                String sql = "INSERT INTO desks (size, id_range, current_id_index, facility_block_id, pos_x, pos_y, width, height, color_hex) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, size, idRange, 0, blockId, x, y, width, height, color);
                totalDesks++;
            }
        }
        
        System.out.println("   [OK] " + totalDesks + " masa eklendi (her masada 2 veya 4 sandalye)");
    }

    /**
     * 7. SEATS Tablosu - Her masada 2 veya 4 sandalye
     */
    private void populateSeats() {
        System.out.println("\n[INFO] SEATS tablosu dolduruluyor...");
        
        var desks = jdbcTemplate.queryForList("SELECT id, size FROM desks");
        
        int totalSeats = 0;
        int twoSeatDesks = 0;
        int fourSeatDesks = 0;
        
        for (var desk : desks) {
            Long deskId = ((Number) desk.get("id")).longValue();
            Integer size = (Integer) desk.get("size");
            
            if (size == 2) {
                twoSeatDesks++;
                // 2 kişilik masa - karşılıklı oturma
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    1, 0.25, 0.5, deskId);
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    2, 0.75, 0.5, deskId);
                totalSeats += 2;
            } else {
                fourSeatDesks++;
                // 4 kişilik masa - köşelerde oturma
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    1, 0.2, 0.25, deskId);
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    2, 0.8, 0.25, deskId);
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    3, 0.2, 0.75, deskId);
                jdbcTemplate.update("INSERT INTO seats (seat_number, rel_x, rel_y, desk_id) VALUES (?, ?, ?, ?)",
                    4, 0.8, 0.75, deskId);
                totalSeats += 4;
            }
        }
        
        System.out.println("   [OK] " + totalSeats + " sandalye eklendi");
        System.out.println("        -> " + twoSeatDesks + " adet 2'li masa | " + fourSeatDesks + " adet 4'lu masa");
    }

    /**
     * 8. RESERVATIONS Tablosu - Gercekci rezervasyonlar
     * NOT: Her kullanici maksimum 3 aktif rezervasyona sahip olabilir (trigger kontrolu)
     */
    private void populateReservations() {
        System.out.println("\n[INFO] RESERVATIONS tablosu dolduruluyor...");
        
        var users = jdbcTemplate.queryForList("SELECT id FROM users WHERE user_role = 'USER'");
        
        var seats = jdbcTemplate.queryForList(
            "SELECT s.id as seat_id, s.desk_id, d.facility_block_id, fb.facility_id " +
            "FROM seats s " +
            "JOIN desks d ON s.desk_id = d.id " +
            "JOIN facility_blocks fb ON d.facility_block_id = fb.id"
        );
        
        int activeCount = 0;
        int completedCount = 0;
        int cancelledCount = 0;
        
        // Gecmis rezervasyonlar (COMPLETED) - trigger kontrolu yok
        for (int i = 0; i < 30; i++) {
            var seat = seats.get(random.nextInt(seats.size()));
            var user = users.get(random.nextInt(users.size()));
            
            LocalDate date = LocalDate.now().minusDays(1 + random.nextInt(14));
            int startHour = 9 + random.nextInt(8);
            LocalTime startTime = LocalTime.of(startHour, 0);
            LocalTime endTime = startTime.plusHours(2 + random.nextInt(3));
            
            createReservation(user, seat, date, startTime, endTime, "COMPLETED");
            completedCount++;
        }
        
        // Iptal edilmis rezervasyonlar - trigger kontrolu yok
        for (int i = 0; i < 10; i++) {
            var seat = seats.get(random.nextInt(seats.size()));
            var user = users.get(random.nextInt(users.size()));
            
            LocalDate date = LocalDate.now().minusDays(random.nextInt(7));
            int startHour = 9 + random.nextInt(8);
            LocalTime startTime = LocalTime.of(startHour, 0);
            LocalTime endTime = startTime.plusHours(2 + random.nextInt(3));
            
            createReservation(user, seat, date, startTime, endTime, "CANCELLED");
            cancelledCount++;
        }
        
        // Aktif rezervasyonlar - her kullaniciya en fazla 3 tane (trigger limiti)
        // 15 user x 3 rezervasyon = maksimum 45 aktif rezervasyon
        java.util.Map<Long, Integer> userActiveCount = new java.util.HashMap<>();
        int maxActivePerUser = 3;
        int targetActiveReservations = 35; // 15 user icin makul sayi
        
        int attempts = 0;
        while (activeCount < targetActiveReservations && attempts < 200) {
            attempts++;
            var seat = seats.get(random.nextInt(seats.size()));
            var user = users.get(random.nextInt(users.size()));
            Long userId = ((Number) user.get("id")).longValue();
            
            // Bu kullanicinin kac aktif rezervasyonu var kontrol et
            int currentUserActive = userActiveCount.getOrDefault(userId, 0);
            if (currentUserActive >= maxActivePerUser) {
                continue; // Bu kullanici dolu, baska kullanici dene
            }
            
            LocalDate date = LocalDate.now().plusDays(1 + random.nextInt(7));
            int startHour = 9 + random.nextInt(10);
            LocalTime startTime = LocalTime.of(startHour, 0);
            LocalTime endTime = startTime.plusHours(1 + random.nextInt(3));
            
            try {
                createReservation(user, seat, date, startTime, endTime, "ACTIVE");
                userActiveCount.put(userId, currentUserActive + 1);
                activeCount++;
            } catch (Exception e) {
                // Cakisma veya baska hata - devam et
            }
        }
        
        System.out.println("   [OK] " + (activeCount + completedCount + cancelledCount) + " rezervasyon eklendi");
        System.out.println("        -> " + activeCount + " ACTIVE | " + completedCount + " COMPLETED | " + cancelledCount + " CANCELLED");
    }
    
    private void createReservation(java.util.Map<String, Object> user, java.util.Map<String, Object> seat,
                                   LocalDate date, LocalTime startTime, LocalTime endTime, String status) {
        Long userId = ((Number) user.get("id")).longValue();
        Long seatId = ((Number) seat.get("seat_id")).longValue();
        Long deskId = ((Number) seat.get("desk_id")).longValue();
        Long blockId = ((Number) seat.get("facility_block_id")).longValue();
        Long facilityId = ((Number) seat.get("facility_id")).longValue();
        
        String sql = "INSERT INTO reservations (user_id, seat_id, desk_id, facility_block_id, facility_id, " +
                   "reservation_date, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, userId, seatId, deskId, blockId, facilityId, date, startTime, endTime, status);
    }

    /**
     * 9. LIBRARIAN_FACILITIES Tablosu - Kutuphanecileri tesislere ata
     */
    private void populateLibrarianFacilities() {
        System.out.println("\n[INFO] LIBRARIAN_FACILITIES tablosu dolduruluyor...");
        
        var librarians = jdbcTemplate.queryForList("SELECT id FROM users WHERE user_role = 'LIBRARIAN'");
        
        int assignmentCount = 0;
        
        // İlk 15 kütüphaneci - her kütüphaneye 1'er
        for (int i = 0; i < Math.min(15, librarians.size()); i++) {
            Long librarianId = ((Number) librarians.get(i).get("id")).longValue();
            Long facilityId = (long) (i + 1);
            
            String sql = "INSERT INTO librarian_facilities (user_id, facility_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, librarianId, facilityId);
            assignmentCount++;
        }
        
        // Kalan 3 kütüphaneci - ilk 3 kütüphaneye ikinci kütüphaneci
        for (int i = 15; i < librarians.size(); i++) {
            Long librarianId = ((Number) librarians.get(i).get("id")).longValue();
            Long facilityId = (long) ((i - 15) + 1);
            
            String sql = "INSERT INTO librarian_facilities (user_id, facility_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, librarianId, facilityId);
            assignmentCount++;
        }
        
        System.out.println("   [OK] " + assignmentCount + " kutuphaneci atamasi yapildi");
    }
    
    /**
     * Ozet bilgileri yazdir
     */
    private void printSummary() {
        Integer totalFacilities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facilities", Integer.class);
        Integer totalBlocks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facility_blocks", Integer.class);
        Integer totalDesks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM desks", Integer.class);
        Integer totalSeats = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seats", Integer.class);
        Integer totalReservations = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservations", Integer.class);
        
        System.out.println("\n========================================================");
        System.out.println("       [OK] VERITABANI BASARIYLA DOLDURULDU");
        System.out.println("========================================================");
        System.out.println("  Kutuphaneler:    " + totalFacilities);
        System.out.println("  Bloklar:         " + totalBlocks + " (her kutuphanede 2 blok)");
        System.out.println("  Masalar:         " + totalDesks);
        System.out.println("  Sandalyeler:     " + totalSeats + " (2 veya 4 kisilik masalar)");
        System.out.println("  Rezervasyonlar:  " + totalReservations);
        System.out.println("--------------------------------------------------------");
        System.out.println("  Test Girisleri:");
        System.out.println("     Admin:     berk.kocak@studyflow.com / password123");
        System.out.println("     Librarian: yusuf.sen@studyflow.com / password123");
        System.out.println("     User:      ahmet.yilmaz@studyflow.com / password123");
        System.out.println("========================================================");
    }
}
