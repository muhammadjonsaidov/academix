package uz.academixai.reporting.infrastructure.pdf;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Compiles {@code reports/quarter-report.jrxml} once at startup (compilation is the expensive step
 * — filling/exporting is cheap per call) and reuses the compiled {@link JasperReport} for every
 * generated PDF. See {@code quarter-report.jrxml}'s own comment for why one generic template covers
 * all 3 report types (SCHOOL/CLASS/STUDENT).
 */
@Component
public class JasperReportGenerator {

  private JasperReport compiledReport;

  @PostConstruct
  void compileTemplate() {
    try (InputStream jrxml =
        new ClassPathResource("reports/quarter-report.jrxml").getInputStream()) {
      compiledReport = JasperCompileManager.compileReport(jrxml);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read quarter-report.jrxml", e);
    } catch (JRException e) {
      throw new IllegalStateException("Failed to compile quarter-report.jrxml", e);
    }
  }

  public byte[] generatePdf(
      String title,
      String subtitle,
      String generatedAt,
      String col1Header,
      String col2Header,
      String col3Header,
      List<ReportRow> rows) {
    Map<String, Object> params = new HashMap<>();
    params.put("REPORT_TITLE", title);
    params.put("REPORT_SUBTITLE", subtitle);
    params.put("GENERATED_AT", generatedAt);
    params.put("COL1_HEADER", col1Header);
    params.put("COL2_HEADER", col2Header);
    params.put("COL3_HEADER", col3Header);

    try {
      JasperPrint print =
          JasperFillManager.fillReport(
              compiledReport, params, new JRBeanCollectionDataSource(rows));
      return JasperExportManager.exportReportToPdf(print);
    } catch (JRException e) {
      throw new IllegalStateException("Failed to generate report PDF", e);
    }
  }
}
