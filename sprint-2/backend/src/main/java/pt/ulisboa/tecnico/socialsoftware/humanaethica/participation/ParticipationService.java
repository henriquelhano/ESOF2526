package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.repository.ActivityRepository;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.EnrollmentRepository;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.repository.ShiftRepository;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Service
public class ParticipationService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ParticipationRepository participationRepository;
    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ParticipationDto> getParticipationsByActivity(Integer activityId) {
        if (activityId == null)
            throw new HEException(ACTIVITY_NOT_FOUND);
        activityRepository.findById(activityId).orElseThrow(() -> new HEException(ACTIVITY_NOT_FOUND, activityId));

        return participationRepository.getParticipationsByActivityId(activityId).stream()
                .sorted(Comparator.comparing(Participation::getAcceptanceDate))
                .map(participation -> new ParticipationDto(participation, User.Role.MEMBER))
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<ParticipationDto> getVolunteerParticipations(Integer userId) {
        if (userId == null)
            throw new HEException(USER_NOT_FOUND);

        return participationRepository.getParticipationsForVolunteerId(userId).stream()
                .sorted(Comparator.comparing(Participation::getAcceptanceDate))
                .map(participation -> new ParticipationDto(participation, User.Role.VOLUNTEER))
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ParticipationDto memberRating(Integer participationId, ParticipationDto participationDto) {
        if (participationId == null)
            throw new HEException(PARTICIPATION_NOT_FOUND);
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new HEException(PARTICIPATION_NOT_FOUND, participationId));

        participation.memberRating(participationDto);
        return new ParticipationDto(participation, User.Role.MEMBER);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ParticipationDto volunteerRating(Integer participationId, ParticipationDto participationDto) {
        if (participationId == null)
            throw new HEException(PARTICIPATION_NOT_FOUND);
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new HEException(PARTICIPATION_NOT_FOUND, participationId));

        participation.volunteerRating(participationDto);
        return new ParticipationDto(participation, User.Role.VOLUNTEER);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ParticipationDto createParticipation(Integer shiftId, Integer enrollmentId, ParticipationDto participationDto) {
        if (enrollmentId == null)
            throw  new HEException(ENROLLMENT_NOT_FOUND);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new HEException(ENROLLMENT_NOT_FOUND));

        if (shiftId == null)
            throw new HEException(SHIFT_NOT_FOUND);
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new HEException(SHIFT_NOT_FOUND, shiftId));

        if (participationDto == null)
            throw new HEException(PARTICIPATION_REQUIRES_INFORMATION);

        Participation participation = new Participation(enrollment, shift, participationDto);
        participationRepository.save(participation);

        return new ParticipationDto(participation, User.Role.MEMBER);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ParticipationDto deleteParticipation(Integer participationId) {
        if (participationId == null)
            throw new HEException(PARTICIPATION_NOT_FOUND);
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new HEException(PARTICIPATION_NOT_FOUND, participationId));

        participation.delete();
        participationRepository.delete(participation);
        return new ParticipationDto(participation, User.Role.VOLUNTEER);
    }
}
