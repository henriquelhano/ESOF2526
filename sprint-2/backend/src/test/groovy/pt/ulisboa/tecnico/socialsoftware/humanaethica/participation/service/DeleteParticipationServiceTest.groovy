package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Unroll

@DataJpaTest
class DeleteParticipationServiceTest extends SpockTest {
    public static final String FIRST_PARTICIPATION = 'first'
    public static final String SECOND_PARTICIPATION = 'second'
    public static final Integer FIRST_PARTICIPATION_RATING = 5
    public static final Integer SECOND_PARTICIPATION_RATING = 2
    def activity
    def shift
    def participation
    def volunteer
    def secondParticipation

    def setup() {
        def institution = institutionService.getDemoInstitution()
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(2), TWO_DAYS_AGO.minusDays(1), NOW)
        and:
        shift = createShift(activity, TWO_DAYS_AGO, ONE_DAY_AGO, 3, SHIFT_LOCATION)
        and:
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        def enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        def participationDto = createParticipationDto(null, null, 5, VOLUNTEER_REVIEW)
        participation = createParticipation(enrollment, shift, participationDto)
    }

    def 'delete participation'() {
        when:
        participationService.deleteParticipation(participation.getId())

        then: "the participation was deleted"
        participationRepository.findAll().size() == 0
    }

    @Unroll
    def 'two participations exist and one is deleted: participationId=#participationId | deletedRating=#deletedRating | remainingRating=#remainingRating '() {
        given:
        def volunteer2 = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        def enrollment2 = createEnrollmentBypassInvariantsValidation(volunteer2, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        def participationDto2 = createParticipationDto(null, null, 2, VOLUNTEER_REVIEW)
        secondParticipation = createParticipation(enrollment2, shift, participationDto2)

        when:
        def result = participationService.deleteParticipation(getFirstOrSecondParticipation(participationId))

        then: "the participation was deleted"
        participationRepository.findAll().size() == 1
        result.volunteerRating == deletedRating
        def remainingParticipation = participationRepository.findAll().get(0)
        remainingParticipation.volunteerRating == remainingRating

        where: "deletes the selected participation and checks the rating of the deleted participation and the remaining one"
        participationId                 || deletedRating                    || remainingRating
        FIRST_PARTICIPATION             || FIRST_PARTICIPATION_RATING       || SECOND_PARTICIPATION_RATING
        SECOND_PARTICIPATION            || SECOND_PARTICIPATION_RATING      || FIRST_PARTICIPATION_RATING
    }

    def getFirstOrSecondParticipation(participationId){
        if (participationId == FIRST_PARTICIPATION)
            return participation.getId()
        else if (participationId == SECOND_PARTICIPATION)
            return secondParticipation.id
        return null
    }

    @Unroll
    def 'invalid arguments: participationId:#participationId'() {
        when:
        participationService.deleteParticipation(participationId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and: "the participation is in the database"
        participationRepository.findAll().size() == 1

        where:
        participationId || errorMessage
        null            || ErrorMessage.PARTICIPATION_NOT_FOUND
        222             || ErrorMessage.PARTICIPATION_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}