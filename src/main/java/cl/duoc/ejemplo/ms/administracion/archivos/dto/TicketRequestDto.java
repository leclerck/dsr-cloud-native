package cl.duoc.ejemplo.ms.administracion.archivos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketRequestDto {

	private String eventId;
	private String eventName;
	private String eventDate;
	private String venue;
	private String userId;
	private String ticketId;
	private String zone;
	private Double price;
}
