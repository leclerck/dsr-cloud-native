package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponseDto {

	private String eventId;
	private String eventName;
	private String eventDate;
	private String venue;
	private String userId;
	private String ticketId;
	private String zone;
	private Double price;
	private String s3Key;
	private String bucket;
}
