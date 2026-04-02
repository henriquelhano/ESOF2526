package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime


@DataJpaTest
class UpdateMemberRatingParticipationMethodTest extends SpockTest {
    Activity activity = Mock()
    Shift shift = Mock()
    Enrollment enrollment = Mock()

    def participation
    def participationDto
    def participationDtoUpdated

    def setup() {
        given:
        activity.getShifts() >> [shift]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        and:
        shift.getParticipations() >> []
        shift.getEnrollments() >> [enrollment]
        shift.getParticipantsLimit() >> 2
        shift.getActivity() >> activity
        and:
        enrollment.getActivity() >> activity
        enrollment.getShifts() >> [shift]
        and:
        participationDto = createParticipationDto(4, VOLUNTEER_REVIEW, null, null)
        participation = new Participation(enrollment, shift, participationDto)
        and:
        participationDtoUpdated = createParticipationDto(null, null, null, null)
    }

    def "member updates a participation"() {
        given:
        participationDtoUpdated.memberRating = 5
        participationDtoUpdated.memberReview = MEMBER_REVIEW

        when:
        participation.memberRating(participationDtoUpdated)

        then: "checks results"
        participation.memberRating == 5
        participation.memberReview == MEMBER_REVIEW
        participation.acceptanceDate.isBefore(LocalDateTime.now())
        participation.activity == activity
    }

    @Unroll
    def "update participation and violate rating in range 1..5: rating=#rating"(){
        given:
        participationDtoUpdated.memberRating = rating
        participationDtoUpdated.memberReview = MEMBER_REVIEW

        when:
        participation.memberRating(participationDtoUpdated)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "update participation and violate review length: review=#review"(){
        given:
        participationDtoUpdated.memberRating = 5
        participationDtoUpdated.memberReview = review

        when:
        participation.memberRating(participationDtoUpdated)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}