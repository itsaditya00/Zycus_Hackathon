import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Inject } from '@angular/core';
import { HackathonService, Order, Suggestion } from './services/hackathon.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'] // FIXED: Wrapped string inside an array
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'hackathon-ui';
  
  // Filter Criteria States
  filterId = '';
  filterStatus = '';
  filterAgentId = '';
  
  // Strategy Control States
  globalStrategy = 'AI_POWERED';
  executionStrategy = 'AI_POWERED';

  // Active Entities Arrays
  ordersList: Order[] = [];
  suggestionsList: Suggestion[] = [];
  logs: string[] = [];

  // Form Binding Data Objects
  newOrder: Partial<Order> = { id: '', description: '', assignedAgentId: '', status: 'ASSIGNED' };
  agentUpdate = { id: '', status: 'ONLINE' };

  // Real-time AI thinking tracking state
  streamingOrderId = '';
  aiThinkingText = '';
  private streamSubscription?: Subscription;

  constructor(
  @Inject('HackathonServiceToken') private hackathonService: HackathonService
) {}

  ngOnInit(): void {
    this.fetchFilteredOrders();
    this.addLog('Dashboard Initialized. System status tracking operational.');
  }

  fetchFilteredOrders(): void {
    const queryFilters = {
      id: this.filterId.trim() || undefined,
      status: this.filterStatus.trim() || undefined,
      agentId: this.filterAgentId.trim() || undefined
    };

    this.hackathonService.getOrders(queryFilters).subscribe({
      next: (res: Order[]) => { // FIXED: Added explicit strict typing
        this.ordersList = res;
        this.addLog(`Fetched ${res.length} orders matching active criteria parameters.`);
      },
      error: (err: any) => this.addLog(`Error querying orders data: ${err.message}`) // FIXED: Added type definition
    });
  }

  submitOrder(): void {
    if (!this.newOrder.id || !this.newOrder.description) return;
    this.hackathonService.createOrder(this.newOrder).subscribe({
      next: (res: Order) => { // FIXED: Added explicit strict typing
        this.addLog(`Successfully registered and persisted order record: ${res.id}`);
        this.fetchFilteredOrders();
        this.newOrder = { id: '', description: '', assignedAgentId: '', status: 'ASSIGNED' };
      },
      error: (err: any) => this.addLog(`Failed to post new order: ${err.message}`) // FIXED: Added type definition
    });
  }

  changeStrategy(): void {
    this.hackathonService.updateGlobalStrategy(this.globalStrategy).subscribe({
      next: () => this.addLog(`Global default backend framework updated to: ${this.globalStrategy}`),
      error: (err: any) => this.addLog(`Strategy switch execution rejected: ${err.message}`) // FIXED: Added type definition
    });
  }

  updateAgent(): void {
    if (!this.agentUpdate.id) return;
    this.hackathonService.patchAgentStatus(this.agentUpdate.id, this.agentUpdate.status).subscribe({
      next: () => {
        this.addLog(`Agent ${this.agentUpdate.id} status modified successfully to ${this.agentUpdate.status}.`);
        this.fetchFilteredOrders();
      },
      error: (err: any) => this.addLog(`Failed to update agent status profiles: ${err.message}`) // FIXED: Added type definition
    });
  }

  getSuggestion(orderId: string): void {
    this.addLog(`Requesting distribution recommendation options for Order ID: ${orderId}`);
    
    // Clear out any stale thinking stream allocations
    this.aiThinkingText = '';
    this.streamingOrderId = orderId;

    // First: Initialize the SSE Stream reader to show "AI thinking" tokens in real time
    if (this.streamSubscription) this.streamSubscription.unsubscribe();
    this.streamSubscription = this.hackathonService
      .observeAiThinkingStream(orderId, this.executionStrategy)
      .subscribe({
        next: (token: string) => { // FIXED: Added explicit strict typing
          this.aiThinkingText += token + ' ';
        },
        error: (err: any) => console.error('Stream completion or disconnect intercept.', err) // FIXED: Changed 'log' to 'console'
      });

    // Second: Fire the actual engine call to produce the decision object entity response payload
    this.hackathonService.requestSuggestion(orderId, this.executionStrategy).subscribe({
      next: (res: Suggestion) => { // FIXED: Added explicit strict typing
        this.addLog(`Received recommendation for order ${orderId}. Match confidence: ${res.confidenceScore}`);
        
        // Remove old occurrences of suggestions for the same order if they exist
        this.suggestionsList = this.suggestionsList.filter(s => s.orderId !== orderId);
        
        // Insert new suggestion object
        this.suggestionsList.push(res);
        
        // Sort the collection explicitly from High to Low based on Confidence Scores
        this.suggestionsList.sort((a, b) => b.confidenceScore - a.confidenceScore);
      },
      error: (err: any) => this.addLog(`Routing algorithm failed: ${err.message}`) // FIXED: Added type definition
    });
  }

  executeDecision(orderId: string, decision: string): void {
    this.hackathonService.processDecision(orderId, decision).subscribe({
      next: (res: Suggestion) => { // FIXED: Added explicit strict typing
        this.addLog(`Suggestion processing resolved. Order reference ${orderId} shifted to state: ${decision}`);
        
        // Clear streaming markers and remove the recommendation from our active view lists
        if (this.streamingOrderId === orderId) {
          this.streamingOrderId = '';
          this.aiThinkingText = '';
        }
        this.suggestionsList = this.suggestionsList.filter(s => s.orderId !== orderId);
        this.fetchFilteredOrders();
      },
      error: (err: any) => this.addLog(`Failed to pass resolution decision context state: ${err.message}`) // FIXED: Added type definition
    });
  }

  addLog(msg: string): void {
    const timestamp = new Date().toLocaleTimeString();
    this.logs.unshift(`[${timestamp}] ${msg}`);
  }

  ngOnDestroy(): void {
    if (this.streamSubscription) this.streamSubscription.unsubscribe();
  }
}