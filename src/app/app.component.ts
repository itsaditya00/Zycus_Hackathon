import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HackathonService } from './services/hackathon.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  // Form Models
  newOrder = { id: '', description: '', assignedAgentId: '', status: 'ASSIGNED' };
  strategy = 'RULE_BASED';
  agentUpdate = { id: '', status: 'OFFLINE' };
  suggestionId = '';

  // Data Displays
  assignedOrders: any[] = [];
  logs: string[] = [];

  constructor(private apiService: HackathonService) {}

  ngOnInit() {
    this.fetchAssignedOrders();
  }

  addLog(message: string) {
    const timestamp = new Date().toLocaleTimeString();
    this.logs.unshift(`[${timestamp}] ${message}`);
  }

  fetchAssignedOrders() {
    this.apiService.getOrdersByStatus('ASSIGNED').subscribe({
      next: (data) => {
        this.assignedOrders = data;
        this.addLog('Fetched assigned orders successfully.');
      },
      error: (err) => this.addLog(`Error fetching orders: ${err.message}`)
    });
  }

  submitOrder() {
    this.apiService.createOrder(this.newOrder).subscribe({
      next: (res) => {
        this.addLog(`Order ${this.newOrder.id} created successfully.`);
        this.fetchAssignedOrders();
        this.newOrder = { id: '', description: '', assignedAgentId: '', status: 'ASSIGNED' };
      },
      error: (err) => this.addLog(`Error creating order: ${err.message}`)
    });
  }

  changeStrategy() {
    this.apiService.updateStrategy(this.strategy).subscribe({
      next: () => this.addLog(`Strategy updated to ${this.strategy}.`),
      error: (err) => this.addLog(`Error updating strategy: ${err.message}`)
    });
  }

  updateAgent() {
    this.apiService.updateAgentStatus(this.agentUpdate.id, this.agentUpdate.status).subscribe({
      next: () => this.addLog(`Agent ${this.agentUpdate.id} status changed to ${this.agentUpdate.status}.`),
      error: (err) => this.addLog(`Error updating agent: ${err.message}`)
    });
  }

  requestSuggestion(orderId: string) {
    this.apiService.triggerSuggestion(orderId).subscribe({
      next: () => this.addLog(`Suggestion triggered for Order: ${orderId}.`),
      error: (err) => this.addLog(`Error triggering suggestion: ${err.message}`)
    });
  }

  processSuggestion(status: 'ACCEPTED' | 'REJECTED') {
    if (!this.suggestionId) return;
    this.apiService.updateSuggestionStatus(this.suggestionId, status).subscribe({
      next: () => {
        this.addLog(`Suggestion ${this.suggestionId} marked as ${status}.`);
        this.suggestionId = '';
      },
      error: (err) => this.addLog(`Error updating suggestion: ${err.message}`)
    });
  }
}