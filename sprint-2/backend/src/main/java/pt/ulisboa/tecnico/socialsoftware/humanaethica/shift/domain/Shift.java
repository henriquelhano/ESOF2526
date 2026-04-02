package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer participantsLimit;
    private String location;
    @ManyToOne
    private Activity activity;
    @ManyToMany(mappedBy = "shifts")
    private List<Enrollment> enrollments = new ArrayList<>();
    @OneToMany(mappedBy = "shift")
    private List<Participation> participations = new ArrayList<>();

    public Shift() {
    }

    public Shift(Activity activity, ShiftDto shiftDto) {
        setActivity(activity);
        setStartTime(DateHandler.toLocalDateTime(shiftDto.getStartTime()));
        setEndTime(DateHandler.toLocalDateTime(shiftDto.getEndTime()));
        setParticipantsLimit(shiftDto.getParticipantsLimit());
        setLocation(shiftDto.getLocation());

        verifyInvariants();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getParticipantsLimit() {
        return participantsLimit;
    }

    public void setParticipantsLimit(Integer participantsLimit) {
        this.participantsLimit = participantsLimit;
    }

    public Integer getCurrentParticipants() {
        return participations.size();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        if (activity != null) {
            activity.addShift(this);
        }
    }

    public List<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public void addParticipation(Participation participation) {
        this.participations.add(participation);
    }

    public void removeParticipation(Participation participation) {
        this.participations.remove(participation);
    }

    private void verifyInvariants() {
        startTimeIsRequired();
        endTimeIsRequired();
        participantsLimitIsPositive();
        locationIsRequired();
        locationLengthIsValid();
        startTimeBeforeEndTime();
        shiftDatesWithinActivity();
        currentParticipantsWithinLimit();
        checkActivityIsApproved();
        checkTotalParticipantsLimit();
    }

    private void checkActivityIsApproved() {
        if (this.activity.getState() != Activity.State.APPROVED) {
            throw new HEException(SHIFT_ON_NON_APPROVED_ACTIVITY);
        }
    }

    private void checkTotalParticipantsLimit() {
        int totalParticipants = this.activity.getShifts().stream()
                .filter(shift -> shift != this)
                .mapToInt(Shift::getParticipantsLimit)
                .sum();

        if (totalParticipants + this.participantsLimit > this.activity.getParticipantsNumberLimit()) {
            throw new HEException(TOTAL_PARTICIPANTS_EXCEEDS_ACTIVITY_LIMIT);
        }
    }

    private void startTimeIsRequired() {
        if (this.startTime == null) {
            throw new HEException(SHIFT_START_TIME_REQUIRED);
        }
    }

    private void endTimeIsRequired() {
        if (this.endTime == null) {
            throw new HEException(SHIFT_END_TIME_REQUIRED);
        }
    }

    private void startTimeBeforeEndTime() {
        if (!this.startTime.isBefore(this.endTime)) {
                throw new HEException(SHIFT_START_TIME_BEFORE_END_TIME);
        }
    }

    private void shiftDatesWithinActivity() {
        LocalDateTime activityStart = this.activity.getStartingDate();
        LocalDateTime activityEnd = this.activity.getEndingDate();

        if (activityStart != null && activityEnd != null) {
            boolean startWithinRange = !this.startTime.isBefore(activityStart)
                    && !this.startTime.isAfter(activityEnd);
            boolean endWithinRange = !this.endTime.isBefore(activityStart) && !this.endTime.isAfter(activityEnd);

            if (!startWithinRange || !endWithinRange) {
                throw new HEException(SHIFT_DATES_WITHIN_ACTIVITY);
            }
        }
    }

    private void participantsLimitIsPositive() {
        if (this.participantsLimit == null || this.participantsLimit <= 0) {
            throw new HEException(SHIFT_PARTICIPANTS_LIMIT_POSITIVE);
        }
    }

    private void locationIsRequired() {
        if (this.location == null) {
            throw new HEException(SHIFT_LOCATION_REQUIRED);
        }
    }

    private void locationLengthIsValid() {
        int length = this.location.trim().length();
        if (length < 20 || length > 200) {
            throw new HEException(SHIFT_LOCATION_INVALID);
        }
    }

    private void currentParticipantsWithinLimit() {
        if (getCurrentParticipants() > this.participantsLimit) {
            throw new HEException(SHIFT_CURRENT_PARTICIPANTS_EXCEEDS_LIMIT);
        }
    }
}