package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateParticipationWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shift
    def enrollment
    def participationDtoMember
    def participationDtoVolunteer

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, NOW.plusDays(1), NOW.plusDays(2), NOW.plusDays(3))
        and:
        shift = createShift(activity, NOW.plusDays(2).plusHours(1), NOW.plusDays(2).plusHours(3), 5, SHIFT_LOCATION)
        and:
        def volunteer = authUserService.loginDemoVolunteerAuth().getUser()
        and:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shift.id]
        enrollment = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)
        and:
        activity.setStartingDate(NOW.minusDays(4))
        activity.setEndingDate(NOW.minusDays(3))
        activity.setApplicationDeadline(NOW.minusDays(5))
        activityRepository.save(activity)
        and:
        shift.setStartTime(NOW.minusDays(4).plusHours(1))
        shift.setEndTime(NOW.minusDays(4).plusHours(3))
        and:
        participationDtoMember = createParticipationDto(5, MEMBER_REVIEW, null, null)
        and:
        participationDtoVolunteer = createParticipationDto(null, null, 5, VOLUNTEER_REVIEW)
    }

    def 'member create participation'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/participations/' + shift.id + '/enrollment/' + enrollment.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        response.memberRating == 5
        response.memberReview == MEMBER_REVIEW
        and:
        participationRepository.getParticipationsByActivityId(activity.id).size() == 1
        def storedParticipant = participationRepository.getParticipationsByActivityId(activity.id).get(0)
        storedParticipant.memberRating == 5
        storedParticipant.memberReview == MEMBER_REVIEW

        cleanup:
        deleteAll()
    }

    def 'member create participation with error'() {
        given:
        demoMemberLogin()
        and:
        participationDtoMember.memberRating = 10

        when:
        def response = webClient.post()
                .uri('/participations/' + shift.id + '/enrollment/' + enrollment.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'admin cannot create participation'() {
        given:
        demoAdminLogin()

        when:
        def response = webClient.post()
                .uri('/participations/' + shift.id + '/enrollment/' + enrollment.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'volunteer cannot create participation'() {
        given:
        demoVolunteerLogin()

        when:
        def response = webClient.post()
                .uri('/participations/' + shift.id + '/enrollment/' + enrollment.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
