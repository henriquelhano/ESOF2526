package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateShiftWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shiftDto

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
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        shiftDto = createShiftDto(IN_TWO_DAYS.plusHours(1),IN_THREE_DAYS.minusHours(1),1, SHIFT_LOCATION)
    }

    def "login as member, and create a shift"() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.getId() + '/shift')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "check response data"
        response.startTime == DateHandler.toISOString(IN_TWO_DAYS.plusHours(1))
        response.endTime == DateHandler.toISOString(IN_THREE_DAYS.minusHours(1))
        response.participantsLimit == 1
        response.location == SHIFT_LOCATION
        response.activityId == activity.getId()
        
        and: 'check database data'
        shiftRepository.count() == 1
        def shift = shiftRepository.findAll().get(0)
        shift.getLocation() == SHIFT_LOCATION
        shift.getActivity().getId() == activity.getId()

        and: 'check getActivities include shifts'
        def activities = webClient.get()
                .uri('/activities')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ActivityDto.class)
                .collectList()
                .block()
        
        activities.size() == 1
        activities.get(0).getShifts().size() == 1
        activities.get(0).getShifts().get(0).getLocation() == SHIFT_LOCATION

        cleanup:
        deleteAll()
    }

    def "login as member of different institution, and create a shift"() {
        given: "another institution and member"
        def institution = createInstitution("name", "email", "nif")
        def member = createMember(USER_1_NAME, USER_1_USERNAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, institution, User.State.APPROVED)

        and: "login"
        normalUserLogin(USER_1_USERNAME, USER_1_PASSWORD)

        when:
        webClient.post()
                .uri('/activities/' + activity.getId() + '/shift')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "error 403"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def "login as volunteer, and create a shift"() {
        given:
        demoVolunteerLogin()

        when:
        webClient.post()
                .uri('/activities/' + activity.getId() + '/shift')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "error 403"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def "guest cannot create a shift"() {
        when:
        webClient.post()
                .uri('/activities/' + activity.getId() + '/shift')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "error 403"
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
