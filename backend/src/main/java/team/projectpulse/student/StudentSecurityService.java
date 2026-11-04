package team.projectpulse.student;

import team.projectpulse.instructor.Instructor;
import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers the questions a route asks about one named student: is that student the caller herself
 * ({@code isStudentSelf}), is the caller an instructor of that student's course section
 * ({@code isCurrentUserInstructorOfStudentSection}), and is the caller the course admin of that student's course
 * ({@code isCurrentUserAdminOfStudentCourse}).
 *
 * <p>Every check fails closed: an id that names no row, or a link not yet established, is denied rather than
 * throwing. These methods run inside an {@code AuthorizationManager}, upstream of the {@code DispatcherServlet},
 * so an exception thrown here escapes the filter chain as a 500 that also tells the caller which ids exist.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * Whether the caller is the course admin of the course the named student's course section belongs to
     * (BR-student-lifecycle, BR-section-scoped-access). The course admin need not be assigned to that course section.
     *
     * @param studentId the id of the student being accessed, not the caller's id
     * @return {@code false} for an id that names no student, or a course with no course admin
     */
    public boolean isCurrentUserAdminOfStudentCourse(Integer studentId) {
        Integer callerId = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null || callerId == null) {
            return false;
        }
        Instructor courseAdmin = student.getSection().getCourse().getCourseAdmin();
        return courseAdmin != null && callerId.equals(courseAdmin.getId());
    }

}
