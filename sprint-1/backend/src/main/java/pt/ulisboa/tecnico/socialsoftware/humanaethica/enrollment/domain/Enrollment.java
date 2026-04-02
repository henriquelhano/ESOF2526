package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Integer id;
    private String motivation;
    private LocalDateTime enrollmentDateTime;
    @ManyToOne
    private Volunteer volunteer;
    @ManyToMany
    private List<Shift> shifts = new ArrayList<>();
    @OneToMany(mappedBy = "enrollment")
    private List<Participation> participations = new ArrayList<>();

    public Enrollment() {}

    public Enrollment(Volunteer volunteer, List<Shift> shifts, EnrollmentDto enrollmentDto) {
        setVolunteer(volunteer);
        setShifts(shifts);
        setMotivation(enrollmentDto.getMotivation());
        setEnrollmentDateTime(LocalDateTime.now());

        verifyInvariants();
    }

    public void update(EnrollmentDto enrollmentDto) {  
        setMotivation(enrollmentDto.getMotivation());

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public void delete(){
        volunteer.removeEnrollment(this);

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public LocalDateTime getEnrollmentDateTime() {
        return enrollmentDateTime;
    }

    public void setEnrollmentDateTime(LocalDateTime enrollmentDateTime) {
        this.enrollmentDateTime = enrollmentDateTime;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        this.volunteer.addEnrollment(this);
    }

    public List<Participation> getParticipations() {
        return participations;
    }
    
    public List<Shift> getShifts() {
        return shifts;
    }

    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }

    private void verifyInvariants() {
        motivationIsRequired();
        enrollOnce();
        enrollBeforeDeadline();
        shiftsFromSameActivity();
        shiftsDoNotOverlap();
    }

    private void motivationIsRequired() {
        if (this.motivation == null || this.motivation.trim().length() < 10) {
            throw new HEException(ENROLLMENT_REQUIRES_MOTIVATION);
        }
    }

    private void enrollOnce() {
        if (this.getShifts().stream()
                .flatMap(shift -> shift.getEnrollments().stream())
                .anyMatch(enrollment -> enrollment != this && enrollment.getVolunteer() == this.volunteer)) {
            throw new HEException(ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED);
        }
    }

    private void enrollBeforeDeadline() {
        shifts.stream()
                .map(shift -> shift.getActivity().getApplicationDeadline())
                .filter(deadline -> this.enrollmentDateTime.isAfter(deadline))
                .findFirst()
                .ifPresent(deadline -> { throw new HEException(ENROLLMENT_AFTER_DEADLINE); });
    }

    private void editOrDeleteEnrollmentBeforeDeadline() {
        shifts.stream()
                .map(shift -> shift.getActivity().getApplicationDeadline())
                .filter(deadline -> LocalDateTime.now().isAfter(deadline))
                .findFirst()
                .ifPresent(deadline -> { throw new HEException(ENROLLMENT_AFTER_DEADLINE); });
    }

    
    private void shiftsFromSameActivity() {
        if (shifts.stream().map(shift -> shift.getActivity().getId()).distinct().count() > 1) {
            throw new HEException(ENROLLMENT_SHIFTS_FROM_DIFFERENT_ACTIVITIES);
        }
    }

    private void shiftsDoNotOverlap() {
        if (shifts.stream().anyMatch(a -> shifts.stream().filter(b -> b != a)
            .anyMatch(b -> a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())))) {
            throw new HEException(ENROLLMENT_SHIFTS_OVERLAP);
        }
    }
}