package pt.ulisboa.tecnico.socialsoftware.humanaethica.assessment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.assessment.dto.AssessmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import spock.lang.Unroll

@DataJpaTest
class UpdateAssessmentMethodTest extends SpockTest {
    Institution institution = Mock()
    Activity activity = Mock()
    def assessmentDtoEdit

    def volunteer
    def assessment

    def setup() {
        given:
        activity.getApplicationDeadline() >> IN_TWO_DAYS
        activity.getEndingDate() >> DateHandler.now()
        institution.getActivities() >> [activity]
        institution.getAssessments() >> []
        and: "volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and: "an assessment"
        assessment = createAssessment(institution, volunteer, ASSESSMENT_REVIEW_1)
        and: "an assessment review edit"
        assessmentDtoEdit = new AssessmentDto();
        assessmentDtoEdit.review = ASSESSMENT_REVIEW_2
    }

    def "update assessment"() {
        given:
        assessment.review = ASSESSMENT_REVIEW_1

        when:
        assessment.update(assessmentDtoEdit);

        then: "checks results"
        assessment.getReview() == ASSESSMENT_REVIEW_2
        assessment.getReviewDate().isBefore(DateHandler.now())
    }

    @Unroll
    def 'invalid review message=#message'() {
        given:
        def assessmentDtoMessage = new AssessmentDto()
        assessmentDtoMessage.review = message

        when:
        assessment.update(assessmentDtoMessage)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        message || errorMessage
        "short" || ErrorMessage.ASSESSMENT_REVIEW_TOO_SHORT
        " a d w"|| ErrorMessage.ASSESSMENT_REVIEW_TOO_SHORT
        null || ErrorMessage.ASSESSMENT_REQUIRES_REVIEW
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}