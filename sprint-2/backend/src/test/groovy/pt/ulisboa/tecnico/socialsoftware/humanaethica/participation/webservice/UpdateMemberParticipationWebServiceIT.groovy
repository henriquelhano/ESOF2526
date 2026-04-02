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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateMemberParticipationWebServiceIT extends SpockTest {
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
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()
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
        def enrollment = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)
        and:
        activity.setStartingDate(NOW.minusDays(4))
        activity.setEndingDate(NOW.minusDays(3))
        activity.setApplicationDeadline(NOW.minusDays(5))
        activityRepository.save(activity)
        and:
        shift.setStartTime(NOW.minusDays(4).plusHours(1))
        shift.setEndTime(NOW.minusDays(4).plusHours(3))
        and:
        def participationDto = createParticipationDto(5, MEMBER_REVIEW, 5, VOLUNTEER_REVIEW)
        participationDto = participationService.createParticipation(shift.id, enrollment.id, participationDto)
        participationId = participationDto.getId()
    }

    def 'login as a member and update a participation'() {
        given: 'a member'
        demoMemberLogin()
        def participationDtoUpdate = createParticipationDto(1, "NEW REVIEW", null, null)

        when: 'the member edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response"
        response.memberRating == 1
        response.memberReview == "NEW REVIEW"
        and: 'check database'
        participationRepository.count() == 1
        def participation = participationRepository.findAll().get(0)
        participation.getMemberRating() == 1
        participation.getMemberReview() == "NEW REVIEW"

        cleanup:
        deleteAll()
    }

    def 'update with a rating of 10 abort and no changes'() {
        given: 'a member'
        demoMemberLogin()
        def participationDtoUpdate = createParticipationDto(10, MEMBER_REVIEW, null, null)

        when: 'the member edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
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
        participation.getMemberRating() == 5

        cleanup:
        deleteAll()
    }

    def 'login as a member of another institution and try to edit a participation'() {
        given: 'a member from another institution'
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        def otherMember = createMember(USER_1_NAME,USER_1_USERNAME,USER_1_PASSWORD,USER_1_EMAIL, AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_1_USERNAME, USER_1_PASSWORD)
        def participationDtoUpdate = createParticipationDto(3, "ANOTHER_REVIEW", null, null)

        when: 'the member tries to edit the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getMemberRating() == 5
        participation.getMemberReview() ==  MEMBER_REVIEW

        cleanup:
        deleteAll()
    }

    def 'login as a member and try to rate a participation with negative rating'() {
        given: 'a member'
        demoMemberLogin()
        def participationDtoUpdate = createParticipationDto(-1, "NEW REVIEW", null, null)

        when: 'the member tries to rate the participation before the activity has ended'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
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
        participation.getMemberRating() == 5
        participation.getMemberReview() == MEMBER_REVIEW

        cleanup:
        deleteAll()
    }

    def 'login as a admin and try to edit a participation'() {
        given: 'a demo'
        demoAdminLogin()
        def participationDtoUpdate = createParticipationDto(1, "ANOTHER_REVIEW", null, null)

        when: 'the admin edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getMemberRating() ==  5
        participation.getMemberReview() == MEMBER_REVIEW

        cleanup:
        deleteAll()
    }


    def 'login as a volunteer and try to update a member rating'() {
        given: 'a demo'
        demoVolunteerLogin()
        def participationDtoUpdate = createParticipationDto(1, "ANOTHER_REVIEW", null, null)

        when: 'the admin edits the participation'
        def response = webClient.put()
                .uri("/participations/" + participationId + "/member")
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoUpdate)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then: "check response status"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        def participation = participationRepository.findAll().get(0)
        participation.getMemberRating() ==  5
        participation.getMemberReview() == MEMBER_REVIEW

        cleanup:
        deleteAll()
    }
}