package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetParticipationsByActivityServiceTest extends SpockTest {
    def activity
    def shift
    def otherShift
    def otherActivity
    def participationDto1
    def participationDto2

    def setup() {
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(2), TWO_DAYS_AGO.minusDays(1), NOW)
        and:
        shift = createShift(activity, TWO_DAYS_AGO, ONE_DAY_AGO, 3, SHIFT_LOCATION)
        and:
        otherActivity = createActivity(institution, ACTIVITY_NAME_2, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(2), TWO_DAYS_AGO.minusDays(1), NOW)
        and:
        otherShift = createShift(otherActivity, TWO_DAYS_AGO, ONE_DAY_AGO, 3, SHIFT_LOCATION)
        and:
        participationDto1 = createParticipationDto(1, MEMBER_REVIEW, null, null)
        participationDto2 = createParticipationDto(2, MEMBER_REVIEW, null, null)
    }

    def "get two participations of the same shift"() {
        given:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollmentOne = createEnrollmentBypassInvariantsValidation(volunteerOne, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollmentTwo = createEnrollmentBypassInvariantsValidation(volunteerTwo, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        createParticipation(enrollmentOne, shift, participationDto1)
        createParticipation(enrollmentTwo, shift, participationDto2)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 2
        participations.get(0).memberRating == 1
        participations.get(1).memberRating == 2
    }

    def "get one participation of an activity"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollmentForShift = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        def enrollmentForOtherShift = createEnrollmentBypassInvariantsValidation(volunteer, [otherShift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        createParticipation(enrollmentForShift, shift, participationDto1)
        createParticipation(enrollmentForOtherShift, otherShift, participationDto1)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 1
        participations.get(0).memberRating == 1
    }

    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        participationService.getParticipationsByActivity(activityId)

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
