package cl.duoc.ejemplo.ms.administracion.archivos.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.EventStatsDto;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.S3ObjectDto;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.TicketRequestDto;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.TicketResponseDto;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidFileException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.InvalidTicketException;
import cl.duoc.ejemplo.ms.administracion.archivos.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

	private static final String PDF_CONTENT_TYPE = "application/pdf";

	private final PdfTicketService pdfTicketService;
	private final EfsService efsService;
	private final AwsS3Service awsS3Service;

	@Value("${aws.s3.bucket}")
	private String bucket;

	public String buildKey(String eventId, String userId, String ticketId) {
		String normalizedTicketId = ticketId.endsWith(".pdf") ? ticketId : ticketId + ".pdf";
		return eventId + "/" + userId + "/" + normalizedTicketId;
	}

	public TicketResponseDto generateAndStore(TicketRequestDto request) {

		validateTicketRequest(request);

		String key = buildKey(request.getEventId(), request.getUserId(), request.getTicketId());
		byte[] pdfBytes = pdfTicketService.generateTicketPdf(request);

		dualWrite(key, pdfBytes);

		return toResponse(key, request);
	}

	public TicketResponseDto uploadPdf(String eventId, String userId, String ticketId, MultipartFile file) {

		if (isBlank(eventId) || isBlank(userId) || isBlank(ticketId)) {
			throw new InvalidTicketException("eventId, userId y ticketId son obligatorios");
		}

		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("El archivo PDF está vacío o es nulo");
		}

		String key = buildKey(eventId, userId, ticketId);

		try {
			efsService.saveToEfs(key, file);
			awsS3Service.upload(bucket, key, file);
		} catch (IOException e) {
			throw new S3UploadException("Error al guardar el ticket en EFS: " + e.getMessage(), e);
		}

		return TicketResponseDto.builder().eventId(eventId).userId(userId).ticketId(ticketId).s3Key(key).bucket(bucket)
				.build();
	}

	public byte[] downloadTicket(String eventId, String userId, String ticketId) {
		String key = buildKey(eventId, userId, ticketId);
		return awsS3Service.downloadAsBytes(bucket, key);
	}

	public byte[] downloadByEvent(String eventId) {
		return zipObjects(eventId + "/");
	}

	public byte[] downloadByUser(String eventId, String userId) {
		return zipObjects(eventId + "/" + userId + "/");
	}

	public TicketResponseDto updateTicket(TicketRequestDto request) {

		validateTicketRequest(request);

		String key = buildKey(request.getEventId(), request.getUserId(), request.getTicketId());
		byte[] pdfBytes = pdfTicketService.generateTicketPdf(request);

		dualWrite(key, pdfBytes);

		return toResponse(key, request);
	}

	public void deleteTicket(String eventId, String userId, String ticketId) {

		String key = buildKey(eventId, userId, ticketId);
		awsS3Service.deleteObject(bucket, key);

		try {
			efsService.delete(key);
		} catch (IOException e) {
			log.warn("No se pudo eliminar el ticket de EFS ({}): {}", key, e.getMessage());
		}
	}

	public List<EventStatsDto> statsByEvent() {

		List<S3ObjectDto> objects = awsS3Service.listObjects(bucket);
		Map<String, Long> counts = new LinkedHashMap<>();

		for (S3ObjectDto object : objects) {
			String key = object.getKey();
			if (key == null || !key.endsWith(".pdf") || !key.contains("/")) {
				continue;
			}
			String eventId = key.substring(0, key.indexOf('/'));
			counts.merge(eventId, 1L, Long::sum);
		}

		List<EventStatsDto> stats = new ArrayList<>();
		counts.forEach((eventId, count) -> stats.add(new EventStatsDto(eventId, count)));
		return stats;
	}

	public EventStatsDto statsForEvent(String eventId) {

		List<S3ObjectDto> objects = awsS3Service.listObjectsByPrefix(bucket, eventId + "/");
		long count = objects.stream().filter(obj -> obj.getKey() != null && obj.getKey().endsWith(".pdf")).count();
		return new EventStatsDto(eventId, count);
	}

	private void dualWrite(String key, byte[] pdfBytes) {
		try {
			efsService.saveBytes(key, pdfBytes);
			awsS3Service.uploadBytes(bucket, key, pdfBytes, PDF_CONTENT_TYPE);
		} catch (IOException e) {
			throw new S3UploadException("Error al guardar el ticket en EFS: " + e.getMessage(), e);
		}
	}

	private byte[] zipObjects(String prefix) {

		List<S3ObjectDto> objects = awsS3Service.listObjectsByPrefix(bucket, prefix);
		List<S3ObjectDto> pdfObjects = objects.stream()
				.filter(obj -> obj.getKey() != null && obj.getKey().endsWith(".pdf")).toList();

		if (pdfObjects.isEmpty()) {
			throw new InvalidTicketException("No se encontraron tickets para el prefijo: " + prefix);
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ZipOutputStream zipOut = new ZipOutputStream(baos)) {

			for (S3ObjectDto object : pdfObjects) {
				byte[] content = awsS3Service.downloadAsBytes(bucket, object.getKey());
				ZipEntry entry = new ZipEntry(object.getKey());
				zipOut.putNextEntry(entry);
				zipOut.write(content);
				zipOut.closeEntry();
			}

			zipOut.finish();
			return baos.toByteArray();

		} catch (IOException e) {
			throw new S3UploadException("Error al generar el ZIP de tickets: " + e.getMessage(), e);
		}
	}

	private void validateTicketRequest(TicketRequestDto request) {

		if (request == null) {
			throw new InvalidTicketException("El cuerpo del ticket es obligatorio");
		}
		if (isBlank(request.getEventId())) {
			throw new InvalidTicketException("eventId es obligatorio");
		}
		if (isBlank(request.getEventName())) {
			throw new InvalidTicketException("eventName es obligatorio");
		}
		if (isBlank(request.getEventDate())) {
			throw new InvalidTicketException("eventDate es obligatorio");
		}
		if (isBlank(request.getVenue())) {
			throw new InvalidTicketException("venue es obligatorio");
		}
		if (isBlank(request.getUserId())) {
			throw new InvalidTicketException("userId es obligatorio");
		}
		if (isBlank(request.getTicketId())) {
			throw new InvalidTicketException("ticketId es obligatorio");
		}
		if (isBlank(request.getZone())) {
			throw new InvalidTicketException("zone es obligatorio");
		}
		if (request.getPrice() == null) {
			throw new InvalidTicketException("price es obligatorio");
		}
	}

	private TicketResponseDto toResponse(String key, TicketRequestDto request) {
		return TicketResponseDto.builder().eventId(request.getEventId()).eventName(request.getEventName())
				.eventDate(request.getEventDate()).venue(request.getVenue()).userId(request.getUserId())
				.ticketId(request.getTicketId()).zone(request.getZone()).price(request.getPrice()).s3Key(key)
				.bucket(bucket).build();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
