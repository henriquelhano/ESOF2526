package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
@Table(name = "shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer participantsLimit;

    @ManyToOne
    private Activity activity;

    @ManyToMany(mappedBy = "shifts")
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "shift")
    private List<Participation> participations = new ArrayList<>();

    public Shift() {}

    public Shift(Activity activity, ShiftDto shiftDto) {
        setActivity(activity);
        setLocation(shiftDto.getLocation());
        setStartTime(DateHandler.toLocalDateTime(shiftDto.getStartTime()));
        setEndTime(DateHandler.toLocalDateTime(shiftDto.getEndTime()));
        setParticipantsLimit(shiftDto.getParticipantsLimit());
        
        verifyInvariants();

        activity.addShift(this);
    }

    public Integer getId() { 
        return id; 
    }

    public String getLocation() { 
        return location; 
    }

    public void setLocation(String location) { 
        this.location = location; 
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

    public Activity getActivity() { 
        return activity; 
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    
    public List<Enrollment> getEnrollments() { 
        return enrollments; 
    }

    public void setEnrollments(List<Enrollment> enrollments) { 
        this.enrollments = enrollments; 
    }
    
    public List<Participation> getParticipations() { 
        return participations; 
    }

    public void setParticipations(List<Participation> participations) { 
        this.participations = participations; 
    }
    
    public void deleteParticipation(Participation participation) {
        this.participations.remove(participation);
    }

    public void addParticipation(Participation participation) {
        this.participations.add(participation);
    }

    private void verifyInvariants() {
        locationIsValid();
        startBeforeEnd();
        datesWithinActivity();
        participantsLimitIsPositive();
        activityIsApproved();
        participantsLimitDoesNotExceedActivity();
    }

    private void locationIsValid() {
        if (this.location == null || this.location.trim().length() < 20 || this.location.trim().length() > 200) {
            throw new HEException(SHIFT_LOCATION_INVALID, this.location);
        }
    }

    private void startBeforeEnd() {
        if (!this.startTime.isBefore(this.endTime)) {
            throw new HEException(SHIFT_START_AFTER_END);
        }
    }

    private void datesWithinActivity() {
        if (this.startTime.isBefore(this.activity.getStartingDate()) ||
                this.endTime.isAfter(this.activity.getEndingDate())) {
            throw new HEException(SHIFT_DATES_OUTSIDE_ACTIVITY);
        }
    }

    private void participantsLimitIsPositive() {
        if (this.participantsLimit == null || this.participantsLimit <= 0) {
            throw new HEException(SHIFT_PARTICIPANTS_LIMIT_INVALID);
        }
    }

    private void activityIsApproved() {
        if (this.activity.getState() != Activity.State.APPROVED) {
            throw new HEException(SHIFT_ACTIVITY_NOT_APPROVED);
        }
    }

    private void participantsLimitDoesNotExceedActivity() {
        int existingLimits = this.activity.getShifts().stream()
                .mapToInt(Shift::getParticipantsLimit).sum();

        int total = existingLimits + this.participantsLimit;

        if (total > this.activity.getParticipantsNumberLimit()) {
            throw new HEException(ErrorMessage.SHIFT_PARTICIPANTS_LIMIT_EXCEEDS_ACTIVITY);
        }
    }
}