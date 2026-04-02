package pt.ulisboa.tecnico.socialsoftware.humanaethica.assessment.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.assessment.dto.AssessmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetAssessmentsByInstitutionWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def institution

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        institution = institutionService.getDemoInstitution()
        and:
        def activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, TWO_DAYS_AGO.minusDays(2), TWO_DAYS_AGO.minusDays(1), TWO_DAYS_AGO)
        and:
        institution.addActivity(activity)
        and:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createAssessment(institution, volunteerOne, ASSESSMENT_REVIEW_1)
        createAssessment(institution, volunteerTwo, ASSESSMENT_REVIEW_2)
    }

    def 'non authenticated user gets two assessment'() {
        when:
        def response = webClient.get()
                .uri('/institutions/' + institution.id + '/assessments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(AssessmentDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).review == ASSESSMENT_REVIEW_1
        response.get(0).getReviewDate() != null
        response.get(1).review == ASSESSMENT_REVIEW_2
        response.get(0).getReviewDate() != null
    }

    def 'institution does not exist'() {
        when:
        webClient.get()
                .uri('/institutions/' + '222' + '/assessments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(AssessmentDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
    }
}
