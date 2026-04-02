package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.webservice

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateVolunteerParticipationWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def volunteer
    def participationId
    def member


    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        member = authUserService.loginDemoMemberAuth().getUser()
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY.withNano(0), IN_TWO_DAYS.withNano(0), IN_THREE_DAYS.withNano(0), null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shiftDto = createShiftDto(SHIFT_LOCATION_1, IN_TWO_DAYS.withNano(0), IN_THREE_DAYS.withNano(0), 3)
        def shift = createShift(activity, shiftDto)

        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shift.id]
        def enrollment = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        activity.setApplicationDeadline(NOW.minusDays(5))
        activity.setStartingDate(NOW.minusDays(4))
        activity.setEndingDate(NOW.minusDays(3))
        activityRepository.save(activity)

        shift.setStartTime(NOW.minusDays(4))
        shift.setEndTime(NOW.minusDays(3))
        shiftRepository.save(shift)

        def participationDto = new ParticipationDto()
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.volunteerRating = 5
        participationDto.volunteerReview = VOLUNTEER_REVIEW
        participationService.createParticipation(shift.id, enrollment.id, participationDto)
        participationId = participationRepository.findAll().get(0).getId()
    }

    def 'login as a volunteer and update a participation'() {
        given: 'a volunteer'
        demoVolunteerLogin()
        def participationDtoUpdate = new ParticipationDto()
        participationDtoUpdate.volunteerRating = 1
        participationDtoUpdate.volunteerReview = "NEW REVIEW"
        participationDtoUpdate.volunteerId = volunteer.getId()


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
        demoVolunteerLogin()
        def participationDtoUpdate = new ParticipationDto()
        participationDtoUpdate.volunteerRating = 10
        participationDtoUpdate.volunteerReview = VOLUNTEER_REVIEW

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
        def participationDtoUpdate = new ParticipationDto()
        participationDtoUpdate.volunteerRating = 1
        participationDtoUpdate.volunteerReview = "ANOTHER_REVIEW"
        participationDtoUpdate.volunteerId = volunteer.id

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
        def participationDtoUpdate = new ParticipationDto()
        participationDtoUpdate.volunteerRating = 1
        participationDtoUpdate.volunteerReview = "ANOTHER_REVIEW"
        participationDtoUpdate.volunteerId = volunteer.id

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
        def participationDtoUpdate = new ParticipationDto()
        participationDtoUpdate.volunteerRating = 1
        participationDtoUpdate.volunteerReview = "ANOTHER_REVIEW"
        participationDtoUpdate.volunteerId = volunteer.id

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

