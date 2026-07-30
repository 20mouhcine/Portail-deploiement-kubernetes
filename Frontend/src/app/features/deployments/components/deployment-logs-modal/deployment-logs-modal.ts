import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeploymentSseService } from '../../../../core/deployments/services/deployment-sse.service';
import { DeploymentEvent } from '../../../../core/deployments/models/deployment-event.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-deployment-logs-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deployment-logs-modal.html',
  styleUrls: ['./deployment-logs-modal.css'],
  
})
export class DeploymentLogsModalComponent implements OnInit, OnDestroy {
  @Input() deploymentId!: string;
  @Input() deploymentName!: string;
  @Output() close = new EventEmitter<void>();

  @ViewChild('logsContainer') private logsContainer!: ElementRef;

  logs = signal<DeploymentEvent[]>([]);
  private sseSubscription?: Subscription;
  private autoScroll = true;

  constructor(private sseService: DeploymentSseService) {}

  ngOnInit(): void {
    this.sseSubscription = this.sseService.subscribeToDeploymentLogs(this.deploymentId).subscribe({
      next: (event) => {
        this.logs.update(currentLogs => {
          // Prevent duplicates if backend sends same historical log on reconnect
          if (currentLogs.some(l => l.id === event.id)) {
            return currentLogs;
          }
          return [...currentLogs, event];
        });
        this.scrollToBottom();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
    }
  }

  closeModal(): void {
    this.close.emit();
  }

  onScroll(): void {
    const element = this.logsContainer.nativeElement;
    // Check if user scrolled up
    this.autoScroll = Math.abs(element.scrollHeight - element.scrollTop - element.clientHeight) < 10;
  }

  private scrollToBottom(): void {
    if (this.autoScroll && this.logsContainer) {
      setTimeout(() => {
        const element = this.logsContainer.nativeElement;
        element.scrollTop = element.scrollHeight;
      }, 0);
    }
  }

  getIconForLevel(level: string): string {
    switch(level) {
      case 'INFO': return '●';
      case 'SUCCESS': return '✔';
      case 'WARNING': return '⚠';
      case 'ERROR': return '✖';
      default: return '●';
    }
  }
}
