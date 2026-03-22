package com.smartcourier.admin.repository;

import com.smartcourier.admin.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HubRepository extends JpaRepository<Hub, Long> {
    List<Hub> findByIsActiveTrue();
    boolean existsByName(String name);
}
