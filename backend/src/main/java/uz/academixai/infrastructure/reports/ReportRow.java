package uz.academixai.infrastructure.reports;

/**
 * Plain JavaBean, deliberately NOT a record — {@code JRBeanCollectionDataSource} binds JRXML {@code
 * <field>} names via {@code java.beans.Introspector} reflection, which only recognizes classic
 * {@code getXxx()} accessors. A record's {@code label()}-style accessors don't match that
 * convention and would silently bind every field to null (confirmed via `javap` against the real
 * {@code JRBeanCollectionDataSource} — no record-aware alternative exists in 7.0.7).
 */
public class ReportRow {

  private final String label;
  private final String value1;
  private final String value2;
  private final String value3;

  public ReportRow(String label, String value1, String value2, String value3) {
    this.label = label;
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
  }

  public String getLabel() {
    return label;
  }

  public String getValue1() {
    return value1;
  }

  public String getValue2() {
    return value2;
  }

  public String getValue3() {
    return value3;
  }
}
