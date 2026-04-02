package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetShiftsByActivityWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity

    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                NOW, IN_ONE_DAY, IN_THREE_DAYS, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shiftDto1 = createShiftDto(SHIFT_LOCATION_1, IN_ONE_DAY, IN_TWO_DAYS, 1)
        def shiftDto2 = createShiftDto(SHIFT_LOCATION_2, IN_TWO_DAYS, IN_THREE_DAYS, 1)
        createShift(activity, shiftDto1)
        createShift(activity, shiftDto2)
    }

    def "member gets shifts"() {
        given:
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
        response.size() == 2
    }

    def "volunteer gets shifts"() {
        given:
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
        response.size() == 2
    }

    def "unauthenticated user gets shifts"() {
        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
    }
}