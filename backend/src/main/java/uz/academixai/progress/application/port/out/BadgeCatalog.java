package uz.academixai.progress.application.port.out;

import java.util.List;
import uz.academixai.progress.domain.Badge;

/** Badge definitions available to the Progress policy. */
public interface BadgeCatalog {

  List<Badge> findAll();
}
