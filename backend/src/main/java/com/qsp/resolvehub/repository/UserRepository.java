package com.qsp.resolvehub.repository;

import com.qsp.resolvehub.entity.Role;
import com.qsp.resolvehub.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer id);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Transactional
    @Query("""
        UPDATE User u
        SET u.tokenVersion = u.tokenVersion + 1
        WHERE u.id = :userId
        """)
    int incrementTokenVersion(@Param("userId") Integer userId);

    long countByRoles(Role roles);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT u FROM User u
       WHERE u.roles = :role
       AND u.account_status = true
       ORDER BY u.id
       """)
    List<User> findActiveAgentsForUpdate(
            @Param("role") Role role
    );

    
}