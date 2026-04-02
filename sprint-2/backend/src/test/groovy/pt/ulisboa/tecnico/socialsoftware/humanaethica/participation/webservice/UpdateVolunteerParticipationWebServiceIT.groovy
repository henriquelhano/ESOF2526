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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateVolunteerParticipationWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shift
    def volunteer
    def participationId
    def member

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        member = authUserService.loginDemoMemberAuth().getUser()
        volunteer = createVolunteer(USER_1_NAME, "volunteer_custom_update", USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        volunteer.getAuthUser().setPassword(passwordEncoder.encode(USER_1_PASSWORD))
        userRepository.save(volunteer)
        and:
        def institution = institutionService.getDemoInstitution()
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
        Enrollment enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))
        and:
        def participationDto = createParticipationDto(5, MEMBER_REVIEW, 5, VOLUNTEER_REVIEW)
        participationDto = participationService.createParticipation(shift.id, enrollment.id, participationDto)
        participationId = participationDto.getId()
    }

    def 'login as a volunteer and update a participation'() {
        given: 'a volunteer'
        normalUserLogin("volunteer_custom_update", USER_1_PASSWORD)
        def participationDtoUpdate = createParticipationDto(null, null, 1, "NEW REVIEW")

        when: 'the member edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/volunteer")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response"
        response.volunteerRating == 1
        response.volunteerReview == "NEW REVIEW"
        and: 'check database'
        participationRepository.count() == 1
        def participation = participationRepository.findAll().get(0)
        participation.getVolunteerRating() == 1
        participation.getVolunteerReview() == "NEW REVIEW"

        cleanup:
        deleteAll()
    }

    def 'update with a rating of 10 abort and no changes'() {
        given: 'a volunteer'
        normalUserLogin("volunteer_custom_update", USER_1_PASSWORD)
        def participationDtoUpdate = createParticipationDto(null, null, 10, VOLUNTEER_REVIEW)

        when: 'the volunteer edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/volunteer")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        and: 'check database'
        participationRepository.count() == 1
        def participation = participationRepository.findAll().get(0)
        participation.getVolunteerRating() == 5

        cleanup:
        deleteAll()
    }

    def 'log in as another volunteer and try to write a review for a participation by a different volunteer'() {
        given: 'another volunteer'
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        volunteer.authUser.setPassword(passwordEncoder.encode(USER_1_PASSWORD))
        userRepository.save(volunteer)
        normalUserLogin(USER_1_USERNAME, USER_1_PASSWORD)
        def participationDtoUpdate = createParticipationDto(null, null, 1, "ANOTHER_REVIEW")

        when: 'the member tries to edit the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/volunteer")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getVolunteerRating() == 5
        participation.getVolunteerReview() == VOLUNTEER_REVIEW

        cleanup:
        deleteAll()
    }

    def 'login as a admin and try to edit a participation'() {
        given: 'a demo'
        demoAdminLogin()
        def participationDtoUpdate = createParticipationDto(null, null, 1, "ANOTHER_REVIEW")

        when: 'the admin edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/volunteer")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getVolunteerRating() == 5
        participation.getVolunteerReview() == VOLUNTEER_REVIEW

        cleanup:
        deleteAll()
    }

    def 'login as a member and try to update a volunteer rating'() {
        given: 'a demo'
        demoMemberLogin()
        def participationDtoUpdate = createParticipationDto(null, null, 1, "ANOTHER_REVIEW")

        when: 'the member edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/volunteer")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getVolunteerRating() ==  5
        participation.getVolunteerReview() == VOLUNTEER_REVIEW

        cleanup:
        deleteAll()
    }


}

