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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class DeleteParticipationServiceTest extends SpockTest {
    public static final String FIRST_PARTICIPATION = 'first'
    public static final String SECOND_PARTICIPATION = 'second'
    public static final Integer FIRST_PARTICIPATION_RATING = 5
    public static final Integer SECOND_PARTICIPATION_RATING = 2
    def activity
    def shift
    def enrollment
    def participation
    def firstParticipation
    def volunteer
    def secondParticipation

    def setup() {
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shiftDto = createShiftDto(SHIFT_LOCATION_1, IN_TWO_DAYS, IN_THREE_DAYS, 3)
        shift = new Shift(activity, shiftDto)
        shiftRepository.save(shift)

        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        enrollment = new Enrollment()
        enrollment.setVolunteer(volunteer)
        enrollment.setShifts([shift])
        enrollment.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment.setEnrollmentDateTime(LocalDateTime.now())
        enrollmentRepository.save(enrollment)

        activity.setApplicationDeadline(THREE_DAYS_AGO)
        activity.setStartingDate(TWO_DAYS_AGO)
        activity.setEndingDate(ONE_DAY_AGO)
        activityRepository.save(activity)

        shift.setStartTime(TWO_DAYS_AGO)
        shift.setEndTime(ONE_DAY_AGO)
        shiftRepository.save(shift)

        def participationDto = new ParticipationDto()
        participationDto.volunteerRating = 5
        participationDto.volunteerReview = VOLUNTEER_REVIEW
        participation = createParticipation(enrollment, shift, participationDto)
    }

    def 'delete participation'() {
        given:
        firstParticipation = participationRepository.findAll().get(0)

        when:
        participationService.deleteParticipation(firstParticipation.id)

        then: "the participation was deleted"
        participationRepository.findAll().size() == 0
    }

    @Unroll
    def 'two participations exist and one is deleted: participationId=#participationId | deletedRating=#deletedRating | remainingRating=#remainingRating'() {
        given:
        def volunteer2 = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollmentDto2 = new EnrollmentDto()
        enrollmentDto2.motivation = ENROLLMENT_MOTIVATION_1
        def enrollment2 = new Enrollment()
        enrollment2.setVolunteer(volunteer2)
        enrollment2.setShifts([shift])
        enrollment2.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment2.setEnrollmentDateTime(LocalDateTime.now())
        enrollmentRepository.save(enrollment2)
        def participationDto2 = new ParticipationDto()
        participationDto2.volunteerRating = 2
        participationDto2.volunteerReview = VOLUNTEER_REVIEW
        createParticipation(enrollment2, shift, participationDto2)
        firstParticipation = participationRepository.findAll().get(0)
        secondParticipation = participationRepository.findAll().get(1)

        when:
        def result = participationService.deleteParticipation(getFirstOrSecondParticipation(participationId))

        then: "the participation was deleted"
        participationRepository.findAll().size() == 1
        result.volunteerRating == deletedRating
        def remainingParticipation = participationRepository.findAll().get(0)
        remainingParticipation.volunteerRating == remainingRating

        where:
        participationId      || deletedRating               || remainingRating
        FIRST_PARTICIPATION  || FIRST_PARTICIPATION_RATING  || SECOND_PARTICIPATION_RATING
        SECOND_PARTICIPATION || SECOND_PARTICIPATION_RATING || FIRST_PARTICIPATION_RATING
    }

    def getFirstOrSecondParticipation(participationId) {
        if (participationId == FIRST_PARTICIPATION)
            return firstParticipation.id
        else if (participationId == SECOND_PARTICIPATION)
            return secondParticipation.id
        return null
    }

    def 'two participation exist and are both deleted'() {
        given:
        def volunteer2 = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollmentDto2 = new EnrollmentDto()
        enrollmentDto2.motivation = ENROLLMENT_MOTIVATION_1
        def enrollment2 = new Enrollment()
        enrollment2.setVolunteer(volunteer2)
        enrollment2.setShifts([shift])
        enrollment2.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment2.setEnrollmentDateTime(LocalDateTime.now())
        enrollmentRepository.save(enrollment2)
        def participationDto2 = new ParticipationDto()
        participationDto2.volunteerRating = 2
        participationDto2.volunteerReview = VOLUNTEER_REVIEW
        createParticipation(enrollment2, shift, participationDto2)
        firstParticipation = participationRepository.findAll().get(0)
        secondParticipation = participationRepository.findAll().get(1)

        when:
        participationService.deleteParticipation(firstParticipation.id)
        participationService.deleteParticipation(secondParticipation.id)

        then: "are the participation were deleted"
        participationRepository.findAll().size() == 0
    }

    @Unroll
    def 'invalid arguments: participationId:#participationId'() {
        when:
        participationService.deleteParticipation(participationId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and: "the participation is in the database"
        participationRepository.findAll().size() == 1

        where:
        participationId || errorMessage
        null            || ErrorMessage.PARTICIPATION_NOT_FOUND
        222             || ErrorMessage.PARTICIPATION_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}