package team.projectpulse.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);

    /**
     * Deactivates a student only if she is still active, in one statement, and returns the number of rows changed.
     *
     * <p>Core entities carry no {@code @Version}, so a read-then-write check would let two concurrent deactivations
     * both see an active student and both report success. Here the database decides: exactly one caller gets 1 and
     * every other caller gets 0 (UC-STU-deactivate-student, extension 3a). The persistence context is flushed before
     * and cleared after, so a later load in the same transaction sees the new state.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Student s SET s.enabled = false WHERE s.id = :id AND s.enabled = true")
    int deactivateIfActive(@Param("id") Integer id);

}
