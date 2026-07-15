package uz.academixai.infrastructure.importing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Reads the first sheet of an uploaded .xlsx roster (academix_tz.md §2.2 bulk-import). Only .xlsx
 * is supported (matches the spec's own {@code students.xlsx} example) — no legacy .xls (poi, not
 * poi-ooxml) dependency is pulled in for that reason.
 */
@Component
public class ExcelImportParser {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Row 0 headers, trimmed, stopping at the first fully blank cell. */
  public List<String> readHeaders(byte[] fileBytes) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        return List.of();
      }
      List<String> headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        String value = cellValueAsString(cell);
        if (value.isBlank()) {
          break;
        }
        headers.add(value);
      }
      return headers;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Applies {@code columnMapping} (target field -&gt; source header, e.g. {@code {"firstName":
   * "Ism"}}) to every data row (row 1+), returning target-field-keyed raw string values. {@code
   * limit} caps the number of rows returned (5 for the analyze preview); {@code null} reads all.
   */
  public List<Map<String, String>> readRows(
      byte[] fileBytes, Map<String, String> columnMapping, Integer limit) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        return List.of();
      }

      Map<String, Integer> columnIndexByHeader = new LinkedHashMap<>();
      for (Cell cell : headerRow) {
        String header = cellValueAsString(cell);
        if (header.isBlank()) {
          break;
        }
        columnIndexByHeader.put(header, cell.getColumnIndex());
      }

      List<Map<String, String>> rows = new ArrayList<>();
      int lastRow = sheet.getLastRowNum();
      for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
        if (limit != null && rows.size() >= limit) {
          break;
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null || isBlankRow(row)) {
          continue;
        }
        Map<String, String> mappedRow = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : columnMapping.entrySet()) {
          String targetField = entry.getKey();
          Integer columnIndex = columnIndexByHeader.get(entry.getValue());
          String value = columnIndex == null ? "" : cellValueAsString(row.getCell(columnIndex));
          mappedRow.put(targetField, value);
        }
        rows.add(mappedRow);
      }
      return rows;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean isBlankRow(Row row) {
    for (Cell cell : row) {
      if (!cellValueAsString(cell).isBlank()) {
        return false;
      }
    }
    return true;
  }

  private String cellValueAsString(Cell cell) {
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.NUMERIC) {
      if (DateUtil.isCellDateFormatted(cell)) {
        return cell.getLocalDateTimeCellValue().toLocalDate().format(ISO_DATE);
      }
      double numeric = cell.getNumericCellValue();
      return numeric == Math.floor(numeric)
          ? String.valueOf((long) numeric)
          : String.valueOf(numeric);
    }
    return cell.toString().trim();
  }
}
