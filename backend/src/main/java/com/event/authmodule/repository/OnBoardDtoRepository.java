package com.event.authmodule.repository;


import com.event.authmodule.entitys.OnBoard;
import com.event.authmodule.entitys.type.RoleType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OnBoardDtoRepository extends JpaRepository<OnBoard, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM OnBoard o WHERE o.username = :username AND o.roleType = :roleType")
    void deleteByUsernameAndRoleType(String username, RoleType roleType);
}