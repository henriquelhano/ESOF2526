package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class CreateShiftMethodTest extends SpockTest {

    Activity activity = Mock()
    def shiftDto

    def setup() {
        given: "valid shift dto"
        shiftDto = new ShiftDto()
        shiftDto.setLocation(SHIFT_LOCATION_1)
        shiftDto.setStartTime(DateHandler.toISOString(IN_ONE_DAY))
        shiftDto.setEndTime(DateHandler.toISOString(IN_TWO_DAYS))
        shiftDto.setParticipantsLimit(1)
    }

    def "create shift successfully"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []

        when:
        def result = new Shift(activity, shiftDto)

        then:
        noExceptionThrown()
        result.getId() == null
        result.location == SHIFT_LOCATION_1
        result.participantsLimit == 1
        result.startTime == DateHandler.toLocalDateTime(shiftDto.startTime)
        result.endTime == DateHandler.toLocalDateTime(shiftDto.endTime)
        result.activity == activity

        1 * activity.addShift(_)
    }

    def "add and remove shift from activity"() {
        given:
        Activity activity = new Activity()
        activity.setShifts(new ArrayList<>())

        Shift shift = Mock()

        when:
        activity.addShift(shift)

        then:
        activity.getShifts().size() == 1

        when:
        activity.removeShift(shift)

        then:
        activity.getShifts().isEmpty()
    }

    def "create shift with enrollments"(){
        given:
        Shift shift = new Shift()
        Enrollment enrollment1 = new Enrollment()
        Enrollment enrollment2 = new Enrollment()

        when:
        shift.setEnrollments(List.of(enrollment1, enrollment2))

        then:
        shift.getEnrollments().size() == 2
        shift.getEnrollments().contains(enrollment1)
        shift.getEnrollments().contains(enrollment2)
    }
    def "add and remove participation in existing participations in shift"() {
        given:
        Shift shift = new Shift()
        Participation participation1 = new Participation()
        Participation participation2 = new Participation()
        shift.setParticipations(new ArrayList<>([participation1]))

        when:
        shift.addParticipation(participation2)

        then:
        shift.getParticipations().size() == 2
        shift.getParticipations().contains(participation1)
        shift.getParticipations().contains(participation2)

        when:
        shift.deleteParticipation(participation1)

        then:
        shift.getParticipations().size() == 1
        !shift.getParticipations().contains(participation1)
        shift.getParticipations().contains(participation2)
    }
    
    @Unroll
    def "create shift with invalid location: location=#location"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []
        shiftDto.setLocation(location)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_LOCATION_INVALID

        where:
        location << [null, "", "   ", "too short", "a" * 201]
    }

    def "create shift and violate start before end invariant"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []
        shiftDto.setStartTime(DateHandler.toISOString(IN_TWO_DAYS))
        shiftDto.setEndTime(DateHandler.toISOString(IN_ONE_DAY))

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_START_AFTER_END
    }

    def "create shift and violate dates within activity - start before activity"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_TWO_DAYS
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []
        shiftDto.setStartTime(DateHandler.toISOString(IN_ONE_DAY))
        shiftDto.setEndTime(DateHandler.toISOString(IN_TWO_DAYS))

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_DATES_OUTSIDE_ACTIVITY
    }

    def "create shift and violate dates within activity - end after activity"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []
        shiftDto.setEndTime(DateHandler.toISOString(IN_THREE_DAYS))

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_DATES_OUTSIDE_ACTIVITY
    }

    @Unroll
    def "create shift with invalid participants limit: limit=#limit"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []
        shiftDto.setParticipantsLimit(limit)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_PARTICIPANTS_LIMIT_INVALID

        where:
        limit << [null, 0, -1]
    }

    def "create shift on non-approved activity"() {
        given:
        activity.getState() >> Activity.State.REPORTED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5
        activity.getShifts() >> []

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_ACTIVITY_NOT_APPROVED
    }

    def "create shift that exceeds activity participants limit"() {
        given:
        Shift existingShift = Mock()
        existingShift.getParticipantsLimit() >> 4

        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 4
        activity.getShifts() >> [existingShift]

        shiftDto.setParticipantsLimit(1)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.errorMessage == ErrorMessage.SHIFT_PARTICIPANTS_LIMIT_EXCEEDS_ACTIVITY
    }

    def "create shift when activity already contains this shift"() {
        given:
        activity.getState() >> Activity.State.APPROVED
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        activity.getParticipantsNumberLimit() >> 5

        List<Shift> shifts = []
        activity.getShifts() >> shifts
        activity.addShift(_) >> { Shift s -> shifts.add(s) }

        when:
        new Shift(activity, shiftDto)

        then:
        noExceptionThrown()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}