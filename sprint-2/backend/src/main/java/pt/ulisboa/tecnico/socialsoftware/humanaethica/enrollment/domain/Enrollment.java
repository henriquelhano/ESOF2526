package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @OneToOne(mappedBy = "enrollment")
    private Participation participation;

    public Enrollment() {
    }

    public Enrollment(Volunteer volunteer, List<Shift> shifts, EnrollmentDto enrollmentDto) {
        setVolunteer(volunteer);
        setMotivation(enrollmentDto.getMotivation());
        setEnrollmentDateTime(LocalDateTime.now());
        shifts.forEach(this::addShift);

        verifyInvariants();
    }

    public void update(EnrollmentDto enrollmentDto) {
        setMotivation(enrollmentDto.getMotivation());

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public void delete() {
        volunteer.removeEnrollment(this);
        shifts.forEach(shift -> shift.removeEnrollment(this));

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

    public Activity getActivity() {
        if (shifts.isEmpty()) {
            return null;
        }
        return shifts.getFirst().getActivity();
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        this.volunteer.addEnrollment(this);
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public void addShift(Shift shift) {
        this.shifts.add(shift);
        shift.addEnrollment(this);
    }

    public Participation getParticipation() {
        return participation;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
    }

    private void verifyInvariants() {
        motivationIsRequired();
        atLeastOneShift();
        enrollBeforeDeadline();
        enrollOnce();
        shiftsActivityConsistency();
        shiftsHaveOverlappingTime();
    }

    private void motivationIsRequired() {
        if (this.motivation == null || this.motivation.trim().length() < 10) {
            throw new HEException(ENROLLMENT_REQUIRES_MOTIVATION);
        }
    }

    private void enrollOnce() {
        Activity myActivity = getActivity();

        if (this.volunteer.getEnrollments().stream()
                .filter(enrollment -> enrollment != this)
                .map(Enrollment::getActivity)
                .anyMatch(activity -> activity != null && activity.getId().equals(myActivity.getId()))) {
            throw new HEException(ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED);
        }
    }

    private void enrollBeforeDeadline() {
        if (this.enrollmentDateTime.isAfter(getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }

    private void atLeastOneShift() {
        if (this.shifts.isEmpty()) {
            throw new HEException(ENROLLMENT_AT_LEAST_ONE_SHIFT);
        }
    }

    private void shiftsActivityConsistency() {
        if (this.shifts.stream().map(Shift::getActivity).distinct().count() != 1) {
            throw new HEException(ENROLLMENT_SHIFTS_MUST_BELONG_TO_SAME_ACTIVITY);
        }
    }

    private void shiftsHaveOverlappingTime() {
        for (Shift shift1 : this.shifts) {
            for (Shift shift2 : this.shifts) {
                if (shift1 != shift2 && overlaps(shift1, shift2)) {
                    throw new HEException(ENROLLMENT_SHIFTS_HAVE_OVERLAPPING_TIME);
                }
            }
        }
    }

    private boolean overlaps(Shift shift1, Shift shift2) {
        return shift1.getStartTime().isBefore(shift2.getEndTime()) &&
                shift2.getStartTime().isBefore(shift1.getEndTime());
    }

    private void editOrDeleteEnrollmentBeforeDeadline() {
        if (LocalDateTime.now().isAfter(getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }
}
