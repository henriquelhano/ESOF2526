package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateShiftWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shiftDto

     def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY.withNano(0), IN_TWO_DAYS.withNano(0), IN_THREE_DAYS.withNano(0), null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activity.setState(Activity.State.APPROVED)
        activityRepository.save(activity)

        shiftDto = createShiftDto(SHIFT_LOCATION_1,
        IN_TWO_DAYS.withNano(0), IN_THREE_DAYS.withNano(0), 3)
    }
    def 'member creates shift successfully'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        response.location == SHIFT_LOCATION_1
        response.participantsLimit == 3
        response.activityId == activity.id
        DateHandler.toLocalDateTime(response.startTime).withNano(0) == IN_TWO_DAYS.withNano(0)
        DateHandler.toLocalDateTime(response.endTime).withNano(0) == IN_THREE_DAYS.withNano(0)
        and: 'check database'
        shiftRepository.count() == 1
        def stored = shiftRepository.findAll().get(0)
        stored.activity.id == activity.id
        stored.participantsLimit == 3

        cleanup:
        deleteAll()
    }

    def 'member creates shift with invalid data'() {
        given:
        demoMemberLogin()
        shiftDto.setParticipantsLimit(-1)

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'member of another institution cannot create shift'() {
        given:
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        createMember(USER_3_NAME, USER_3_USERNAME, USER_3_PASSWORD, USER_3_EMAIL,
                AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_3_USERNAME, USER_3_PASSWORD)

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'volunteer cannot create shift'() {
        given:
        demoVolunteerLogin()

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'admin cannot create shift'() {
        given:
        demoAdminLogin()

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'unauthenticated user cannot create shift'() {
        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
