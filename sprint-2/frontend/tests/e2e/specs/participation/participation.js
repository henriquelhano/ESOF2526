describe('Participation', () => {
  beforeEach(() => {
    cy.deleteAllButArs()
    cy.createDemoEntities();
    cy.createDatabaseInfoForParticipations()
  });

  afterEach(() => {
    cy.deleteAllButArs()
  });

  it('create participation', () => {
    const MEMBER_REVIEW_1 = 'The volunteer did a good job';

    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');
    cy.intercept('POST', '/participations/*/enrollment/*').as('participation');

    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    // Applications = index 4
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 2)
      .eq(0).children().eq(4).should('contain', '2')

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1).children().eq(4).should('contain', '2')

    // open enrollments
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]').click()

    cy.wait('@enrollments');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .should('have.length', 2)
      .eq(0).children().eq(4).should('contain', 'false')

    // create participation
    cy.get('[data-cy="selectParticipantButton"]').first().click();

    cy.get('[data-cy="participantsNumberInput"]').type('3');
    cy.get('[data-cy="participantsReviewInput"]').type(MEMBER_REVIEW_1);

    cy.get('[data-cy="createParticipation"]').click();
    cy.wait('@participation');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'true')

    // back to activities
    cy.get('[data-cy="getActivities"]').click();

    // Participations = index 5
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0).children().eq(5).should('contain', '2')

    cy.logout();
  });

  it('update participation', () => {
    const MEMBER_REVIEW_1 = 'The volunteer did an okay job';
    const MEMBER_REVIEW_2 = 'The volunteer did a good job';
    const VOLUNTEER_REVIEW = 'The activity was well organized';

    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');
    cy.intercept('POST', '/participations/*/enrollment/*').as('participation');
    cy.intercept('PUT', '/participations/*').as('updateParticipation');
    cy.intercept('GET', '/activities/1/participations').as('participations');

    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 2)
      .eq(0).children().eq(4).should('contain', '2')

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1).children().eq(4).should('contain', '2')

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]').click()

    cy.wait('@enrollments');
    cy.wait('@participations');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .should('have.length', 2);
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'false');

    // create participation
    cy.get('[data-cy="selectParticipantButton"]').first().click();

    cy.get('[data-cy="participantsNumberInput"]').type('3');
    cy.get('[data-cy="participantsReviewInput"]').type(MEMBER_REVIEW_1);

    cy.get('[data-cy="createParticipation"]').click();
    cy.wait('@participation');
    cy.wait('@participations');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'true');

    // edit participation
    cy.get('[data-cy="editParticipantButton"]').first().click();

    cy.get('[data-cy="participantsNumberInput"]').clear().type('5');
    cy.get('[data-cy="participantsReviewInput"]').clear().type(MEMBER_REVIEW_2);

    cy.get('[data-cy="createParticipation"]').click();
    cy.wait('@participations');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'true');

    // back to activities
    cy.get('[data-cy="getActivities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0).children().eq(5).should('contain', '2');

    cy.logout();

    // volunteer writes review
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerEnrollments"]').click();

    cy.get('[data-cy="writeParticipationButton"]').first().click();

    cy.get('[data-cy="ratingInput"]').type('3');
    cy.get('[data-cy="reviewInput"]').type(VOLUNTEER_REVIEW);
    cy.get('[data-cy="saveParticipation"]').click();

    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(6)
      .invoke('text')
      .should('include', MEMBER_REVIEW_2)
      .and('match', /★{5} 5\/5/);

    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(7)
      .invoke('text')
      .should('include', VOLUNTEER_REVIEW)
      .and('match', /★{3}☆{2} 3\/5/);

    cy.logout();

    // back as member
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', '2');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]').click();

    cy.wait('@enrollments');

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(2)
      .invoke('text')
      .should('include', MEMBER_REVIEW_2)
      .and('match', /★{5} 5\/5/);

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(3)
      .invoke('text')
      .should('include', VOLUNTEER_REVIEW)
      .and('match', /★{3}☆{2} 3\/5/);

    cy.logout();
  });

  it('delete participation', () => {
    const MEMBER_REVIEW_1 = 'The volunteer did an okay job';

    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');
    cy.intercept('POST', '/participations/*/enrollment/*').as('participation');
    cy.intercept('DELETE', '/participations/*').as('deleteParticipation');

    cy.demoMemberLogin();
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', '2');

    // open enrollments
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]').click();
    cy.wait('@enrollments');

    // Before creating participation: should be false
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'false');

    // create participation
    cy.get('[data-cy="selectParticipantButton"]').first().click();
    cy.get('[data-cy="participantsNumberInput"]').type('3');
    cy.get('[data-cy="participantsReviewInput"]').type(MEMBER_REVIEW_1);
    cy.get('[data-cy="createParticipation"]').click();
    cy.wait('@participation');

    // After creating: should be true
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'true');

    // delete participation
    cy.get('[data-cy="deleteParticipantButton"]').first().click();
    cy.get('[data-cy="deleteParticipationDialogButton"]').click();
    cy.wait('@deleteParticipation');

    // After delete: should be false
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .eq(0).children().eq(4).should('contain', 'false');

    // back to activities view
    cy.get('[data-cy="getActivities"]').click();
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0).children().eq(5).should('contain', '1');

    cy.logout();
  });

  it('should not allow participation when shift capacity is exceeded', () => {
    const ERROR_MESSAGE = 'Current participants cannot exceed participants limit';
    const MEMBER_REVIEW = 'The volunteer did a good job';

    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForParticipationShiftCapacityValidation();

    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');
    cy.intercept('POST', '/participations/*/enrollment/*').as('participation');

    cy.demoMemberLogin();
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
      
    cy.get('[data-cy="memberActivitiesTable"] tbody tr').eq(0).find('[data-cy="showEnrollments"]').click();
    cy.wait('@enrollments');

    // Selecionar o segundo voluntário (o que vai falhar)
    getTargetEnrollmentRow().find('[data-cy="selectParticipantButton"]').click();
      
    // Rating e Review
    cy.get('[data-cy="participantsNumberInput"]').type('3'); 
    cy.get('[data-cy="participantsReviewInput"]').type(MEMBER_REVIEW);

    cy.get('[data-cy="createParticipation"]').click();

    // Aguarda a resposta 400
    cy.wait('@participation').its('response.statusCode').should('eq', 400);

    cy.get('.v-alert')
      .should('be.visible')
      .and('contain', ERROR_MESSAGE);
    cy.get('button').contains('Close').click({ force: true });
    getTargetEnrollmentRow().children().eq(4).should('contain', 'false');
  });

  function getTargetEnrollmentRow() {
    return cy.get('[data-cy="activityEnrollmentsTable"] tbody tr').eq(1);
  }
});