package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;

import java.util.List;

@Repository
@Transactional
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    @Query("SELECT s FROM Shift s WHERE s.activity.id = :activityId")
    List<Shift> getShiftsByActivityId(Integer activityId);
}