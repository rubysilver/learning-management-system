package com.krzelj.lms.repository;

import com.krzelj.lms.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken rt where rt.user.id = :userId")
    int deleteAllByUserId(@Param("userId") long userId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from RefreshToken rt where rt.tokenHash = :tokenHash")
    int deleteByTokenHashReturningCount(@Param("tokenHash") String tokenHash);
}

