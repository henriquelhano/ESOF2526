package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateEnrollmentMethodTest extends SpockTest {
    Activity activity = Mock()
    Shift shift = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Enrollment otherEnrollment = Mock()
    def enrolmentDto

    def setup() {
        given: "enrolment info"
        enrolmentDto = new EnrollmentDto()
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1
    }

    def "create enrollment"() {
        given:
        def shifts = [shift]
        shift.getActivity() >> activity
        shift.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        def result = new Enrollment(volunteer, shifts, enrolmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.volunteer == volunteer
        result.getShifts().contains(shift)
        result.getParticipations() != null
        result.getParticipations().isEmpty()
        
        and: "check that it is added"
        1 * volunteer.addEnrollment(_)
    }

    @Unroll
    def "create enrollment and violate motivation is required invariant: motivation=#motivation"() {
        given:
        def shifts = [shift]
        and:
        enrolmentDto.motivation = motivation

        when:
        new Enrollment(volunteer, shifts, enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        motivation | errorMessage
        null       | ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      | ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "<10"    | ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION 
    }

    def "create enrollment and violate enrollment before deadline invariant"() {
        given:
        def shifts = [shift]
        shift.getActivity() >> activity
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        shift.getEnrollments() >> []
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, shifts, enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    def "create enrollment and violate enroll once invariant"() {
        given:
        def shifts = [shift]
        shift.getActivity() >> activity
        activity.getApplicationDeadline() >> IN_ONE_DAY
        shift.getEnrollments() >> [otherEnrollment]
        otherEnrollment.getVolunteer() >> volunteer
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, shifts, enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED
    }

   def "create enrollment with non-overlapping shifts"() {
        given:
        def shift2 = Mock(Shift)
        shift.getActivity() >> activity
        shift2.getActivity() >> activity
        activity.getApplicationDeadline() >> IN_ONE_DAY
        shift.getEnrollments() >> []
        shift2.getEnrollments() >> []
        shift.getStartTime() >> IN_TWO_DAYS
        shift.getEndTime() >> IN_THREE_DAYS
        shift2.getStartTime() >> IN_THREE_DAYS
        shift2.getEndTime() >> IN_FOUR_DAYS
        def shifts = [shift, shift2]
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        def result = new Enrollment(volunteer, shifts, enrolmentDto)

        then:
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.getShifts().size() == 2
    }

    def "create enrollment and violate shifts from same activity invariant"() {
        given:
        def activity2 = Mock(Activity)
        def shift2 = Mock(Shift)
        shift.getActivity() >> activity
        shift2.getActivity() >> activity2
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity2.getApplicationDeadline() >> IN_ONE_DAY
        activity.getId() >> 1
        activity2.getId() >> 2
        shift.getEnrollments() >> []
        shift2.getEnrollments() >> []
        def shifts = [shift, shift2]

        when:
        new Enrollment(volunteer, shifts, enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_SHIFTS_FROM_DIFFERENT_ACTIVITIES
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}