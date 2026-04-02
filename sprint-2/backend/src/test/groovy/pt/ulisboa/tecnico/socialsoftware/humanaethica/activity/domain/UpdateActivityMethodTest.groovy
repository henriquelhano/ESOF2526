package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class UpdateActivityMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme themeOne = Mock()
    Theme themeTwo = Mock()
    Activity otherActivity = Mock()
    def activity
    def activityDtoTwo

    def setup() {
        given:
        otherActivity.getName() >> ACTIVITY_NAME_3
        institution.getActivities() >> [otherActivity]
        and:
        themeOne.getState() >> Theme.State.APPROVED
        def themes = [themeOne]
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        and:
        activityDtoTwo = createActivityDto(ACTIVITY_NAME_2, ACTIVITY_REGION_2, 4, ACTIVITY_DESCRIPTION_2, NOW, IN_ONE_DAY, IN_TWO_DAYS)
    }

    def "update activity"() {
        given:
        themeTwo.getState() >> Theme.State.APPROVED
        def themes = [themeTwo]

        when:
        activity.update(activityDtoTwo, themes)

        then: "checks if activity"
        activity.getInstitution() == institution
        activity.getName() == ACTIVITY_NAME_2
        activity.getRegion() == ACTIVITY_REGION_2
        activity.getParticipantsNumberLimit() == 4
        activity.getDescription() == ACTIVITY_DESCRIPTION_2
        activity.getStartingDate() == IN_ONE_DAY
        activity.getEndingDate() == IN_TWO_DAYS
        activity.getApplicationDeadline() == NOW
        activity.getThemes().size() == 1
        activity.getThemes().get(0) == themeTwo
    }

    @Unroll
    def "create activity and violate invariants for arguments: name=#name | region=#region | participants=#participants | description=#description | deadline=#deadline | start=#start | end=#end | themeStatus=#themeStatus"() {
        given:
        themeTwo.getState() >> themeStatus
        def themes = [themeTwo]
        and: "an activity dto"
        activityDtoTwo = createActivityDto(name, region, participants, description, deadline, start, end)

        when:
        activity.update(activityDtoTwo, themes)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        name            | region            | participants | description            | deadline | start      | end         | themeStatus           || errorMessage
        null            | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_NAME_INVALID
        " "             | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_NAME_INVALID
        ACTIVITY_NAME_2 | null              | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_REGION_NAME_INVALID
        ACTIVITY_NAME_2 | " "               | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_REGION_NAME_INVALID
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | -5           | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_SHOULD_HAVE_ONE_TO_FIVE_PARTICIPANTS
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 0            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_SHOULD_HAVE_ONE_TO_FIVE_PARTICIPANTS
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 6            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_SHOULD_HAVE_ONE_TO_FIVE_PARTICIPANTS
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 20           | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_SHOULD_HAVE_ONE_TO_FIVE_PARTICIPANTS
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | null                   | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_DESCRIPTION_INVALID
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | "  "                   | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_DESCRIPTION_INVALID
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | null     | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | "  "     | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | null       | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | "   "      | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | null        | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | "     "     | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_INVALID_DATE
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | NOW        | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_APPLICATION_DEADLINE_AFTER_START
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_ONE_DAY  | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_START_AFTER_END
        ACTIVITY_NAME_3 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.APPROVED  || ErrorMessage.ACTIVITY_ALREADY_EXISTS
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.DELETED   || ErrorMessage.THEME_NOT_APPROVED
        ACTIVITY_NAME_2 | ACTIVITY_REGION_2 | 2            | ACTIVITY_DESCRIPTION_2 | NOW      | IN_ONE_DAY | IN_TWO_DAYS | Theme.State.SUBMITTED || ErrorMessage.THEME_NOT_APPROVED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}