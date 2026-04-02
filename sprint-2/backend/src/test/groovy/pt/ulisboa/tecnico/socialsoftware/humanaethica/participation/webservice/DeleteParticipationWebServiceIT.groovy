package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.webservice

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeleteParticipationWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shift
    def volunteer

    def participationDto

    def setup() {
        given:
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        def institution = institutionService.getDemoInstitution()
        and:
        volunteer = createVolunteer(USER_1_NAME, "volunteer_custom_delete", USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        volunteer.getAuthUser().setPassword(passwordEncoder.encode(USER_1_PASSWORD))
        userRepository.save(volunteer)
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1, NOW.plusDays(1), NOW.plusDays(2), NOW.plusDays(3))
        and:
        shift = createShift(activity, NOW.plusDays(2).plusHours(1), NOW.plusDays(2).plusHours(3), 5, SHIFT_LOCATION)
        and:
        activity.setStartingDate(TWO_DAYS_AGO)
        activity.setEndingDate(ONE_DAY_AGO)
        activity.setApplicationDeadline(TWO_DAYS_AGO.minusDays(1))
        activityRepository.save(activity)
        and:
        shift.setStartTime(TWO_DAYS_AGO.plusHours(1))
        shift.setEndTime(TWO_DAYS_AGO.plusHours(3))
        and:
        def enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        participationDto = createParticipationDto(null, null, 5, VOLUNTEER_REVIEW)
        participationDto = participationService.createParticipation(shift.id, enrollment.id, participationDto)
    }

    def 'login as a member and delete a participation'() {
        given: 'a member'
        demoMemberLogin()

        when: 'the member deletes the participation'
        def response = webClient.delete()
                .uri("/participations/" + participationDto.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response"
        response.volunteerRating == 5
        and: 'check database'
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'delete a participation that does not exist'() {
        given: 'a member'
        demoMemberLogin()

        when: 'the member deletes the participation'
        webClient.delete()
                .uri("/participations/" + 222)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: 'check database'
        participationRepository.count() == 1

        cleanup:
        deleteAll()
    }

    def 'login as a member of another institution and try to delete a participation'() {
        given: 'a member'
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        def otherMember = createMember(USER_1_NAME,USER_1_USERNAME,USER_1_PASSWORD,USER_1_EMAIL, AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_1_USERNAME, USER_1_PASSWORD)

        when: 'the member deletes the participation'
        webClient.delete()
                .uri("/participations/" + participationDto.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: 'check database'
        participationRepository.count() == 1

        cleanup:
        deleteAll()
    }

    def 'login as volunteer and delete a participation'() {
        given: 'a volunteer'
        normalUserLogin("volunteer_custom_delete", USER_1_PASSWORD)

        when: 'the volunteer tries to delete the participation'
        webClient.delete()
                .uri("/participations/" + participationDto.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: 'check database'
        participationRepository.count() == 1

        cleanup:
        deleteAll()
    }

    def 'login as a admin and delete a participation'() {
        given: 'a admin'
        demoAdminLogin()

        when: 'the admin tries to delete the participation'
        webClient.delete()
                .uri("/participations/" + participationDto.id)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        and: 'check database'
        participationRepository.count() == 1

        cleanup:
        deleteAll()
    }
}