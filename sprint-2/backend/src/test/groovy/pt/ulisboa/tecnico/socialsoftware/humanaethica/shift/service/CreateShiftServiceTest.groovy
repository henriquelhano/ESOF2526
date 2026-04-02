package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import spock.lang.Unroll
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@DataJpaTest
class CreateShiftServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity
    def shiftDto

    def setup() {
        given:
        def institution = institutionService.getDemoInstitution()
        and:
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1, IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        shiftDto = createShiftDto(IN_TWO_DAYS.plusHours(1),IN_THREE_DAYS.minusHours(1),1, SHIFT_LOCATION)
    }

    def "create shift with valid data"() {
        when:
        def result = shiftService.createShift(activity.getId(), shiftDto)

        then:
        result.getId() != null
        result.getStartTime() == DateHandler.toISOString(IN_TWO_DAYS.plusHours(1))
        result.getEndTime() == DateHandler.toISOString(IN_THREE_DAYS.minusHours(1))
        result.getParticipantsLimit() == 1
        result.getLocation() == SHIFT_LOCATION
        shiftRepository.findAll().size() == 1
        and: "the stored data is correct"
        def storedShift = shiftRepository.findById(result.id).get()
        storedShift.getActivity() == activity
        storedShift.getStartTime() == IN_TWO_DAYS.plusHours(1)
        storedShift.getEndTime() == IN_THREE_DAYS.minusHours(1)
        storedShift.getParticipantsLimit() == 1
        storedShift.getLocation() == SHIFT_LOCATION
    }

    @Unroll
    def "create shift with invalid data: #error"() {
        when:
        shiftService.createShift(getActivityId(activityId), getShiftDto(shiftValue, shiftDto))

        then:
        def exception = thrown(HEException)
        exception.getErrorMessage() == error

        where:
        activityId | shiftValue || error
        null       | EXIST      || ErrorMessage.ACTIVITY_NOT_FOUND
        NO_EXIST   | EXIST      || ErrorMessage.ACTIVITY_NOT_FOUND
        EXIST      | null       || ErrorMessage.SHIFT_INFORMATION_REQUIRED
    }

    def getActivityId(activityId) {
        if (activityId == EXIST)
            return activity.id
        else if (activityId == NO_EXIST)
            return 222
        else
            return null
    }

    def getShiftDto(value, shiftDto) {
        if (value == EXIST) {
            return shiftDto
        }
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
