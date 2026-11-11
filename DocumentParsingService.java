package cn.edu.sdua.jiankang.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文档解析模块 —— 使用 Apache POI(Word/Excel)与 PDFBox(PDF)提取纯文本
 */
@Service
public class DocumentParsingService {

    /** 根据文件后缀解析出纯文本内容 */
    public String extractText(MultipartFile file) {
        String name = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase();
        try {
            byte[] bytes = file.getBytes();
            if (name.endsWith(".pdf")) {
                return parsePdf(bytes);
            } else if (name.endsWith(".docx")) {
                return parseDocx(bytes);
            } else if (name.endsWith(".doc")) {
                return parseDoc(bytes);
            } else if (name.endsWith(".xlsx")) {
                return parseExcel(bytes, true);
            } else if (name.endsWith(".xls")) {
                return parseExcel(bytes, false);
            } else if (name.endsWith(".txt") || name.endsWith(".md") || name.isEmpty()) {
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("暂不支持的文件类型: " + name);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    public String resolveFileType(String fileName) {
        if (fileName == null) return "unknown";
        String n = fileName.toLowerCase();
        int dot = n.lastIndexOf('.');
        return dot >= 0 ? n.substring(dot + 1) : "unknown";
    }

    private String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return normalize(stripper.getText(doc));
        }
    }

    private String parseDocx(byte[] bytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(bytes);
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return normalize(extractor.getText());
        }
    }

    private String parseDoc(byte[] bytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(bytes);
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            return normalize(extractor.getText());
        }
    }

    private String parseExcel(byte[] bytes, boolean xssf) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = new ByteArrayInputStream(bytes);
             Workbook wb = xssf ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            for (Sheet sheet : wb) {
                sb.append("### ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        line.append(getCellText(cell)).append("\t");
                    }
                    sb.append(line).append("\n");
                }
            }
        }
        return normalize(sb.toString());
    }

    private String getCellText(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
