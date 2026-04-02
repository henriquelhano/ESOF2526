package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import spock.lang.Unroll

@DataJpaTest
class CreateShiftServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'

    def member
    def activity

    def setup() {
        def institution = institutionService.getDemoInstitution()
        member = authUserService.loginDemoMemberAuth().getUser()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
    }

    def "create shift successfully"() {
        given:
        def shiftDto = createShiftDto(SHIFT_LOCATION_1, IN_TWO_DAYS, IN_THREE_DAYS, 2)

        when:
        def result = shiftService.createShift(activity.id, shiftDto)

        then:
        result.location == SHIFT_LOCATION_1
        result.participantsLimit == 2
        result.activityId == activity.id
        and:
        shiftRepository.findAll().size() == 1
        def stored = shiftRepository.findAll().get(0)
        stored.activity.id == activity.id
    }

    @Unroll
    def "invalid arguments: activityId=#activityId | shiftDtoValue=#shiftDtoValue"() {
        given:
        def shiftDto = createShiftDto(SHIFT_LOCATION_1, IN_ONE_DAY, IN_TWO_DAYS, 1)

        when:
        shiftService.createShift(getActivityId(activityId), getShiftDto(shiftDtoValue, shiftDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        shiftRepository.findAll().size() == 0

        where:
        activityId | shiftDtoValue || errorMessage
        null       | EXIST         || ErrorMessage.ACTIVITY_NOT_FOUND
        NO_EXIST   | EXIST         || ErrorMessage.ACTIVITY_NOT_FOUND
        EXIST      | null          || ErrorMessage.SHIFT_NOT_FOUND
    }

    def getActivityId(activityId) {
        if (activityId == EXIST) {
            return activity.id
        } else if (activityId == NO_EXIST) {
            return 222
        } else {
            return null
        }
    }

    def getShiftDto(value, dto) {
        if (value == EXIST) {
            return dto
        } else {
            return null
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}