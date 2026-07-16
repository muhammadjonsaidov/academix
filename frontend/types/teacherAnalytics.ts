// academix_tz.md §2.3 "Dashboard" / "O'quvchi progressi" / "Sinf taqqoslash".

export interface TeacherClassProgress {
  classId: string;
  className: string;
  studentCount: number;
  avgScore: number;
  gradedCount: number;
}

export interface TeacherDashboard {
  myClasses: { id: string; fullName: string; studentCount: number }[];
  pendingSubmissions: number;
  gradedToday: number;
  myRating: { score: number; trend: string };
  classProgressSummary: TeacherClassProgress[];
}

export interface TeacherSubjectStat {
  subject: string;
  averageScore: number;
  submissionRate: number;
  trend: string;
  weakAreas: string[];
  strongAreas: string[];
}

export interface TeacherStudentProgress {
  student: { id: string; firstName: string; lastName: string };
  subjectStats: TeacherSubjectStat[];
  xpHistory: { date: string; xp: number; reason: string }[];
  recentSubmissions: {
    submissionId: string;
    assignmentId: string;
    status: string;
    isLate: boolean;
    submittedAt: string;
  }[];
}

export interface TeacherClassAnalytics {
  classAverage: number;
  topStudents: {
    studentId: string;
    firstName: string;
    lastName: string;
    avgScore: number;
    gradedCount: number;
  }[];
  bottomStudents: {
    studentId: string;
    firstName: string;
    lastName: string;
    avgScore: number;
    gradedCount: number;
  }[];
  subjectWeakAreas: string[];
  submissionRateBySubject: TeacherSubjectStat[];
}
