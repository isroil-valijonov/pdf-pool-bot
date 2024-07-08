package com.example.pdfpoolbot.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users WHERE DATE(created_date) = CAST(:date AS DATE)", nativeQuery = true)
    List<User> findUsersByCreationDate(@Param("date") String date);

    @Query(value = "SELECT COUNT(*) FROM users WHERE DATE_TRUNC('month', created_date) = DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')", nativeQuery = true)
    Long countUsersLastMonth();

    @Query(value = "SELECT COUNT(*) FROM users WHERE DATE_TRUNC('month', created_date) = DATE_TRUNC('month', CURRENT_DATE)", nativeQuery = true)
    Long countUsersThisMonth();

    @Query(value = "SELECT * FROM users WHERE DATE_TRUNC('month', created_date) = DATE_TRUNC('month', CURRENT_DATE)", nativeQuery = true)
    List<User> findActiveUsersThisMonth();

}
