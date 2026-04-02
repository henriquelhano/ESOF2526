package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateParticipationMethodTest extends SpockTest {
    Activity activity = Mock()
    Shift shift = Mock()
    Enrollment enrollment = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Participation otherParticipation = Mock()
    def participationDto

    def setup() {
        given:
        participationDto = new ParticipationDto()
        shift.getActivity() >> activity
        enrollment.getVolunteer() >> volunteer
    }

    def "member creates a participation"() {
        given:
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10
        enrollment.getShifts() >> [shift]

        when:
        def result = new Participation(enrollment, shift, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview == MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.activity == activity
        result.volunteer == volunteer
        result.shift == shift
        and: "check that it is added"
    }

    def "create participation and violate participate once invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> volunteer
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_VOLUNTEER_IS_ALREADY_PARTICIPATING
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        participationDto.memberRating = null
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE
    }

    def "create participation and violate rating before end invariant"() {
        given:
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.memberRating = 5
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate number of participants less or equal limit invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 1
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_IS_FULL
    }

    def "create participation and violate shift participants limit invariant"() {
        given:
        Participation otherShiftParticipation = Mock()
        shift.getParticipations() >> [otherShiftParticipation]
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 1
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 4
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipantsLimit() >> 1
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_SHIFT_IS_FULL
    }

    def "create participation and violate enrollment has shift invariant"() {
        given:
        Shift otherShift = Mock()
        enrollment.getShifts() >> [otherShift]
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 1
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 4
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ENROLLMENT_DOES_NOT_HAVE_SHIFT
    }

    @Unroll
    def "create participation and violate member rating in range 1..5: rating=#rating"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        participationDto.memberRating = rating
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5, 0, 6, 20]
    }

    @Unroll
    def "create participation and violate volunteer rating in range 1..5: rating=#rating"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        participationDto.volunteerRating = rating
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5, 0, 6, 20]
    }

    @Unroll
    def "create participation and violate volunteer review length: review=#review"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        participationDto.volunteerReview = review
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789", "a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @Unroll
    def "create participation and violate member review length: review=#review"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        participationDto.memberReview = review
        shift.getParticipations() >> []
        shift.getParticipantsLimit() >> 10

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789", "a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}