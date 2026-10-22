package com.example.usermanagementservice.repository;

import com.example.usermanagementservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

//    Optional<User> findById(Long id);

    @Query(value = """
            SELECT u.email, u.role, COUNT(*) AS book_count
            FROM users u
            JOIN books b ON u.email = b.fetched_by
            GROUP BY u.email, u.role
            ORDER BY book_count DESC
            """, nativeQuery = true)
    List<Object[]> findUserRoleAndBookCount();

    @Query(value = """
            UPDATE users SET email = :newEmail, password_hash = :newPassword WHERE id = :userId
                        """, nativeQuery = true)
    User updateEmailAndPassword(@Param("userId") Long userId,
                               @Param("newEmail") String newEmail,
                               @Param("newPassword") String newPassword);

}
