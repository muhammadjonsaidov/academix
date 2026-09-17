// Hand-written, mirrors academix_tz.md §2.2 "Dashboard" / "Taqqoslash" sections.

export interface ClassProgress {
  classId: string;
  className: string;
  studentCount: number;
  avgScore: number;
  gradedCount: number;
}

export interface TeacherRanking {
  teacherId: string;
  firstName: string;
  lastName: string;
  avgGrade: number;
  gradedCount: number;
}

export interface PsychAlerts {
  high: number;
  medium: number;
}

export interface AdminDashboard {
  totalClasses: number;
  totalStudents: number;
  totalTeachers: number;
  totalSubjects: number;
  totalAssignments: number;
  activeToday: number;
  homeworkSubmissionRate: number;
  classProgressList: ClassProgress[];
  teacherRankings: TeacherRanking[];
  psychologicalAlerts: PsychAlerts;
}

export interface PeriodProgress {
  periodStart: string;
  avgScore: number;
  gradedCount: number;
}

// academix_tz.md §8 "Admin dashboard cost showback" — grading calls only (HOMEWORK/EXAM).
export interface AiUsage {
  byClass: { classId: string; className: string; callCount: number }[];
  bySubject: { subjectId: string; subjectName: string; callCount: number }[];
  byTeacher: { teacherId: string; firstName: string; lastName: string; callCount: number }[];
}
