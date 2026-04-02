package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class UpdateVolunteerRatingParticipationServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity
    def shift
    def enrollment
    def participation
    def volunteer
    def member

    def setup() {
        def institution = institutionService.getDemoInstitution()
        volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        member = authUserService.loginDemoMemberAuth().getUser()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                FOUR_DAYS_AGO, THREE_DAYS_AGO, ONE_DAY_AGO, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shiftDto = createShiftDto(SHIFT_LOCATION_1, THREE_DAYS_AGO, TWO_DAYS_AGO, 3)
        shift = new Shift(activity, shiftDto)
        shiftRepository.save(shift)

        enrollment = new Enrollment()
        enrollment.setVolunteer(volunteer)
        enrollment.setShifts([shift])
        enrollment.setMotivation(ENROLLMENT_MOTIVATION_1)
        enrollment.setEnrollmentDateTime(NOW)
        enrollmentRepository.save(enrollment)

        def participationDto = new ParticipationDto()
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        participation = participationService.createParticipation(shift.id, enrollment.id, participationDto)
    }

    def 'volunteer updates a participation'() {
        given:
        def updatedParticipationDto = new ParticipationDto()
        updatedParticipationDto.volunteerReview = VOLUNTEER_REVIEW
        updatedParticipationDto.volunteerRating = 3

        when:
        def result = participationService.volunteerRating(participation.id, updatedParticipationDto)

        then: "the returned data is correct"
        result.volunteerRating == 3
        result.volunteerReview == VOLUNTEER_REVIEW
        and: "the participation is stored"
        participationRepository.findAll().size() == 1
        and: "contains the correct data"
        def storedParticipation = participationRepository.findAll().get(0)
        storedParticipation.volunteerRating == 3
        storedParticipation.volunteerReview == VOLUNTEER_REVIEW
        storedParticipation.acceptanceDate.isBefore(LocalDateTime.now())
        storedParticipation.activity.id == activity.id
        storedParticipation.volunteer.id == volunteer.id
        storedParticipation.shift.id == shift.id
    }

    @Unroll
    def 'invalid arguments: rating=#rating | review=#review | participationId=#participationId'() {
        given:
        def updatedParticipationDto = new ParticipationDto()
        updatedParticipationDto.volunteerRating = rating
        updatedParticipationDto.volunteerReview = review

        when:
        participationService.volunteerRating(getParticipationId(participationId), updatedParticipationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and: "the participation is in the database"
        participationRepository.findAll().size() == 1

        where:
        rating | participationId | review          || errorMessage
        -2     | EXIST           | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
        3      | null            | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_NOT_FOUND
        3      | NO_EXIST        | MEMBER_REVIEW   || ErrorMessage.PARTICIPATION_NOT_FOUND
        3      | EXIST           | ""              || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        3      | EXIST           | "a".repeat(110) || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
    }

    def getParticipationId(participationId) {
        if (participationId == EXIST) return participation.id
        else if (participationId == NO_EXIST) return 222
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}