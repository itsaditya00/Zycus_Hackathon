import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HackathonService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // 1. POST /orders
  createOrder(order: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/orders`, order);
  }

  // 2. GET /orders?status=ASSIGNED
  getOrdersByStatus(status: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/orders?status=${status}`);
  }

  // 3. PUT /suggestions/strategy
  updateStrategy(strategy: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/suggestions/strategy`, { strategy });
  }

  // 4. PATCH /agents/:id/status
  updateAgentStatus(agentId: string, status: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/agents/${agentId}/status`, { status });
  }

  // 5. POST /orders/:id/suggest
  triggerSuggestion(orderId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/orders/${orderId}/suggest`, {});
  }

  // 6. PATCH /suggestions/:id
  updateSuggestionStatus(suggestionId: string, status: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/suggestions/${suggestionId}`, { status });
  }
}