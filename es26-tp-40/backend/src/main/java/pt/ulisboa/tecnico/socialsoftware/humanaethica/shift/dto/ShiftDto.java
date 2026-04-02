package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto;

import java.util.List;
import java.util.ArrayList;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

public class ShiftDto {
    private Integer id;
    private Integer activityId;
    private String location;
    private String startTime;
    private String endTime;
    private Integer participantsLimit;
    private List<Integer> enrollmentIds = new ArrayList<>();
    private List<Integer> participationIds = new ArrayList<>();

    public ShiftDto() {}

    public ShiftDto(Shift shift) {
        this.id = shift.getId();
        this.activityId = shift.getActivity().getId();
        this.location = shift.getLocation();
        this.startTime = DateHandler.toISOString(shift.getStartTime());
        this.endTime = DateHandler.toISOString(shift.getEndTime());
        this.participantsLimit = shift.getParticipantsLimit();
        this.enrollmentIds = shift.getEnrollments().stream()
                .map(enrollment -> enrollment.getId())
                .toList();
        this.participationIds = shift.getParticipations().stream()
                .map(participation -> participation.getId())
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

    public String getLocation() { 
        return location; 
    }

    public void setLocation(String location) { 
        this.location = location; 
    }

    public String getStartTime() { 
        return startTime; 
    }

    public void setStartTime(String startTime) { 
        this.startTime = startTime; 
    }

    public String getEndTime() { 
        return endTime; 
    }

    public void setEndTime(String endTime) { 
        this.endTime = endTime; 
    }
    
    public List<Integer> getEnrollmentIds() {
        return enrollmentIds;
    }

    public void setEnrollmentIds(List<Integer> enrollmentIds) {
        this.enrollmentIds = enrollmentIds;
    }

    public Integer getParticipantsLimit() { 
        return participantsLimit; 
    }

    public void setParticipantsLimit(Integer participantsLimit) { 
        this.participantsLimit = participantsLimit; 
    }

    public List<Integer> getParticipationIds() { 
        return participationIds; 
    }

    public void setParticipationIds(List<Integer> participationIds) { 
        this.participationIds = participationIds; 
    }
}