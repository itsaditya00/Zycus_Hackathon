import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

// Import your service from its correct directory
import { HackathonService } from './services/hackathon.service'; 

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(), // Essential for your service HTTP requests
    
    // Map the missing token explicitly to your service class
    { provide: 'HackathonServiceToken', useClass: HackathonService }
  ]
};
