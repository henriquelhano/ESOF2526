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

import java.util.Comparator;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Service
public class ShiftService {
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ShiftRepository shiftRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ShiftDto> getShiftsByActivity(Integer activityId) {
        if (activityId == null) {
            throw new HEException(ACTIVITY_NOT_FOUND);
        }
        activityRepository.findById(activityId).orElseThrow(() -> new HEException(ACTIVITY_NOT_FOUND, activityId));

        return shiftRepository.getShiftsByActivityId(activityId).stream().sorted(Comparator.comparing(Shift::getEndTime))
                .map(ShiftDto::new)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShiftDto createShift(Integer activityId, ShiftDto shiftDto) {
        if (shiftDto == null) {
            throw new HEException(SHIFT_NOT_FOUND);
        }
        if (activityId == null) {
            throw new HEException(ACTIVITY_NOT_FOUND);
        }
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> new HEException(ACTIVITY_NOT_FOUND, activityId));

        Shift shift = new Shift(activity, shiftDto);
        shiftRepository.save(shift);

        return new ShiftDto(shift);
    }
}