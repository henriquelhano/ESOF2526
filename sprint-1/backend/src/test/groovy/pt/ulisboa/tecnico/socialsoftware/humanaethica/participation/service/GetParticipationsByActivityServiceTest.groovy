package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetParticipationsByActivityServiceTest extends SpockTest {
    def activity
    def otherActivity
    def shift
    def otherShift
    def participationDto1
    def participationDto2

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, NOW, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        activityDto.name = ACTIVITY_NAME_2
        otherActivity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(otherActivity)

        def shiftDto = createShiftDto(SHIFT_LOCATION_1, TWO_DAYS_AGO, ONE_DAY_AGO, 3)
        shift = new Shift(activity, shiftDto)
        shiftRepository.save(shift)

        def otherShiftDto = createShiftDto(SHIFT_LOCATION_2, TWO_DAYS_AGO, ONE_DAY_AGO, 3)
        otherShift = new Shift(otherActivity, otherShiftDto)
        shiftRepository.save(otherShift)

        participationDto1 = new ParticipationDto()
        participationDto1.memberRating = 1
        participationDto1.memberReview = MEMBER_REVIEW
        participationDto2 = new ParticipationDto()
        participationDto2.memberRating = 2
        participationDto2.memberReview = MEMBER_REVIEW
    }

    def "get two participations of the same activity"() {
        given:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment1 = new Enrollment()
        enrollment1.setVolunteer(volunteerOne)
        enrollment1.setShifts([shift])
        enrollment1.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment1.setEnrollmentDateTime(NOW)
        enrollmentRepository.save(enrollment1)
        def enrollment2 = new Enrollment()
        enrollment2.setVolunteer(volunteerTwo)
        enrollment2.setShifts([shift])
        enrollment2.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment2.setEnrollmentDateTime(NOW)
        enrollmentRepository.save(enrollment2)
        and:
        createParticipation(enrollment1, shift, participationDto1)
        createParticipation(enrollment2, shift, participationDto2)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 2
        participations.get(0).memberRating == 1
        participations.get(1).memberRating == 2
    }

    def "get one participation of an activity"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment1 = new Enrollment()
        enrollment1.setVolunteer(volunteer)
        enrollment1.setShifts([shift])
        enrollment1.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment1.setEnrollmentDateTime(LocalDateTime.now())
        enrollmentRepository.save(enrollment1)
        def enrollment2 = new Enrollment()
        enrollment2.setVolunteer(volunteer)
        enrollment2.setShifts([otherShift])
        enrollment2.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment2.setEnrollmentDateTime(LocalDateTime.now())
        enrollmentRepository.save(enrollment2)
        and:
        createParticipation(enrollment1, shift, participationDto1)
        createParticipation(enrollment2, otherShift, participationDto1)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 1
        participations.get(0).memberRating == 1
    }

    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        participationService.getParticipationsByActivity(activityId)

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