package pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetActivitiesWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def setup() {
        deleteAll()
        and:
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        and:
        def institution = institutionService.getDemoInstitution()
        given: "activity info"
        def themes = new ArrayList<>()
        themes.add(createTheme(THEME_NAME_1, Theme.State.APPROVED,null))
        and: "an activity"
        createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
        and: 'another activity'
        createActivity(institution, ACTIVITY_NAME_2, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, themes)
    }

    def "get activities"() {
        when:
        def response = webClient.get()
                .uri('/activities')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ActivityDto.class)
                .collectList()
                .block()

        then: "check response"
        response.size() == 2
        response.get(1).name == ACTIVITY_NAME_2
        response.get(1).region == ACTIVITY_REGION_1
        response.get(1).participantsNumberLimit == 1
        response.get(1).description == ACTIVITY_DESCRIPTION_1
        DateHandler.toLocalDateTime(response.get(1).startingDate).withNano(0) == IN_TWO_DAYS.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).endingDate).withNano(0) == IN_THREE_DAYS.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).applicationDeadline).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(1).themes.size() == 1

        cleanup:
        deleteAll()
    }
}
