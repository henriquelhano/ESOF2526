package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme

@DataJpaTest
class GetActivitiesServiceTest extends SpockTest {
    def setup() {
        given:
        def institution = institutionService.getDemoInstitution()
        and: "an activity"
        def themes = new ArrayList<>()
        themes.add(createTheme(THEME_NAME_1, Theme.State.APPROVED,null))
        createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        and: 'another activity'
        createActivity(institution, ACTIVITY_NAME_2, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
    }

    def 'get two activities'() {
        when:
        def result = activityService.getActivities()

        then:
        result.size() == 2
        result.get(0).name == ACTIVITY_NAME_1
        result.get(1).name == ACTIVITY_NAME_2
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
