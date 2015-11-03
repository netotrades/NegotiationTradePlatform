package trading.common;
 
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWriter {
	private XSSFWorkbook workbook;
	private XSSFSheet sheet;
	static int rowCount = 0;
	Object[] data = new Object[3];

	public ExcelWriter() {
		workbook = new XSSFWorkbook();
		sheet = workbook.createSheet("Results");
		createHeaderRow(sheet);
	}
	
	public void writefile(String report) throws IOException {
		String[] details = report.split(",");
		data[0] = details[0];
		data[1] = details[2].substring(details[1].lastIndexOf(" ") +1);
		data[2] = details[3].substring(11, details[3].length()-49);
		
		

		Row row = sheet.createRow(++rowCount);
		int columnCount = 0;

		for (Object field : data) {
			Cell cell = row.createCell(++columnCount);
			if (field instanceof String) {
				cell.setCellValue((String) field);
			} else if (field instanceof Integer) {
				cell.setCellValue((Integer) field);
			}
		}

		try (FileOutputStream outputStream = new FileOutputStream("Results.xlsx")) {
			workbook.write(outputStream);
		}
	}
	
	private void createHeaderRow(XSSFSheet sheet) {
		 
	    CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
	    XSSFFont font = sheet.getWorkbook().createFont();
	    font.setBold(true);
	    font.setFontHeightInPoints((short) 11);
	    cellStyle.setFont(font);
	 
	    Row row = sheet.createRow(0);
	    Cell cellTitle = row.createCell(1);
	 
	    cellTitle.setCellStyle(cellStyle);
	    cellTitle.setCellValue("Date:Time");
	 
	    Cell cellAuthor = row.createCell(2);
	    cellAuthor.setCellStyle(cellStyle);
	    cellAuthor.setCellValue("Buyer's offer");
	 
	    Cell cellPrice = row.createCell(3);
	    cellPrice.setCellStyle(cellStyle);
	    cellPrice.setCellValue("Seller's offer");
	}
}
