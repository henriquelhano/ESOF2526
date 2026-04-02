package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.repository.ActivityRepository;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.repository.ShiftRepository;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ACTIVITY_NOT_FOUND;
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.SHIFT_INFORMATION_REQUIRED;

import java.util.Comparator;
import java.util.List;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShiftDto createShift(Integer activityId, ShiftDto shiftDto) {
        if (activityId == null) {
            throw new HEException(ACTIVITY_NOT_FOUND);
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new HEException(ACTIVITY_NOT_FOUND, activityId));

        if (shiftDto == null) {
            throw new HEException(SHIFT_INFORMATION_REQUIRED);
        }

        Shift shift = new Shift(activity, shiftDto);
        shiftRepository.save(shift);

        return new ShiftDto(shift);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ShiftDto> getShiftsByActivity(Integer activityId) {
        if (activityId == null) {
            throw new HEException(ACTIVITY_NOT_FOUND);
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new HEException(ACTIVITY_NOT_FOUND, activityId));

        return activity.getShifts().stream()
                .sorted(Comparator.comparing(Shift::getStartTime))
                .map(ShiftDto::new)
                .toList();
    }
}
