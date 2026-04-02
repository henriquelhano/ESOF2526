package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import spock.lang.Unroll

@DataJpaTest
class ValidateActivityMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme theme = Mock()
    Activity activity

    def setup() {
    }

    @Unroll
    def "validate activity with: state:#state"() {
        given: "activity"
        institution.getActivities() >> []
        and:
        theme.getState() >> Theme.State.APPROVED
        and:
        def themes = [theme]
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(state)

        when:
        activity.validate()

        then: "it is suspended"
        activity.getState() == resultState

        where:
        state                    || resultState
        Activity.State.SUSPENDED || Activity.State.APPROVED
        Activity.State.REPORTED  || Activity.State.APPROVED
    }

    @Unroll
    def "violate validate precondition: activityState=#activityState | themeState=#themeState || message=#message"() {
        given:
        institution.getActivities() >> []
        and:
        theme.getState() >> themeState
        and:
        def themes = []
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        activity.setState(activityState)
        activity.addTheme(theme)

        when:
        activity.validate()

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == message
        activity.getState() == activityState

        where:
        activityState            | themeState            || message
        Activity.State.APPROVED  | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_ALREADY_APPROVED
        Activity.State.SUSPENDED | Theme.State.SUBMITTED || ErrorMessage.THEME_NOT_APPROVED
        Activity.State.SUSPENDED | Theme.State.DELETED   || ErrorMessage.THEME_NOT_APPROVED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}