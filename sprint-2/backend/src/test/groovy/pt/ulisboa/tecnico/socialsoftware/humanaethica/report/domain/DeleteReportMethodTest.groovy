package pt.ulisboa.tecnico.socialsoftware.humanaethica.report.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.report.dto.ReportDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class DeleteReportMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme theme = Mock()
    def reportOne
    def volunteer
    def activity
    def activity2
    def reportTwo

    def setup() {
        theme.getState() >> Theme.State.APPROVED
        institution.getActivities() >> []

        given:"activity"
        def themes = [theme]
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)

        and: "volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        and: "report"
        def reportDto = new ReportDto()
        reportDto.justification = REPORT_JUSTIFICATION_1
        reportOne = new Report(activity, volunteer, reportDto)
    }

    def "delete report"() {

        when: "report is deleted"
        reportOne.delete()

        then: "checks if the report was deleted in the activtiy and volunteer"
        volunteer.getReports().size() == 0
        activity.getReports().size() == 0

    }
   
    def "try to delete report after activity deadline"() {
        given:
        activity2 = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, [theme])
        
        and: "report"
        def reportDtoTwo = new ReportDto()
        reportDtoTwo.justification = REPORT_JUSTIFICATION_1
        reportTwo = new Report(activity2, volunteer, reportDtoTwo)
        activity2.setEndingDate(ONE_DAY_AGO)

        when:
        reportTwo.delete()

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.REPORT_AFTER_ACTIVTY_CLOSED
    }
   
    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}