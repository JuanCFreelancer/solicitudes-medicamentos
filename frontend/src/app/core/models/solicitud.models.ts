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

export interface Solicitud {
  id: number;
  medicamento: Medicamento;
  numeroOrden: string | null;
  direccion: string | null;
  telefono: string | null;
  correoContacto: string | null;
  /** ISO-8601 (UTC). */
  createdAt: string;
}

/** Página del listado; {@code page} es base 0. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
