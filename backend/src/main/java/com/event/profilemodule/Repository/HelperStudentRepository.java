package com.event.profilemodule.Repository;

import com.event.profilemodule.entity.student.ProfileStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HelperStudentRepository extends JpaRepository<ProfileStudent, Long> {
    List<ProfileStudent> findByPreferences_PreferenceType(String preferenceType);

    /** [id, name] of every profile with a name filled in. */
    @Query("select p.id, p.name from ProfileStudent p where p.name is not null and p.name <> ''")
    List<Object[]> findIdAndName();

    // Atomic create: concurrent first requests for a new user no longer collide on the primary key
    @Modifying
    @Query(value = "INSERT INTO profile.profile_student (id, privacy) VALUES (:id, false) ON CONFLICT (id) DO NOTHING",
            nativeQuery = true)
    void insertIfAbsent(@Param("id") Long id);

}