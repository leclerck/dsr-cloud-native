package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.TicketRequestDto;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidTicketException;

@Service
public class PdfTicketService {

	public byte[] generateTicketPdf(TicketRequestDto ticket) {

		try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
			PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				float y = 750;

				contentStream.beginText();
				contentStream.setFont(titleFont, 20);
				contentStream.newLineAtOffset(50, y);
				contentStream.showText("E-Ticket");
				contentStream.endText();

				y -= 50;
				writeLine(contentStream, bodyFont, 14, 50, y, "Event: " + safe(ticket.getEventName()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y, "Date: " + safe(ticket.getEventDate()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y, "Venue: " + safe(ticket.getVenue()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y, "User ID: " + safe(ticket.getUserId()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y, "Ticket ID: " + safe(ticket.getTicketId()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y, "Zone: " + safe(ticket.getZone()));
				y -= 25;
				writeLine(contentStream, bodyFont, 14, 50, y,
						"Price: " + (ticket.getPrice() != null ? ticket.getPrice().toString() : ""));
			}

			document.save(outputStream);
			return outputStream.toByteArray();

		} catch (IOException e) {
			throw new InvalidTicketException("Error al generar el PDF del ticket: " + e.getMessage());
		}
	}

	private void writeLine(PDPageContentStream contentStream, PDType1Font font, float fontSize, float x, float y,
			String text) throws IOException {

		contentStream.beginText();
		contentStream.setFont(font, fontSize);
		contentStream.newLineAtOffset(x, y);
		contentStream.showText(text);
		contentStream.endText();
	}

	private String safe(String value) {
		if (value == null) {
			return "";
		}
		// PDF Type1 fonts do not support all unicode characters
		return value.replaceAll("[^\\x20-\\x7E]", "?");
	}
}
