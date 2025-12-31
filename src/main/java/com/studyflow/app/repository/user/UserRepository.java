package com.studyflow.app.repository.user;

import com.studyflow.app.model.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "SELECT COUNT(*) FROM users WHERE email = :email", nativeQuery = true)
    int checkIfEmailExists(@Param("email") String email);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO users(email, password, first_name, last_name, user_role) " +
            "VALUES(:email, :password, :firstName, :lastName, :userRole)", nativeQuery = true)
    void saveNewUser(@Param("email") String email,
                     @Param("password") String password,
                     @Param("firstName") String firstName,
                     @Param("lastName") String lastName,
                     @Param("userRole") String userRole);

    @Query(value = "SELECT password FROM users WHERE email = :email", nativeQuery = true)
    String getUserPassword(@Param("email") String email);

    @Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
    User getUserByEmailAddress(String email);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users " +
            "SET user_role = :userRole " +
            "WHERE email = :email",
            nativeQuery = true)
    void updateUserRole(@Param("email") String email, @Param("userRole") String userRole);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO librarian_facilities (facility_id, user_id) " +
            "VALUES (:facilityId, :userId)",
            nativeQuery = true)
    void insertUserFacility(@Param("userId") Long userId,
                            @Param("facilityId") Long facilityId);

}
