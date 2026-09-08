export interface NodeMetrics {
  readonly name: string;
  readonly totalCpuMillicores: number;
  readonly usedCpuMillicores: number;
  readonly cpuPercentage: number;
  readonly totalMemoryMi: number;
  readonly usedMemoryMi: number;
  readonly memoryPercentage: number;
}

export interface ClusterMetrics {
  readonly totalCpuMillicores: number;
  readonly usedCpuMillicores: number;
  readonly cpuPercentage: number;
  readonly totalMemoryMi: number;
  readonly usedMemoryMi: number;
  readonly memoryPercentage: number;
  readonly nodes: readonly NodeMetrics[];
  readonly timestamp: string;
}
