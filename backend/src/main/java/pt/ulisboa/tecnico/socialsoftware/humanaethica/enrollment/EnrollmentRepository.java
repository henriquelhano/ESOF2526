package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;

import java.util.List;

@Repository
@Transactional
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    @Query("SELECT DISTINCT e FROM Enrollment e JOIN e.shifts s WHERE s.id = :shiftId")
    List<Enrollment> getEnrollmentsByShiftId(Integer shiftId);

    @Query("SELECT e FROM Enrollment e WHERE e.volunteer.id = :userId")
    List<Enrollment> getEnrollmentsForVolunteerId(Integer userId);
}
