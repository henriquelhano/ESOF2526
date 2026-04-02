package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException

@DataJpaTest
class GetShiftsByActivityServiceTest extends SpockTest {

    def activity

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                NOW , IN_ONE_DAY, IN_THREE_DAYS, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
    }

    def "get shifts by activity with no shifts"() {
        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 0
    }

    def "get shifts by activity with shifts"() {
        given:
        def shiftDto1 = createShiftDto(SHIFT_LOCATION_1, IN_ONE_DAY, IN_TWO_DAYS, 1)
        def shiftDto2 = createShiftDto(SHIFT_LOCATION_2, IN_TWO_DAYS, IN_THREE_DAYS, 1)
        createShift(activity, shiftDto1)
        createShift(activity, shiftDto2)

        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 2
    }

    def "get shifts with null activity id"() {
        when:
        shiftService.getShiftsByActivity(null)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_NOT_FOUND
    }

    def "get shifts with non-existing activity id"() {
        when:
        shiftService.getShiftsByActivity(222)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}