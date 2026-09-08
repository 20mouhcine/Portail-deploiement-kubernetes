package org.example.Deployment.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMetricsResponse {

    private long totalCpuMillicores;
    private long usedCpuMillicores;
    private double cpuPercentage;

    private long totalMemoryMi;
    private long usedMemoryMi;
    private double memoryPercentage;

    private List<NodeMetrics> nodes;
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeMetrics {
        private String name;
        private long totalCpuMillicores;
        private long usedCpuMillicores;
        private double cpuPercentage;
        private long totalMemoryMi;
        private long usedMemoryMi;
        private double memoryPercentage;
    }
}
