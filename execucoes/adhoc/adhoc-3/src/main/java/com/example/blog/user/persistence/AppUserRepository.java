package com.example.blog.user.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    // lower() casa com o índice único uq_app_user_email.
    @Query("select u from AppUser u where lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailIgnoringCase(@Param("email") String email);

    @Query("select count(u) > 0 from AppUser u where lower(u.email) = lower(:email)")
    boolean existsByEmailIgnoringCase(@Param("email") String email);

    @Query("select count(u) > 0 from AppUser u where lower(u.email) = lower(:email) and u.id <> :id")
    boolean existsByEmailIgnoringCaseAndIdNot(@Param("email") String email, @Param("id") Long id);
}
