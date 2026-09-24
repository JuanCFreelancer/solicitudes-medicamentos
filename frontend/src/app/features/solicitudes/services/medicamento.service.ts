import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Medicamento } from '../../../core/models/solicitud.models';

@Injectable({ providedIn: 'root' })
export class MedicamentoService {
  private readonly http = inject(HttpClient);

  list(): Observable<Medicamento[]> {
    return this.http.get<Medicamento[]>(`${environment.solicitudesApiUrl}/medicamentos`);
  }
}
