package com.template.identity.infrastructure;

import com.template.identity.domain.profiles.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Query("select p from Profile p where p.user.externalId = :externalId")
    List<Profile> findByUserExternalId(@Param("externalId") String externalId);

    @Query("select p from Profile p where p.user.id = :userId")
    List<Profile> findByUserId(@Param("userId") Long userId);
}
