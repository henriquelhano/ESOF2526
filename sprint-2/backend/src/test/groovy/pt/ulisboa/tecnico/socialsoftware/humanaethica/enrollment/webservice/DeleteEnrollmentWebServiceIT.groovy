package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeleteEnrollmentWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def volunteer
    def enrollmentId
    def shift

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        def institution = institutionService.getDemoInstitution()
        and:
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        shift = createShift(activity, IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(3), 5, SHIFT_LOCATION)
        and:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.volunteerId = volunteer.id
        enrollmentDto.shiftIds = [shift.id]
        enrollmentService.createEnrollment(volunteer.id, enrollmentDto)
        and:
        def storedEnrollment = enrollmentRepository.findAll().get(0)
        enrollmentId = storedEnrollment.id
    }

    def 'login as a volunteer and remove an enrollment'() {
        given: 'a volunteer'
        demoVolunteerLogin()

        when: 'then volunteer deletes the enrollment'
        def response = webClient.delete()
                .uri("/enrollments/" + enrollmentId)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()

        then: "check response"
        response.motivation == ENROLLMENT_MOTIVATION_1
        and: "check database"
        enrollmentRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'login as a member and try to delete an enrollment'() {
        given: 'a member'
        demoMemberLogin()

        when: 'the member deletes the enrollment'
        webClient.delete()
                .uri("/enrollments/" + enrollmentId)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()
        
        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: "check database"
        enrollmentRepository.count() == 1

        cleanup:
        deleteAll()
    }

    def 'login as a admin and try to delete an enrollment'() {
        given: 'a admin'
        demoAdminLogin()

        when: 'the admin deletes the enrollment'
        webClient.delete()
                .uri("/enrollments/" + enrollmentId)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()
        
        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: "check database"
        enrollmentRepository.count() == 1

        cleanup:
        deleteAll()
    }
}