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
class SuspendActivityMethodTest extends SpockTest {
    Institution institution = Mock()
    Activity activity
    def member

    def setup() {
        member = authUserService.loginDemoMemberAuth().getUser()
    }

    @Unroll
    def "suspend activity with: state:#state"() {
        given: "activity"
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(state)

        when:
        activity.suspend(member.id, ACTIVITY_SUSPENSION_JUSTIFICATION_VALID)

        then:
        activity.getState() == resultState

        where:
        state                   || resultState
        Activity.State.APPROVED || Activity.State.SUSPENDED
        Activity.State.REPORTED || Activity.State.SUSPENDED
    }

    @Unroll
    def "violate suspend precondition"() {
        given:
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(Activity.State.SUSPENDED)

        when:
        activity.suspend(member.id, ACTIVITY_SUSPENSION_JUSTIFICATION_VALID)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_ALREADY_SUSPENDED
        activity.getState() == Activity.State.SUSPENDED
    }

    @Unroll
    def "suspend activity with an invalid justification:#justification"() {
        given: "activity"
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)

        when:
        activity.suspend(member.id, justification)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        justification           || errorMessage
        null                    || ErrorMessage.ACTIVITY_SUSPENSION_JUSTIFICATION_INVALID
        "too short"             || ErrorMessage.ACTIVITY_SUSPENSION_JUSTIFICATION_INVALID
        generateLongString()    || ErrorMessage.ACTIVITY_SUSPENSION_JUSTIFICATION_INVALID
    }

    def "suspend activity after the ending date"() {
        given: "activity"
        institution.getActivities() >> []
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setEndingDate(ONE_DAY_AGO)

        when:
        activity.suspend(member.id, ACTIVITY_SUSPENSION_JUSTIFICATION_VALID)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_SUSPENSION_AFTER_END
    }

    def generateLongString(){
        return 'a'* 257
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}