package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift

@DataJpaTest
class DeleteParticipationMethodTest extends SpockTest {
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
     }

    def "delete participation"() {
        when: "a participation is deleted"
        participation.delete()

        then: "checks results"
        1 * shift.removeParticipation(participation)
        1 * enrollment.setParticipation(null)
    }

    def "delete one of multiple participations in shift"() {
        given:
        def otherEnrollment = Mock(Enrollment)
        otherEnrollment.getShifts() >> [shift]
        otherEnrollment.getActivity() >> activity
        and:
        def otherParticipation = new Participation(otherEnrollment, shift, participationDto)

        when: "one participation is deleted"
        otherParticipation.delete()

        then: "the other participation remains"
        1 * shift.removeParticipation(otherParticipation)
        1 * otherEnrollment.setParticipation(null)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}