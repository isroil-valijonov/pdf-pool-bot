package com.example.pdfpoolbot.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query(nativeQuery = true, value = "select admin_id from admin order by admin_id")
    List<Long> getAllAdminId();
}
