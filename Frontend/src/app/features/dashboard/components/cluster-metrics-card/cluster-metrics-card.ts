import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricsService } from '../../../../core/metrics/services/metrics.service';

@Component({
  selector: 'app-cluster-metrics-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cluster-metrics-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClusterMetricsCard implements OnInit, OnDestroy {
  private readonly metricsService = inject(MetricsService);

  readonly metrics = this.metricsService.metrics;
  readonly connected = this.metricsService.connected;
  readonly error = this.metricsService.error;

  ngOnInit(): void {
    this.metricsService.startStreaming();
  }

  ngOnDestroy(): void {
    this.metricsService.stopStreaming();
  }

  formatCpu(millicores: number): string {
    const cores = millicores / 1000;
    return `${cores.toFixed(2)} Cores`;
  }

  formatMemory(mi: number): string {
    if (mi >= 1024) {
      return `${(mi / 1024).toFixed(2)} GiB`;
    }
    return `${mi} MiB`;
  }

  getPercentageTone(pct: number): 'emerald' | 'amber' | 'rose' {
    if (pct > 85) return 'rose';
    if (pct > 70) return 'amber';
    return 'emerald';
  }
}
