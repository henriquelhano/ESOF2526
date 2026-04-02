package pt.ulisboa.tecnico.socialsoftware.humanaethica.assessment.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Unroll


@DataJpaTest
class DeleteAssessmentServiceTest extends SpockTest {
    def volunteer
    def activity
    def assessment

    def setup() {
        def institution = institutionService.getDemoInstitution()

        given: "activity info"
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(1), TWO_DAYS_AGO, NOW)
        and: "a volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and: "assessment"
        assessment = createAssessment(institution, volunteer, ASSESSMENT_REVIEW_1)
    }

    def 'delete assessment'() {
        given:
        assessment = assessmentRepository.findAll().get(0)
        when:
        assessmentService.deleteAssessment(assessment.id)
        then: "check that assessment was deleted"
        assessmentRepository.findAll().size() == 0
    }

    @Unroll
    def 'invalid arguments: assessmentId:#assessmentId'() {
        when:
        assessmentService.deleteAssessment(assessmentId);

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        assessmentId    ||  errorMessage
        null            ||  ErrorMessage.ASSESSMENT_NOT_FOUND
        222             ||  ErrorMessage.ASSESSMENT_NOT_FOUND
    }


    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}