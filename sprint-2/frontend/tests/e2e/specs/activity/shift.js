describe('Shift', () => {
  beforeEach(() => {
    cy.deleteAllButArs();
    cy.createDemoEntities();
    cy.createDatabaseInfoForShifts();
  });

  afterEach(() => {
    cy.deleteAllButArs();
  });

  it('create shift', () => {
    const LOCATION = 'Lisbon City Center Hall';
    const PARTICIPANTS_LIMIT = '3';

    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('POST', '/activities/*/shift').as('createShift');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="createShift"]').click();

    cy.get('[data-cy="locationInput"]').type(LOCATION);
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(6)
      .click({ force: true });
    cy.get('#endTimeInput-input').click();
    cy.get('#endTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(7)
      .click({ force: true });
    cy.get('[data-cy="participantsLimitInput"]').type(PARTICIPANTS_LIMIT);

    cy.get('[data-cy="saveShift"]').click();
    cy.wait('@createShift');

    cy.get('[data-cy="shiftsTable"] tbody tr')
      .should('have.length', 1)
      .eq(0)
      .children()
      .eq(0)
      .should('contain', LOCATION);

    cy.logout();
  });

  it('save button disabled when location is too short', () => {
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="createShift"]').click();

    cy.get('[data-cy="locationInput"]').type('Too short');
    cy.get('[data-cy="saveShift"]').should('be.disabled');

    cy.logout();
  });

  it('error when shift start time is after end time', () => {
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShift');
 
    cy.demoMemberLogin();
 
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');
 
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');
 
    cy.get('[data-cy="createShift"]').click();
 
    cy.get('[data-cy="locationInput"]').type('Lisbon City Center Hall');
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(7)
      .click({ force: true });
    cy.get('#endTimeInput-input').click();
    cy.get('#endTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(6)
      .click({ force: true });
    cy.get('[data-cy="participantsLimitInput"]').type('3');
 
    cy.get('[data-cy="saveShift"]').click();
    cy.wait('@createShift');
 
    cy.get('.v-alert.error').should('be.visible');
 
    cy.logout();
  });

  it('error when shift dates are outside activity dates', () => {
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShift');
 
    cy.demoMemberLogin();
 
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');
 
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');
 
    cy.get('[data-cy="createShift"]').click();
 
    cy.get('[data-cy="locationInput"]').type('Lisbon City Center Hall');
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(0)
      .click({ force: true });
    cy.get('#endTimeInput-input').click();
    cy.get('#endTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({ force: true });
    cy.get('[data-cy="participantsLimitInput"]').type('3');
 
    cy.get('[data-cy="saveShift"]').click();
    cy.wait('@createShift');
 
    cy.get('.v-alert.error').should('be.visible');
 
    cy.logout();
  });

  it('create shift button disabled for non-approved activity', () => {
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');

    cy.demoMemberLogin();

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .contains('A2')
      .parents('tr')
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="createShift"]').should('be.disabled');

    cy.logout(); 
  });

  it('error when total participants limit of shifts exceeds activity limit', () => {
    cy.intercept('GET', '/users/*/getInstitution').as('getInstitution');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShift');
 
    cy.demoMemberLogin();
 
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitution');
 
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="manageShifts"]')
      .click();
    cy.wait('@getShifts');
 
    cy.get('[data-cy="createShift"]').click();
 
    cy.get('[data-cy="locationInput"]').type('Lisbon City Center Hall');
    cy.get('#startTimeInput-input').click();
    cy.get('#startTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(0)
      .click({ force: true });
    cy.get('#endTimeInput-input').click();
    cy.get('#endTimeInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(1)
      .click({ force: true });
    cy.get('[data-cy="participantsLimitInput"]').type('4');
 
    cy.get('[data-cy="saveShift"]').click();
    cy.wait('@createShift');
 
    cy.get('.v-alert.error').should('be.visible');
 
    cy.logout();
  });

  
});