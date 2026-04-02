package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class GetShiftsByActivityServiceTest extends SpockTest {
    def activity
    def otherActivity

    def setup() {
        given:
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, ONE_DAY_AGO)
        and:
        otherActivity = createActivity(institution, ACTIVITY_NAME_2, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, ONE_DAY_AGO)
    }

    def "get two shifts of the same activity"() {
        given:
        def shift1 = createShift(activity, TWO_DAYS_AGO.plusHours(2), TWO_DAYS_AGO.plusHours(4), 2, SHIFT_LOCATION)
        def shift2 = createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(1), 3, SHIFT_LOCATION)

        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then: 'shifts returned sorted by start time'
        result.size() == 2
        result.get(0).startTime == DateHandler.toISOString(TWO_DAYS_AGO)
        result.get(0).participantsLimit == 3
        result.get(1).startTime == DateHandler.toISOString(TWO_DAYS_AGO.plusHours(2))
        result.get(1).participantsLimit == 2
    }

    def "get only shifts for target activity"() {
        given:
        createShift(activity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(2), 2, SHIFT_LOCATION)
        createShift(otherActivity, TWO_DAYS_AGO, TWO_DAYS_AGO.plusHours(2), 3, SHIFT_LOCATION)

        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 1
        result.get(0).participantsLimit == 2
        result.get(0).activityId == activity.id
    }

    def "get shifts for activity with no shifts"() {
        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 0
    }

    @Unroll
    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        shiftService.getShiftsByActivity(activityId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityId || errorMessage
        null       || ErrorMessage.ACTIVITY_NOT_FOUND
        222        || ErrorMessage.ACTIVITY_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
