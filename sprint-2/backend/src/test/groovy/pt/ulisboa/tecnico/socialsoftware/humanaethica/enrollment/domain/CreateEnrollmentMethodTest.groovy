package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ENROLLMENT_AT_LEAST_ONE_SHIFT
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ENROLLMENT_SHIFTS_HAVE_OVERLAPPING_TIME
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ENROLLMENT_SHIFTS_MUST_BELONG_TO_SAME_ACTIVITY
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateEnrollmentMethodTest extends SpockTest {
    Activity activity = Mock()
    Volunteer volunteer = Mock()
    Enrollment otherEnrollment = Mock()
    Shift shift = Mock()
    def enrolmentDto

    def setup() {
        given: "enrolment info"
        enrolmentDto = new EnrollmentDto()
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1
        and:
        shift.getActivity() >> activity
        volunteer.getEnrollments() >> []
        activity.getId() >> 1
    }

    def "create enrollment"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        
        when:
        def result = new Enrollment(volunteer, List.of(shift), enrolmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.getActivity() == activity
        result.volunteer == volunteer
        result.getShifts().size() == 1
        result.getShifts().contains(shift)
        and: "check that it is added"
        1 * volunteer.addEnrollment(_)
        1 * shift.addEnrollment(_)
    }

    @Unroll
    def "create enrollment and violate motivation is required invariant: motivation=#motivation"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        and:
        enrolmentDto.motivation = motivation

        when:
        new Enrollment(volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        motivation || errorMessage
        null       || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "< 10"     || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }

    def "create enrollment and violate enrollment before deadline invariant"() {
        given:
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    def "create enrollment with shift from another activity"() {
        given:
        Activity otherActivity = Mock()
        activity.getApplicationDeadline() >> IN_ONE_DAY
        and:
        Shift otherShift = Mock()
        otherShift.getActivity() >> otherActivity
        and: "an enrollment dto"
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.setMotivation(ENROLLMENT_MOTIVATION_1)

        when:
        new Enrollment(volunteer, List.of(shift, otherShift), enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ENROLLMENT_SHIFTS_MUST_BELONG_TO_SAME_ACTIVITY
    }

    def "create enrollment and violate enroll once invariant"() {
        given:
        volunteer = Mock()
        activity.getApplicationDeadline() >> IN_ONE_DAY
        volunteer.getEnrollments() >> [otherEnrollment]
        otherEnrollment.getActivity() >> activity
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED
    }

    @Unroll
    def "create enrollment with overlapping shifts configurations (valid): #description"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        and:
        shift.getStartTime() >> NOW.plusHours(10)
        shift.getEndTime() >> NOW.plusHours(12)
        and:
        Shift otherShift = Mock()
        otherShift.getActivity() >> activity
        otherShift.getStartTime() >> NOW.plusHours(s2Start)
        otherShift.getEndTime() >> NOW.plusHours(s2End)
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, List.of(shift, otherShift), enrolmentDto)

        then:
        noExceptionThrown()

        where:
        description           | s2Start | s2End
        "Before"              | 8       | 9
        "Touch end-start"     | 8       | 10
        "Touch start-end"     | 12      | 14
        "After"               | 13      | 14
    }

    @Unroll
    def "create enrollment with overlapping shifts configurations (invalid): #description"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        and:
        shift.getStartTime() >> NOW.plusHours(10)
        shift.getEndTime() >> NOW.plusHours(12)
        and:
        Shift otherShift = Mock()
        otherShift.getActivity() >> activity
        otherShift.getStartTime() >> NOW.plusHours(s2Start)
        otherShift.getEndTime() >> NOW.plusHours(s2End)
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, List.of(shift, otherShift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ENROLLMENT_SHIFTS_HAVE_OVERLAPPING_TIME

        where:
        s2Start | s2End || description
        9       | 11    || "Overlap start"
        10      | 11    || "Inside"
        10      | 12    || "Exact match"
        9       | 13    || "Encloses"
        11      | 13    || "Overlap end"
    }

    def "create enrollment and violate at least one shift invariant"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY

        when:
        new Enrollment(volunteer, new ArrayList<>(), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ENROLLMENT_AT_LEAST_ONE_SHIFT
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}