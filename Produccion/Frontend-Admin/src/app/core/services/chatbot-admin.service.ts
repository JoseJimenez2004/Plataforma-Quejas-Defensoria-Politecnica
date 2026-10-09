import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PreguntaChatbot, PreguntaChatbotRequest } from '../models/admin.models';

/** CRUD de preguntas del chatbot (chatbot-service, /api/chatbot/admin). */
@Injectable({ providedIn: 'root' })
export class ChatbotAdminService {
  private readonly apiUrl = '/api/chatbot/admin/preguntas';

  constructor(private http: HttpClient) {}

  listar(): Observable<PreguntaChatbot[]> {
    return this.http.get<PreguntaChatbot[]>(this.apiUrl);
  }

  crear(datos: PreguntaChatbotRequest): Observable<PreguntaChatbot> {
    return this.http.post<PreguntaChatbot>(this.apiUrl, datos);
  }

  editar(id: number, datos: PreguntaChatbotRequest): Observable<PreguntaChatbot> {
    return this.http.put<PreguntaChatbot>(`${this.apiUrl}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
