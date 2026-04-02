package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*

@DataJpaTest
class CreateShiftMethodTest extends SpockTest {
    Activity activity = Mock()
    def shiftDto

    def setup() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getShifts() >> []
        activity.getParticipantsNumberLimit() >> 10
        and:
        shiftDto = new ShiftDto()
        shiftDto.startTime = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.endTime = DateHandler.toISOString(IN_TWO_DAYS)
        shiftDto.participantsLimit = 5
        shiftDto.location = "This is a valid location with more than twenty characters"
    }

    def "create shift with valid data"() {
        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    def "create shift with null start time"() {
        given:
        shiftDto.startTime = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_START_TIME_REQUIRED
    }

    def "create shift with null end time"() {
        given:
        shiftDto.endTime = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_END_TIME_REQUIRED
    }

    @Unroll
    def "create shift with valid participants limit: #description"() {
        given:
        shiftDto.participantsLimit = limit

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getParticipantsLimit() == limit
        and: "invocations"
        1 * activity.addShift(_)

        where:
        limit | description
        1     | "participants limit is one (minimum valid boundary)"
        5     | "participants limit is five"
    }

    @Unroll
    def "create shift with invalid participants limit: #description"() {
        given:
        shiftDto.participantsLimit = limit

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_PARTICIPANTS_LIMIT_POSITIVE

        where:
        limit | description
        null  | "participants limit is not defined"
        0     | "participants limit is zero (boundary)"
        -10   | "participants limit is significantly negative"
    }

    def "create shift with null location"() {
        given:
        shiftDto.location = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_REQUIRED
    }

    @Unroll
    def "create shift with valid location size, boundary analysis: length #length"() {
        given:
        shiftDto.location = "A" * length

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)

        where:
        length << [20, 100, 200]
    }

    @Unroll
    def "create shift with invalid location size: length #length"() {
        given:
        shiftDto.location = "A" * length

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_INVALID

        where:
        length << [0, 19, 201, 250]
    }

    def "create shift with valid time range, boundary analysis"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.endTime = DateHandler.toISOString(IN_ONE_DAY.plusMinutes(1))

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_ONE_DAY.plusMinutes(1)
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    @Unroll
    def "create shift with invalid time range: #description"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_START_TIME_BEFORE_END_TIME

        where:
        startTime    | endTime      | description
        IN_ONE_DAY   | IN_ONE_DAY   | "start time equal to end time"
        IN_TWO_DAYS  | IN_ONE_DAY   | "start time after end time"
    }

    @Unroll
    def "create shift with start time immediately after now"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(NOW.plusMinutes(1))
        shiftDto.endTime = DateHandler.toISOString(IN_TWO_DAYS)

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == NOW.plusMinutes(1)
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    @Unroll
    def "create shift with dates within activity range: #description"() {
        given:
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        def result = new Shift(activity, shiftDto)

        then:
        result.getStartTime() == startTime
        result.getEndTime() == endTime

        where:
        startTime                      | endTime                        | description
        IN_ONE_DAY                     | IN_TWO_DAYS                    | "start at activity start"
        IN_TWO_DAYS                    | IN_THREE_DAYS                  | "end at activity end"
    }

    @Unroll
    def "create shift with dates outside activity range: #description"() {
        given:
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_DATES_WITHIN_ACTIVITY

        where:
        startTime                      | endTime                        | description
        IN_ONE_DAY.minusHours(10)      | IN_TWO_DAYS                    | "start before activity start"
        IN_ONE_DAY.minusMinutes(1)     | IN_TWO_DAYS                    | "start one minute before activity start"
        IN_TWO_DAYS                    | IN_THREE_DAYS.plusMinutes(1)   | "end one minute after activity end"
        IN_TWO_DAYS                    | IN_THREE_DAYS.plusDays(1)      | "end after activity end"
    }

    @Unroll
    def "create shift with non-approved activity: #state"() {
        given:
        activity = Mock(Activity)
        activity.getState() >> state

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_ON_NON_APPROVED_ACTIVITY

        where:
        state << [Activity.State.REPORTED, Activity.State.SUSPENDED]
    }

    @Unroll
    def "create shift with total participants exceeding activity limit"() {
        given:
        activity = Mock(Activity)
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> 10
        def existingShift = Mock(Shift)
        existingShift.getParticipantsLimit() >> limit
        activity.getShifts() >> [existingShift]

        shiftDto.participantsLimit = 3

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == TOTAL_PARTICIPANTS_EXCEEDS_ACTIVITY_LIMIT

        where:
        limit << [8, 10]
    }

    @Unroll
    def "create shift with total participants within activity limit boundary"() {
        given:
        activity.getParticipantsNumberLimit() >> 10
        def existingShift = Mock(Shift)
        existingShift.getParticipantsLimit() >> limit
        activity.getShifts() >> [existingShift]

        shiftDto.participantsLimit = 3

        when:
        def shift = new Shift(activity, shiftDto)

        then:
        shift.participantsLimit == 3

        where:
        limit << [1, 7]
    }
    
    def "create shift and verify associations are initialized"() {
        when:
        def result = new Shift(activity, shiftDto)

        then:
        result.getEnrollments() != null
        result.getEnrollments().isEmpty()
        result.getParticipations() != null
        result.getParticipations().isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
