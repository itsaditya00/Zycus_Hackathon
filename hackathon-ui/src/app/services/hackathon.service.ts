import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Order {
  id: string;
  description: string;
  assignedAgentId: string;
  status: 'ASSIGNED' | 'REASSIGNMENT_PENDING' | 'REASSIGNED' | 'DELIVERED';
  createdAt?: string;
}

export interface Suggestion {
  id: string;
  orderId: string;
  recommendedAgentId: string;
  confidenceScore: number;
  reasoning: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'FAILED';
  triggerReason?: string;
}

@Injectable({
  providedIn: 'root'
})
export class HackathonService {
  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // POST /orders
  createOrder(order: Partial<Order>): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/orders`, order);
  }

  // GET /orders with dynamic criteria mapping
  getOrders(filters: { id?: string; status?: string; agentId?: string }): Observable<Order[]> {
    let params = new HttpParams();
    if (filters.id) params = params.set('id', filters.id);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.agentId) params = params.set('agentId', filters.agentId);

    return this.http.get<Order[]>(`${this.baseUrl}/orders`, { params });
  }

  // PUT /suggestions/strategy
  updateGlobalStrategy(strategy: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/suggestions/strategy`, { strategy });
  }

  // PATCH /agents/{id}/status
  patchAgentStatus(id: string, status: string): Observable<any> {
    return this.http.patch(`${this.baseUrl}/agents/${id}/status`, { status });
  }

  // POST /orders/{id}/suggest?strategy=
  requestSuggestion(orderId: string, strategy?: string): Observable<Suggestion> {
    let url = `${this.baseUrl}/orders/${orderId}/suggest`;
    if (strategy) url += `?strategy=${strategy}`;
    return this.http.post<Suggestion>(url, {});
  }

  // PATCH /suggestions/{orderId}
  processDecision(orderId: string, status: string): Observable<Suggestion> {
    return this.http.patch<Suggestion>(`${this.baseUrl}/suggestions/${orderId}`, { status });
  }

  // GET /orders/{id}/suggest/stream (Server-Sent Events)
  observeAiThinkingStream(orderId: string, strategy?: string): Observable<string> {
    return new Observable<string>((observer) => {
      let url = `${this.baseUrl}/orders/${orderId}/suggest/stream`;
      if (strategy) url += `?strategy=${strategy}`;

      const eventSource = new EventSource(url);

      eventSource.onmessage = (event) => {
        observer.next(event.data);
      };

      eventSource.onerror = (error) => {
        if (eventSource.readyState === EventSource.CLOSED) {
          observer.complete();
        } else {
          observer.error(error);
        }
      };

      return () => {
        eventSource.close();
      };
    });
  }
}