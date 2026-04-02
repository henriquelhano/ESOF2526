package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetShiftsByActivityWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, ONE_DAY_AGO)
    }

    def 'get two shifts sorted by start time without authentication'() {
        given:
        createShift(activity, TWO_DAYS_AGO.plusHours(2), TWO_DAYS_AGO.plusHours(4), 2, SHIFT_LOCATION)
        createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(1), 3, SHIFT_LOCATION)

        when: 'no login - unauthenticated request'
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        DateHandler.toLocalDateTime(response.get(0).startTime).withNano(0) == TWO_DAYS_AGO.withNano(0)
        response.get(0).participantsLimit == 3
        DateHandler.toLocalDateTime(response.get(1).startTime).withNano(0) == TWO_DAYS_AGO.plusHours(2).withNano(0)
        response.get(1).participantsLimit == 2
    }

    def 'volunteer can get shifts'() {
        given:
        createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(2), 3, SHIFT_LOCATION)
        demoVolunteerLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 1
    }

    def 'member can get shifts'() {
        given:
        createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(2), 3, SHIFT_LOCATION)
        demoMemberLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 1
    }

    def 'admin can get shifts'() {
        given:
        createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(2), 3, SHIFT_LOCATION)
        demoAdminLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 1
    }

    def 'get shifts for activity with no shifts'() {
        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 0
    }

    def 'activity does not exist'() {
        when:
        webClient.get()
                .uri('/activities/' + 222 + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
    }

    def cleanup() {
        deleteAll()
    }
}
