package uz.academixai.wellbeing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.WatchlistEntry;
import uz.academixai.infrastructure.persistence.PsychologicalSignalEntity;
import uz.academixai.infrastructure.persistence.PsychologicalSignalRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.WatchlistEntryEntity;
import uz.academixai.infrastructure.persistence.WatchlistEntryRepository;
import uz.academixai.wellbeing.application.port.out.PsychologistWorkspaceStore;

/**
 * JPA adapter that applies school ownership through student profiles for psychologist operations.
 */
@Repository
public class JpaPsychologistWorkspaceStore implements PsychologistWorkspaceStore {

  private final PsychologicalSignalRepository signals;
  private final StudentProfileRepository students;
  private final WatchlistEntryRepository watchlist;

  public JpaPsychologistWorkspaceStore(
      PsychologicalSignalRepository signals,
      StudentProfileRepository students,
      WatchlistEntryRepository watchlist) {
    this.signals = signals;
    this.students = students;
    this.watchlist = watchlist;
  }

  @Override
  public List<PsychologicalSignal> listSignals(UUID schoolId, SignalSeverity severity) {
    List<UUID> studentIds = studentIds(schoolId);
    if (studentIds.isEmpty()) {
      return List.of();
    }
    return (severity == null
            ? signals.findByStudentIdInOrderByDetectedAtDesc(studentIds)
            : signals.findByStudentIdInAndSeverityOrderByDetectedAtDesc(studentIds, severity))
        .stream().map(PsychologicalSignalEntity::toDomain).toList();
  }

  @Override
  public Optional<PsychologicalSignal> findSignal(UUID schoolId, UUID signalId) {
    List<UUID> studentIds = studentIds(schoolId);
    if (studentIds.isEmpty()) {
      return Optional.empty();
    }
    return signals
        .findByIdAndStudentIdIn(signalId, studentIds)
        .map(PsychologicalSignalEntity::toDomain);
  }

  @Override
  public PsychologicalSignal saveSignal(PsychologicalSignal signal) {
    return signals.save(PsychologicalSignalEntity.fromDomain(signal)).toDomain();
  }

  @Override
  public boolean studentExists(UUID schoolId, UUID studentId) {
    return students.findByUserIdAndSchoolId(studentId, schoolId).isPresent();
  }

  @Override
  public List<WatchlistEntry> listWatchlist(UUID schoolId) {
    List<UUID> studentIds = studentIds(schoolId);
    return watchlist.findAllByOrderByAddedAtDesc().stream()
        .map(WatchlistEntryEntity::toDomain)
        .filter(entry -> studentIds.contains(entry.studentId()))
        .toList();
  }

  @Override
  public Optional<WatchlistEntry> findWatchlistEntry(UUID studentId) {
    return watchlist.findByStudentId(studentId).map(WatchlistEntryEntity::toDomain);
  }

  @Override
  public WatchlistEntry saveWatchlistEntry(WatchlistEntry entry) {
    return watchlist.save(WatchlistEntryEntity.fromDomain(entry)).toDomain();
  }

  @Override
  public void deleteWatchlistEntry(UUID studentId) {
    watchlist.deleteByStudentId(studentId);
  }

  private List<UUID> studentIds(UUID schoolId) {
    return students.findBySchoolId(schoolId).stream().map(StudentProfileEntity::getUserId).toList();
  }
}
