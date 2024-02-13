package io.quarkus.qe;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

public class PDFUtils {

    private static final Logger LOGGER = Logger.getLogger(PDFUtils.class);

    private PDFUtils(){}

    public static void writeToPDF(List<Extension> extensions, String outputFileName) throws IOException {

        try (PdfWriter writer = new PdfWriter(outputFileName);
             PdfDocument pdfDocument = new PdfDocument(writer);
             Document document = new Document(pdfDocument)) {


            Table table = new Table(4);
            table.setWidth(100);

            // Name header cells
            table.addCell(createCell("Number", true));
            table.addCell(createCell("Name", true));
            table.addCell(createCell("Build with Quarkus", true));
            table.addCell(createCell("Artifact", true));


            int index = 1;
            for (Extension extension : extensions) {
                table.addCell(createCell(Integer.toString(index), false));
                table.addCell(createCell(extension.getName(), false));
                table.addCell(createCell(extension.getMetadata().getBuiltWithQuarkusCore(), false));
                table.addCell(createCell(extension.getArtifact(), false));
                index++;
            }

            // Add table to PDF document
            document.add(table);
            LOGGER.info(outputFileName + " file was written successfully!");
        }
    }

    private static Cell createCell(String content, boolean isHeaderCell) {
        Cell cell = new Cell();
        Paragraph paragraph = new Paragraph(content);
        if (isHeaderCell) {
            paragraph.setBold();
        }
        cell.add(paragraph);
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }
}
