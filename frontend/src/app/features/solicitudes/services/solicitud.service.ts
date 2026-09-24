import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page, Solicitud, SolicitudRequest } from '../../../core/models/solicitud.models';

@Injectable({ providedIn: 'root' })
export class SolicitudService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.solicitudesApiUrl}/solicitudes`;

  create(request: SolicitudRequest): Observable<Solicitud> {
    return this.http.post<Solicitud>(this.baseUrl, request);
  }

  /** @param page número de página base 0 */
  list(page: number, size: number): Observable<Page<Solicitud>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Solicitud>>(this.baseUrl, { params });
  }
}
