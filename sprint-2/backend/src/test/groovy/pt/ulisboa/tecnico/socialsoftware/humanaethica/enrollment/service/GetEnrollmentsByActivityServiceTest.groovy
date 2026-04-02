package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetEnrollmentsByActivityServiceTest extends SpockTest {
    def activity
    def otherActivity
    def shift
    def otherShift

    def setup() {
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        shift = createShift(activity, IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(3), 5, SHIFT_LOCATION)
        and:
        otherActivity = createActivity(institution, ACTIVITY_NAME_2, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        otherShift = createShift(otherActivity, IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(3), 5, SHIFT_LOCATION)
    }

    def "get two enrollments of the same activity"() {
        given:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createEnrollment(volunteerOne, List.of(shift), ENROLLMENT_MOTIVATION_1)
        createEnrollment(volunteerTwo, List.of(shift), ENROLLMENT_MOTIVATION_2)

        when:
        def enrollments = enrollmentService.getEnrollmentsByActivity(activity.id)

        then:
        enrollments.size() == 2
        enrollments.get(0).motivation == ENROLLMENT_MOTIVATION_1
        enrollments.get(1).motivation == ENROLLMENT_MOTIVATION_2
    }

    def "get one enrollment of an activity"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createEnrollment(volunteer, List.of(shift), ENROLLMENT_MOTIVATION_1)
        createEnrollment(volunteer, List.of(otherShift), ENROLLMENT_MOTIVATION_2)

        when:
        def enrollments = enrollmentService.getEnrollmentsByActivity(activity.id)

        then:
        enrollments.size() == 1
        enrollments.get(0).motivation == ENROLLMENT_MOTIVATION_1
    }

    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        enrollmentService.getEnrollmentsByActivity(activityId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityId || errorMessage
        null       || ErrorMessage.ACTIVITY_NOT_FOUND
        222        || ErrorMessage.ACTIVITY_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
