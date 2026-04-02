package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

import java.util.List;
import java.util.ArrayList;

public class EnrollmentDto {
    private Integer id;
    private Integer activityId;
    private Integer volunteerId;
    private List<Integer> shiftIds = new ArrayList<>();
    private String volunteerName;
    private String motivation;
    private String enrollmentDateTime;
    private boolean isParticipating;


    public EnrollmentDto() {}

    public EnrollmentDto(Enrollment enrollment) {
        this.id = enrollment.getId();
        this.activityId = enrollment.getShifts().stream().filter(shift -> shift.getActivity() != null)
                .map(shift -> shift.getActivity().getId())
                .findFirst()
                .orElse(null);
        this.volunteerId = enrollment.getVolunteer().getId();
        this.volunteerName = enrollment.getVolunteer().getName();
        this.motivation = enrollment.getMotivation();
        this.enrollmentDateTime = DateHandler.toISOString(enrollment.getEnrollmentDateTime());
        this.isParticipating = enrollment.getShifts().stream().filter(shift -> shift.getActivity() != null && shift.getActivity().getParticipations() != null)
                .flatMap(shift -> shift.getActivity().getParticipations().stream())
                .anyMatch(p -> p.getVolunteer().getId().equals(enrollment.getVolunteer().getId()));             
        this.shiftIds = enrollment.getShifts().stream()
                .map(Shift::getId)
                .toList();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getEnrollmentDateTime() {
        return enrollmentDateTime;
    }

    public void setEnrollmentDateTime(String enrollmentDateTime) {
        this.enrollmentDateTime = enrollmentDateTime;
    }

    public boolean isParticipating() {
        return isParticipating;
    }

    public void setParticipating(boolean participating) {
        isParticipating = participating;
    }

    public List<Integer> getShiftIds() {
        return shiftIds;
    }

    public void setShiftIds(List<Integer> shiftIds) {
        this.shiftIds = shiftIds;
    }
}