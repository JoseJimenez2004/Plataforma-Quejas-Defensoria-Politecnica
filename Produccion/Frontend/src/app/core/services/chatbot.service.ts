import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CategoriaChatbot } from '../models/chatbot.models';

/** Consume el chatbot-service (público, sin login) que alimenta el widget de mini-chat/
 * tutorial del portal. Ver Backend/chatbot-service. */
@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly apiUrl = '/api/chatbot';

  constructor(private http: HttpClient) {}

  obtenerMenu(): Observable<CategoriaChatbot[]> {
    return this.http.get<CategoriaChatbot[]>(`${this.apiUrl}/menu`);
  }
}
