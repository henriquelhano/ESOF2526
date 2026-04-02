package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import spock.lang.Unroll

@DataJpaTest
class ReportActivityMethodTest extends SpockTest {
    Institution institution = Mock()
    Activity activity

    def setup() {
    }

    @Unroll
    def "report activity with: state:#state"() {
        given: "activity"
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(state)

        when:
        activity.report()

        then: "it is reported"
        activity.getState() == resultState

        where:
        state                    || resultState
        Activity.State.APPROVED  || Activity.State.REPORTED
        Activity.State.SUSPENDED || Activity.State.REPORTED
    }

    @Unroll
    def "violate report precondition"() {
        given:
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(Activity.State.REPORTED)

        when:
        activity.report()

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_ALREADY_REPORTED
        activity.getState() == Activity.State.REPORTED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}