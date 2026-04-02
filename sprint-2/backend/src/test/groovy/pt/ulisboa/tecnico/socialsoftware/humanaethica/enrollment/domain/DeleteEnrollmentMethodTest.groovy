package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift

@DataJpaTest
class DeleteEnrollmentMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme theme = Mock()
    def enrollmentOne
    def volunteer
    def activity
    def activity2
    def enrollmentTwo
    def shift = Mock(Shift)

    def setup() {
        theme.getState() >> Theme.State.APPROVED
        institution.getActivities() >> []
        given:"activity"
        def themes = [theme]
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        and: "volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        shift.getActivity() >> activity
        and: "enrollment"
        enrollmentOne = createEnrollment(volunteer, List.of(shift), ENROLLMENT_MOTIVATION_1)
    }

    def "delete enrollment"() {
        when: "enrollment is deleted"
        enrollmentOne.delete()

        then: "checks if the enrollment was deleted in the activity and volunteer"
        volunteer.getEnrollments().size() == 0

        1 * shift.removeEnrollment(enrollmentOne)
    }
   
    def "try to delete enrollment after deadline"() {
        given:
        def themes = [theme]
        activity2 = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)

        def shift2 = Mock(Shift)
        shift2.getActivity() >> activity2
        
        and: "enrollment"
        def enrollmentDtoTwo = new EnrollmentDto()
        enrollmentDtoTwo.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentTwo = new Enrollment(volunteer, List.of(shift2), enrollmentDtoTwo)
        activity2.setApplicationDeadline(ONE_DAY_AGO)

        when:
        enrollmentTwo.delete()

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}