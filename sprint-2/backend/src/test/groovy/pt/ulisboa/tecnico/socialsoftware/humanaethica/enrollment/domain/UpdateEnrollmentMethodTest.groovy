package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class UpdateEnrollmentMethodTest extends SpockTest {
    Institution institution = Mock()
    Activity activity = Mock()

    def enrollment
    def enrollmentDtoOne
    def enrollmentDtoEdit
    def activity2
    def enrollmentTwo
    def volunteer

    def setup() {
        given:
        activity.getApplicationDeadline() >> IN_TWO_DAYS
        activity.getId() >> 1
        and:
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        def shift = Mock(Shift)
        shift.getActivity() >> activity
        and:
        enrollment = createEnrollment(volunteer, List.of(shift), ENROLLMENT_MOTIVATION_1)
        and:
        enrollmentDtoEdit = new EnrollmentDto()
    }

    def "update enrollment"() {
        given:
        enrollmentDtoEdit.motivation = ENROLLMENT_MOTIVATION_2
        when:
        enrollment.update(enrollmentDtoEdit)
        then: "checks results"
        enrollment.getMotivation() == ENROLLMENT_MOTIVATION_2
        enrollment.enrollmentDateTime.isBefore(LocalDateTime.now())
        enrollment.getActivity() == activity
        enrollment.volunteer == volunteer
    }

    @Unroll
    def "edit enrollment and violate motivation is required invariant: motivation=#motivation"() {
      given:
      enrollmentDtoEdit.motivation = motivation

      when:
      enrollment.update(enrollmentDtoEdit)

      then:
      def error = thrown(HEException)
      error.getErrorMessage() == errorMessage

      where:
        motivation || errorMessage
        null       || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "< 10"     || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }


    def "try to update enrollment after deadline"() {
        given:
        def deadline = IN_ONE_DAY
        activity2 = Mock(Activity)
        activity2.getId() >> 2
        activity2.getApplicationDeadline() >> { deadline }
        and:
        def shift2 = Mock(Shift)
        shift2.getActivity() >> activity2
        and:
        enrollmentTwo = createEnrollment(volunteer, List.of(shift2), ENROLLMENT_MOTIVATION_1)
        and:
        deadline = ONE_DAY_AGO
        enrollmentDtoEdit.motivation = ENROLLMENT_MOTIVATION_2

        when:
        enrollmentTwo.update(enrollmentDtoEdit)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}