export interface Medicamento {
  id: number;
  nombre: string;
  /** false = NO POS: la solicitud exige datos adicionales. */
  esPos: boolean;
}

export interface SolicitudRequest {
  medicamentoId: number;
  numeroOrden?: string;
  direccion?: string;
  telefono?: string;
  correoContacto?: string;
}

/**
 * Resultado de notificar al usuario al radicar. Solo viene en la respuesta del POST.
 * ENVIADA: llegó · FALLIDA: el servicio no pudo entregar · NO_DISPONIBLE: no se pudo contactar al servicio.
 */
export type EstadoNotificacion = 'ENVIADA' | 'FALLIDA' | 'NO_DISPONIBLE';

export interface Solicitud {
  id: number;
  medicamento: Medicamento;
  numeroOrden: string | null;
  direccion: string | null;
  telefono: string | null;
  correoContacto: string | null;
  /** ISO-8601 (UTC). */
  createdAt: string;
  /** Campo opcional: solo al crear. Un cliente que lo ignore sigue funcionando (contrato compatible). */
  notificacion?: EstadoNotificacion;
}

/** Página del listado; {@code page} es base 0. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
