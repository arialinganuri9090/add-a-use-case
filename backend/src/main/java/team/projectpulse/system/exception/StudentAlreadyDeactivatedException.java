package team.projectpulse.system.exception;

/**
 * A deactivation named a student who is already deactivated (UC-STU-deactivate-student, extension 3a), including one
 * another user deactivated after the caller's view of her was loaded. Mapped to HTTP 409.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("The student is already deactivated.");
    }

}
