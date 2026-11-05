describe('Peer Evaluation Menu Test', () => {
  beforeEach(() => {
    // Use the custom login command to log in before each test
    cy.login('john', '123456')

    // Visit the dashboard page after logging in
    cy.visit('/home')
  })

  it('Should display submenus when clicking "Peer Evaluation"', () => {
    // Step 1: Find and click the "Peer Evaluation" menu item
    cy.contains('Peer Evaluation')
      .should('be.visible') // Ensure the menu item is visible
      .click() // Click to expand the submenus

    // Step 2: Verify that the submenus "My Evaluations" and "Submit Evaluations" appear
    cy.contains('My Evaluations').should('be.visible') // Ensure "My Evaluations" submenu is visible

    cy.contains('Submit Evaluations').should('be.visible') // Ensure "Submit Evaluations" submenu is visible
  })

  it('Should navigate to "My Evaluations" when submenu is clicked', () => {
    // Step 1: Click the "Peer Evaluation" menu item to reveal the submenus
    cy.contains('Peer Evaluation').click()

    // Step 2: Click "My Evaluations" and verify the navigation to the correct page
    cy.contains('My Evaluations').click()

    // Step 3: Assert the new URL or page content to ensure correct navigation
    cy.url().should('include', '/my-evaluations') // Adjust the URL check as needed
    cy.contains('My Peer Evaluations').should('be.visible') // Adjust the page content check
  })

  it('Should navigate to "Submit Evaluations" when submenu is clicked', () => {
    // Step 1: Click the "Peer Evaluation" menu item to reveal the submenus
    cy.contains('Peer Evaluation').click()

    // Step 2: Click "Submit Evaluations" and verify the navigation to the correct page
    cy.contains('Submit Evaluations').click()

    // Step 3: Assert the new URL or page content to ensure correct navigation
    cy.url().should('include', '/submit-evaluations') // Adjust the URL check as needed
    cy.contains('Submit Peer Evaluations').should('be.visible') // Adjust the page content check
  })
})

// BR-deactivated-not-evaluated (UC-STU-deactivate-student): the submit form leaves a deactivated teammate out of a new
// submission, but an evaluation of her submitted before deactivation stays listed and editable. The API is stubbed;
// EvaluationIntegrationTest covers the server refusing a new evaluation of her.
describe('Peer evaluation form and a deactivated teammate', () => {
  const ok = (data) => ({ flag: true, code: 200, message: 'OK', data })
  const teammates = [
    {
      id: 4,
      firstName: 'John',
      lastName: 'Smith',
      email: 'j.smith@abc.edu',
      username: 'j.smith@abc.edu',
      enabled: true
    },
    {
      id: 5,
      firstName: 'Eric',
      lastName: 'Hudson',
      email: 'e.hudson@abc.edu',
      username: 'e.hudson@abc.edu',
      enabled: false
    },
    {
      id: 6,
      firstName: 'Jerry',
      lastName: 'Moon',
      email: 'j.moon@abc.edu',
      username: 'j.moon@abc.edu',
      enabled: true
    }
  ]
  const criterion = {
    criterionId: 1,
    criterion: 'Quality of work',
    description: 'Quality',
    maxScore: 10
  }

  beforeEach(() => {
    cy.login('j.smith@abc.edu', '123456')
    cy.intercept('GET', '**/students/teams/*', { body: ok(teammates) })
    cy.intercept('GET', '**/sections/*', {
      body: ok({ sectionId: 2, sectionName: '2023-2024', rubricId: 1 })
    })
    cy.intercept('GET', '**/rubrics/*', {
      body: ok({ rubricId: 1, name: 'Rubric', criteria: [criterion] })
    })
  })

  it('gives no row to a deactivated teammate in a new submission', () => {
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', { body: ok([]) }).as('existing')

    cy.visit('/evaluations/submit-evaluations')
    cy.wait('@existing')

    cy.get('.el-table')
      .should('contain', 'John Smith')
      .and('contain', 'Jerry Moon')
      .and('not.contain', 'Eric Hudson')
    cy.get('[data-cy=evaluation-submit]').should('exist')
  })

  it('lists an evaluation of her submitted before deactivation, and Update sends a PUT for it', () => {
    const existing = [4, 5, 6].map((id, i) => ({
      evaluationId: 100 + i,
      week: '2023-W31',
      evaluatorId: 4,
      evaluateeId: id,
      evaluateeName: teammates[i].firstName + ' ' + teammates[i].lastName,
      ratings: [{ ratingId: 200 + i, criterionId: 1, actualScore: 8 }],
      publicComment: '',
      privateComment: ''
    }))
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', { body: ok(existing) }).as('existing')
    cy.intercept('PUT', '**/evaluations/*', (req) => req.reply(ok(req.body))).as('update')

    cy.visit('/evaluations/submit-evaluations')
    cy.wait('@existing')

    cy.get('.el-table').should('contain', 'Eric Hudson')
    cy.get('[data-cy=evaluation-update]').click()

    cy.get('@update.all').should('have.length', 3)
    cy.get('@update.all').then((calls) => {
      expect(calls.map((c) => c.request.url)).to.satisfy((urls) =>
        urls.some((u) => u.endsWith('/evaluations/101'))
      )
    })
  })
})
