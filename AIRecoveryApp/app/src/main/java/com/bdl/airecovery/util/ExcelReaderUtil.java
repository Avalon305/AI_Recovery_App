package com.bdl.airecovery.util;

import android.util.Log;

import com.bdl.airecovery.entity.SegmentCalibration;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelToList
 */
public class ExcelReaderUtil {

    private static final String XLS = "xls";

    /**
     * 根据文件后缀名类型获取对应的工作簿对象
     * @param inputStream 读取文件的输入流
     * @param fileType 文件后缀名类型(仅支持xls）
     * @return 包含文件数据的工作簿对象
     * @throws IOException
     */
    private static Workbook getWorkbook(InputStream inputStream, String fileType) throws IOException {
        Workbook workbook = null;
        if (fileType.equalsIgnoreCase(XLS)) {
            workbook = new HSSFWorkbook(inputStream);
        }
        return workbook;
    }

    public static List<SegmentCalibration> readExcel(String filePath) {
        Workbook workbook = null;
        FileInputStream excelFile = null;
        try {
            // 获取Excel后缀名
            String fileType = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());
            // 获取Excel文件
            excelFile = new FileInputStream(filePath);
            // 获取工作簿
            workbook = getWorkbook(excelFile, fileType);
            // 获取Excel数据
            List<SegmentCalibration> datas = parseExcel(workbook);
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (null != workbook) workbook.close();
                if (null != excelFile) excelFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static List<SegmentCalibration> parseExcel(Workbook workbook) {
        List<SegmentCalibration> datas = new ArrayList<>();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);

            if (null == sheet) continue;

            int headerRowNum = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(headerRowNum);
            if (null == headerRow) {
                Log.e("parseExcel", "表头数据未找到");
            }

            int rowStart = headerRowNum + 1;
            int rowEnd = sheet.getPhysicalNumberOfRows();
            for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (null == row) continue;

                SegmentCalibration data = convertRowToData(row);

                if (null == data) {
                    Log.e("converRowToData", rowNum + "行数据不合法，已忽略");
                    continue;
                }
                datas.add(data);
            }
        }

        return datas;

    }

    public static SegmentCalibration convertRowToData(Row row) {
        SegmentCalibration sc = new SegmentCalibration();
        Cell cell;
        int cellNum = 0;
        cell = row.getCell(cellNum++);
        sc.setId((int) cell.getNumericCellValue());

        cell = row.getCell(cellNum++);
        sc.setForce((int) cell.getNumericCellValue());

        cell = row.getCell(cellNum++);
        sc.setSegmentPosition((int) cell.getNumericCellValue());

        cell = row.getCell(cellNum++);
        sc.setGoingTorque((int) cell.getNumericCellValue());

        cell = row.getCell(cellNum++);
        sc.setReturnTorque((int) (cell.getNumericCellValue()));

        cell = row.getCell(cellNum++);
        sc.setGoingSpeed((int) (cell.getNumericCellValue()));

        cell = row.getCell(cellNum++);
        sc.setReturnSpeed((int) (cell.getNumericCellValue()));

        cell = row.getCell(cellNum++);
        sc.setBounce((int) (cell.getNumericCellValue()));

        cell = row.getCell(cellNum++);
        sc.setPullThresholdVal((int) (cell.getNumericCellValue()));

        return sc;
    }



}
