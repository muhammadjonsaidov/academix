package uz.academixai.school.application.port.out;

import java.util.List;
import java.util.Map;

/** Outbound port for reading the uploaded spreadsheet. */
public interface SpreadsheetImport {

  List<String> readHeaders(byte[] fileBytes);

  /** Rows mapped through {@code columnMapping}; a null {@code limit} means every row. */
  List<Map<String, String>> readRows(
      byte[] fileBytes, Map<String, String> columnMapping, Integer limit);
}
