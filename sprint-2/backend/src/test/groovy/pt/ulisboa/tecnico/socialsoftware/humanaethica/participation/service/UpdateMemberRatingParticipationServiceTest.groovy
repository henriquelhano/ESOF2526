package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class UpdateMemberRatingParticipationServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity
    def shift
    def participation
    def volunteer
    def member

    def setup() {
        given:
        def institution = institutionService.getDemoInstitution()
        and:
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()
        and:
        member = authUserService.loginDemoMemberAuth().getUser()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(2), TWO_DAYS_AGO.minusDays(1), NOW)
        and:
        shift = createShift(activity, TWO_DAYS_AGO, ONE_DAY_AGO, 3, SHIFT_LOCATION)
        and:
        volunteer = userRepository.findById(volunteer.getId()).get()
        def enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        def participationDto = createParticipationDto(null, null, 5, VOLUNTEER_REVIEW)
        participation = participationService.createParticipation(shift.getId(), enrollment.getId(), participationDto)
    }

    def 'member updates a participation' () {
        given:
        def updatedParticipationDto = createParticipationDto(5, MEMBER_REVIEW, null, null)

        when:
        def result = participationService.memberRating(participation.id, updatedParticipationDto)

        then: "the returned data is correct"
        result.memberReview == MEMBER_REVIEW
        result.memberRating == 5
        and: "the participation is stored"
        participationRepository.findAll().size() == 1
        and: "contains the correct data"
        def storedParticipation = participationRepository.findAll().get(0)
        storedParticipation.memberReview == MEMBER_REVIEW
        storedParticipation.memberRating == 5
        storedParticipation.acceptanceDate.isBefore(LocalDateTime.now())
        storedParticipation.activity.id == activity.id
        storedParticipation.volunteer.id == volunteer.id
    }

    @Unroll
    def 'invalid arguments: rating=#rating | review=#review |participationId=#participationId'() {
        given:
        def updatedParticipationDto = createParticipationDto(rating, review, null, null)

        when:
        participationService.memberRating(getParticipationId(participationId), updatedParticipationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and: "the participation is in the database"
        participationRepository.findAll().size() == 1

        where:
        rating          | participationId   | review          || errorMessage
        -2              | EXIST             | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
        3               | null              | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_NOT_FOUND
        3               | NO_EXIST          | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_NOT_FOUND
        3               | EXIST             | ""              || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        3               | EXIST             | "a".repeat(110) || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

    }

    def getParticipationId(participationId){
        if (participationId == EXIST)
            return participation.id
        else if (participationId == NO_EXIST)
            return 222
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
