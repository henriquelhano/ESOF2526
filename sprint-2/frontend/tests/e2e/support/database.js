const credentials = {
  user: Cypress.env('psql_db_username'),
  host: Cypress.env('psql_db_host'),
  database: Cypress.env('psql_db_name'),
  password: Cypress.env('psql_db_password'),
  port: Cypress.env('psql_db_port'),
};

const INSTITUTION_COLUMNS = "institutions (id, active, confirmation_token, creation_date, email, name, nif, token_generation_date)";
const USER_COLUMNS = "users (user_type, id, creation_date, name, role, state, institution_id)";
const AUTH_USERS_COLUMNS = "auth_users (auth_type, id, active, email, username, user_id)";
const ACTIVITY_COLUMNS = "activity (id, application_deadline, creation_date, description, ending_date, name, participants_number_limit, region, starting_date, state, institution_id)";
const ENROLLMENT_COLUMNS = "enrollment (id, enrollment_date_time, motivation, volunteer_id)";
const PARTICIPATION_COLUMNS = "participation (id, acceptance_date, member_rating, enrollment_id, shift_id)";
const SHIFT_COLUMNS = "shift (id, end_time, location, participants_limit, start_time, activity_id)";
const ENROLLMENT_SHIFTS_COLUMNS = "enrollment_shifts (enrollments_id, shifts_id)";
const ASSESSMENT_COLUMNS = "assessment (id, review, review_date, institution_id, volunteer_id)";
const REPORT_COLUMNS = "report (id, justification, activity_id, volunteer_id)";


const now = new Date();
const tomorrow = new Date(now);
tomorrow.setDate(now.getDate() + 1);
const dayAfterTomorrow = new Date(now);
dayAfterTomorrow.setDate(now.getDate() + 2);
const yesterday = new Date(now);
yesterday.setDate(now.getDate() - 1);
const dayBeforeYesterday = new Date(now);
dayBeforeYesterday.setDate(now.getDate() - 2);
const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 2);
const endOfMonth = new Date(now.getFullYear(), now.getMonth(), 27);


Cypress.Commands.add('deleteAllButArs', () => {
  cy.task('queryDatabase', {
    query: "DELETE FROM ASSESSMENT",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM PARTICIPATION",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM ENROLLMENT_SHIFTS",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM SHIFT",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM ENROLLMENT",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM REPORT",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM ACTIVITY",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "DELETE FROM AUTH_USERS WHERE NOT (username = 'ars')",
    credentials: credentials,
  });
  cy.task('queryDatabase', {
    query: "DELETE FROM USERS WHERE NOT (name = 'ars')",
    credentials: credentials,
  });
  cy.task('queryDatabase', {
    query: "DELETE FROM INSTITUTIONS",
    credentials: credentials,
  });
});

Cypress.Commands.add('createDemoEntities', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + INSTITUTION_COLUMNS + generateInstitutionTuple(1, "DEMO INSTITUTION", "000000000"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(2, "MEMBER", "DEMO-MEMBER", "MEMBER", 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(2, "DEMO", "demo-member", 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(3, "VOLUNTEER", "DEMO-VOLUNTEER", "VOLUNTEER", "NULL"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(3, "DEMO", "demo-volunteer", 3),
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForEnrollments', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(1, "A1", "Enrollment is open", tomorrow.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(1, tomorrow.toISOString(), tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(4, dayAfterTomorrow.toISOString(), dayAfterTomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(5, dayAfterTomorrow.toISOString(), dayAfterTomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  // shift 6: start=tomorrow, end=dayAfterTomorrow (sobrepõe com shift 7)
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(6, dayAfterTomorrow.toISOString(), tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  // shift 7: start=tomorrow, end=dayAfterTomorrow (sobrepõe com shift 6)
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(7, dayAfterTomorrow.toISOString(), tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(2, "A2", "Enrollment is open and it is already enrolled", tomorrow.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(2, tomorrow.toISOString(), tomorrow.toISOString(), 1, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(3, "A3", "Enrollment is closed", yesterday.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(3, tomorrow.toISOString(), tomorrow.toISOString(), 1, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(5, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (5, 2)",
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForReports', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(1, "A1", "Enrollment is open", tomorrow.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(1, tomorrow.toISOString(), tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(2, "A2", "Enrollment is open and it is already enrolled", tomorrow.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(2, tomorrow.toISOString(), tomorrow.toISOString(), 1, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(3, "A3", "Enrollment is closed", yesterday.toISOString(), tomorrow.toISOString(),
      tomorrow.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(3, tomorrow.toISOString(), tomorrow.toISOString(), 1, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(5, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (5, 2)",
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForParticipations', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(4, "VOLUNTEER", "DEMO-VOLUNTEER-2", "VOLUNTEER", "NULL"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(4, "DEMO", "demo-volunteer-2", 4),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(5, "VOLUNTEER", "DEMO-VOLUNTEER-3", "VOLUNTEER", "NULL"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(5, "DEMO", "demo-volunteer-3", 5),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(1, "A1", "Has vacancies", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 2, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(1, yesterday.toISOString(), yesterday.toISOString(), 2, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(2, "A2", "Has no vacancies", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(2, yesterday.toISOString(), yesterday.toISOString(), 1, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(1, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (1, 1)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(2, 4),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (2, 1)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(3, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (3, 2)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(4, 5),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (4, 2)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(5, 2, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(6, 3, 2),
    credentials: credentials,
  })
});

// T2.16: finished activity with one full shift used to validate that new participations cannot exceed the shift capacity.
Cypress.Commands.add('createDatabaseInfoForParticipationShiftCapacityValidation', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(4, "VOLUNTEER", "DEMO-VOLUNTEER-2", "VOLUNTEER", "NULL"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(4, "DEMO", "demo-volunteer-2", 4),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + USER_COLUMNS + generateUserTuple(5, "VOLUNTEER", "DEMO-VOLUNTEER-3", "VOLUNTEER", "NULL"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + AUTH_USERS_COLUMNS + generateAuthUserTuple(5, "DEMO", "demo-volunteer-3", 5),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(
      1,
      "A1",
      "Activity used to validate the shift participants capacity invariant during participation creation",
      dayBeforeYesterday.toISOString(),
      yesterday.toISOString(),
      yesterday.toISOString(),
      2,
      1,
    ),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(
      1,
      yesterday.toISOString(),
      yesterday.toISOString(),
      1,
      1,
    ),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(1, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (1, 1)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(2, 4),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (2, 1)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(1, 1, 1),
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForShifts', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(1, "A1", "Activity for shifts", startOfMonth.toISOString(), startOfMonth.toISOString(),
      endOfMonth.toISOString(), 3, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTupleWithState(2, "A2", "Suspended activity", startOfMonth.toISOString(), startOfMonth.toISOString(),
      endOfMonth.toISOString(), 3, 1, 'SUSPENDED'),
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForAssessments', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + INSTITUTION_COLUMNS + generateInstitutionTuple(2, "DEMO INSTITUTION-2", "000000002"),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(1, "A1", "Same institution is enrolled and participates", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(1, yesterday.toISOString(), yesterday.toISOString(), 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(2, "A2", "Same institution is enrolled and participates", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 2, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(2, yesterday.toISOString(), yesterday.toISOString(), 2, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(3, "A3", "Same institution is enrolled and does not participate", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 3, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(3, yesterday.toISOString(), yesterday.toISOString(), 3, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(4, "A4", "Same institution is not enrolled", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 3, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(4, yesterday.toISOString(), yesterday.toISOString(), 3, 4),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(5, "A5", "Same institution before end date", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      tomorrow.toISOString(), 3, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(5, tomorrow.toISOString(), yesterday.toISOString(), 3, 5),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ACTIVITY_COLUMNS + generateActivityTuple(6, "A6", "Other institution is enrolled and participates", dayBeforeYesterday.toISOString(), yesterday.toISOString(),
      yesterday.toISOString(), 3, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + SHIFT_COLUMNS + generateShiftTuple(6, yesterday.toISOString(), yesterday.toISOString(), 3, 6),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(1, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (1, 1)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(2, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (2, 2)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(3, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (3, 3)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_COLUMNS + generateEnrollmentTuple(4, 3),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ENROLLMENT_SHIFTS_COLUMNS + " VALUES (4, 6)",
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(1, 1, 1),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(2, 2, 2),
    credentials: credentials,
  })
  cy.task('queryDatabase', {
    query: "INSERT INTO " + PARTICIPATION_COLUMNS + generateParticipationTuple(3, 4, 6),
    credentials: credentials,
  })
});

Cypress.Commands.add('createDatabaseInfoForVolunteerAssessments', () => {
  cy.task('queryDatabase', {
    query: "INSERT INTO " + ASSESSMENT_COLUMNS + generateAssessmentTuple(1, "Muito bom!", 2, 3),
    credentials: credentials,
  })
});

function generateAuthUserTuple(id, authType, username, userId) {
  return "VALUES ('"
    + authType + "', '"
    + id + "', 't', 'demo_member@mail.com','"
    + username + "', '"
    + userId + "')"
}

function generateUserTuple(id, userType, name, role, institutionId) {
  return "VALUES ('"
    + userType + "', '"
    + id + "', '2022-02-06 17:58:21.419878', '"
    + name + "', '"
    + role + "', 'ACTIVE', "
    + institutionId + ")";
}

function generateInstitutionTuple(id, name, nif) {
  return "VALUES ('"
    + id + "', 't', 'abca428c09862e89', '2022-08-06 17:58:21.402146','demo_institution@mail.com', '" +
    name + "', '" +
    nif + "', '2024-02-06 17:58:21.402134')";
}

function generateActivityTuple(id, name, description, deadline, start, end, participants, institutionId) {
  return "VALUES ('"
    + id + "', '"
    + deadline +
    "', '2022-08-06 17:58:21.402146', '" +
    description + "', '"
    + end + "', '"
    + name + "', '" +
    participants +
    "', 'Lisbon',  '"
    + start + "', 'APPROVED', " +
    institutionId + ")";
}

function generateActivityTupleWithState(id, name, description, deadline, start, end, participants, institutionId, state) {
  return "VALUES ('"
    + id + "', '"
    + deadline +
    "', '2022-08-06 17:58:21.402146', '" +
    description + "', '"
    + end + "', '"
    + name + "', '" +
    participants +
    "', 'Lisbon',  '"
    + start + "', '" + state + "', " +
    institutionId + ")";
}

function generateEnrollmentTuple(id, volunteerId) {
  return "VALUES ("
    + id + ", '2022-08-06 17:58:21.402146', 'sql-inserted-motivation', "
    + volunteerId + ")";
}

function generateParticipationTuple(id, enrollmentId, shiftId) {
  return "VALUES ("
    + id + ", '2024-02-06 18:51:37.595713', '5', " +
    enrollmentId + ", " +
    shiftId + ")";
}

function generateShiftTuple(id, end_time, start_time, participants, activityId) {
  return "VALUES (" + id + ", '" + end_time + "', 'Lisbon', " + participants + ", '" + start_time + "', " + activityId + ")";
}

function generateAssessmentTuple(id, review, institutionId, volunteerId) {
  return "VALUES (" + id + ", '" + review + "', '2024-02-07 18:51:37.595713', '" + institutionId + "', " + volunteerId + ")";
}