package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateParticipationMethodTest extends SpockTest {
    Activity activity = Mock()
    Enrollment enrollment = Mock()
    Shift shift = Mock()
    def participationDto

    def setup() {
    }

    def "member creates a participation"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(5, MEMBER_REVIEW, null, null)

        when:
        def result = new Participation(enrollment, shift, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview ==  MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.shift == shift
        and: "check that it is added"
        1 * shift.addParticipation(_)
        1 * enrollment.setParticipation(_)
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(null, null, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE
    }

    def "create participation and violate rating before end invariant"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(5, MEMBER_REVIEW, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate shift participants less or equal limit invariant"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> [Mock(Participation), Mock(Participation), Mock(Participation)]
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(null, null, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_CURRENT_PARTICIPANTS_EXCEEDS_LIMIT
    }

    @Unroll
    def "create participant with success (limit boundary): existing=#existing, limit=#limit"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> limit
        def participationList = new ArrayList()
        for (int i = 0; i < existing; i++) {
            def p = Mock(Participation)
            participationList.add(p)
        }
        shift.getParticipations() >> participationList
        and:
        participationDto = createParticipationDto(null, null, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        noExceptionThrown()

        where:
        existing | limit
        0        | 10
        5        | 10
        9        | 10
    }

    @Unroll
    def "create participation and violate member rating in range 1..5: rating=#rating"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(rating, null, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer rating in range 1..5: rating=#rating"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(null, null, rating, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer review length: review=#review"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(null, null, null, review)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @Unroll
    def "create participation and violate member review length: review=#review"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        participationDto = createParticipationDto(null, review, null, null)

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    def "create participation and violate shift belongs to enrollment invariant"() {
        given:
        enrollment.getShifts() >> [shift]
        enrollment.getActivity() >> activity
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 2
        and:
        def localEnrollment = Mock(Enrollment)
        localEnrollment.getShifts() >> []
        and:
        participationDto = createParticipationDto(null, null, null, null)

        when:
        new Participation(localEnrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_SHIFT_NOT_IN_ENROLLMENT
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}