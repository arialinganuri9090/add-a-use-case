// UC-STU-deactivate-student: the Deactivate action in the edit-student dialog. The student API is stubbed, so these tests
// check what the dialog shows and what it sends; the backend's own tests cover who may deactivate and the 409 for 3a.
// Run against the dev stack (backend + `npm run dev`) with `npm run cy:open`.

const john = {
  id: 4,
  username: 'j.smith@abc.edu',
  enabled: true,
  roles: 'student',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  sectionId: 2,
  sectionName: '2023-2024',
  teamId: 1,
  teamName: 'Team1'
}

const result = (data, message = 'OK') => ({ flag: true, code: 200, message, data })

const page = (content) =>
  result({ content, totalElements: content.length, size: 50, number: 0 }, 'Find students successfully')

describe('Deactivate a student', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', { body: page([john]) }).as('search')
    cy.intercept('GET', '**/students/4', { body: result(john, 'Find student successfully') }).as('student')
    cy.intercept('PATCH', '**/students/4/deactivate', {
      body: result({ ...john, enabled: false }, 'Deactivate student successfully')
    }).as('deactivate')
  })

  function openEditDialog() {
    cy.visit('/students')
    cy.wait('@search')
    cy.get('[data-cy=edit-student]').first().click()
    cy.wait('@student')
    cy.get('[data-cy=student-status]').should('contain', 'Active')
  }

  it('states the consequences and asks for confirmation (steps 3-4)', () => {
    openEditDialog()

    cy.get('[data-cy=deactivate-student]').click()

    cy.get('[data-cy=deactivate-consequences]')
      .should('contain', 'John Smith')
      .and('contain', 'can no longer log in')
      .and('contain', 'previously submitted work are kept')
      .and('contain', 'no longer asked to evaluate')
      .and('contain', 'not a deletion')
    cy.get('.deactivate-confirm').should('contain', 'Deactivate')
    cy.get('.deactivate-cancel').should('contain', 'Cancel')
  })

  it('sends no request when the user cancels (5a)', () => {
    openEditDialog()

    cy.get('[data-cy=deactivate-student]').click()
    cy.get('.deactivate-cancel').click()

    cy.get('[data-cy=student-status]').should('contain', 'Active')
    cy.get('@deactivate.all').should('have.length', 0)
  })

  it('sends exactly one request on confirm and reports the deactivation (steps 5-6)', () => {
    openEditDialog()

    cy.get('[data-cy=deactivate-student]').click()
    // From here the student reloads as deactivated
    cy.intercept('GET', '**/students/4', { body: result({ ...john, enabled: false }) }).as('reloaded')
    cy.get('.deactivate-confirm').click()

    cy.wait('@deactivate')
    cy.contains('The student has been deactivated.')
    cy.wait('@reloaded')
    cy.get('[data-cy=deactivate-student]').should('not.exist')
    cy.get('[data-cy=student-status-switch]').should('exist')
    cy.get('@deactivate.all').should('have.length', 1)
  })

  it('shows the server message and reloads when the student was already deactivated (3a)', () => {
    cy.intercept('PATCH', '**/students/4/deactivate', {
      statusCode: 409,
      body: { flag: false, code: 409, message: 'The student is already deactivated.' }
    }).as('conflict')
    openEditDialog()

    cy.get('[data-cy=deactivate-student]').click()
    cy.intercept('GET', '**/students/4', { body: result({ ...john, enabled: false }) }).as('reloaded')
    cy.get('.deactivate-confirm').click()

    cy.wait('@conflict')
    cy.contains('The student is already deactivated.')
    cy.wait('@reloaded')
    cy.get('[data-cy=deactivate-student]').should('not.exist')
  })
})
