package cl.duoc.ejemplo.ms.administracion.archivos.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.duoc.ejemplo.ms.administracion.archivos.dto.EventStatsDto;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.TicketRequestDto;
import cl.duoc.ejemplo.ms.administracion.archivos.dto.TicketResponseDto;
import cl.duoc.ejemplo.ms.administracion.archivos.service.TicketService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

	private final TicketService ticketService;

	@PostMapping
	public ResponseEntity<TicketResponseDto> generateTicket(@RequestBody TicketRequestDto request) {

		TicketResponseDto response = ticketService.generateAndStore(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/upload")
	public ResponseEntity<TicketResponseDto> uploadTicket(@RequestParam String eventId, @RequestParam String userId,
			@RequestParam String ticketId, @RequestParam("file") MultipartFile file) {

		TicketResponseDto response = ticketService.uploadPdf(eventId, userId, ticketId, file);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{eventId}/{userId}/{ticketId}")
	public ResponseEntity<byte[]> downloadTicket(@PathVariable String eventId, @PathVariable String userId,
			@PathVariable String ticketId) {

		byte[] pdf = ticketService.downloadTicket(eventId, userId, ticketId);
		String filename = ticketId.endsWith(".pdf") ? ticketId : ticketId + ".pdf";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.APPLICATION_PDF).body(pdf);
	}

	@GetMapping("/event/{eventId}")
	public ResponseEntity<byte[]> downloadByEvent(@PathVariable String eventId) {

		byte[] zip = ticketService.downloadByEvent(eventId);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + eventId + "-tickets.zip\"")
				.contentType(MediaType.parseMediaType("application/zip")).body(zip);
	}

	@GetMapping("/event/{eventId}/user/{userId}")
	public ResponseEntity<byte[]> downloadByUser(@PathVariable String eventId, @PathVariable String userId) {

		byte[] zip = ticketService.downloadByUser(eventId, userId);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + eventId + "-" + userId + "-tickets.zip\"")
				.contentType(MediaType.parseMediaType("application/zip")).body(zip);
	}

	@PutMapping
	public ResponseEntity<TicketResponseDto> updateTicket(@RequestBody TicketRequestDto request) {

		TicketResponseDto response = ticketService.updateTicket(request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{eventId}/{userId}/{ticketId}")
	public ResponseEntity<Void> deleteTicket(@PathVariable String eventId, @PathVariable String userId,
			@PathVariable String ticketId) {

		ticketService.deleteTicket(eventId, userId, ticketId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/stats")
	public ResponseEntity<List<EventStatsDto>> statsByEvent() {
		return ResponseEntity.ok(ticketService.statsByEvent());
	}

	@GetMapping("/stats/{eventId}")
	public ResponseEntity<EventStatsDto> statsForEvent(@PathVariable String eventId) {
		return ResponseEntity.ok(ticketService.statsForEvent(eventId));
	}
}
