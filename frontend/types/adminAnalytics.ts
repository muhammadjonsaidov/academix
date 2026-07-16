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
  totalStudents: number;
  totalTeachers: number;
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
