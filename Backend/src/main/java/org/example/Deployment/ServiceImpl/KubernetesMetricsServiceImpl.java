package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.ClusterMetricsResponse;
import org.example.Deployment.Service.IKubernetesMetricsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesMetricsServiceImpl implements IKubernetesMetricsService {

    private final KubernetesClient kubernetesClient;

    @Override
    public ClusterMetricsResponse getClusterMetrics() {
        // Fetch node capacities safely
        Map<String, Node> nodeMap = kubernetesClient.nodes().list().getItems().stream()
                .filter(node -> node.getMetadata() != null && node.getMetadata().getName() != null)
                .collect(Collectors.toMap(
                        node -> node.getMetadata().getName(),
                        node -> node,
                        (existing, replacement) -> existing
                ));

        // Fetch current node metrics from Metrics Server (with fallback if unavailable)
        List<NodeMetrics> nodeMetricsList = new ArrayList<>();
        try {
            NodeMetricsList metricsResult = kubernetesClient.top().nodes().metrics();
            if (metricsResult != null && metricsResult.getItems() != null) {
                nodeMetricsList = metricsResult.getItems();
            }
        } catch (Exception e) {
            log.warn("Unable to fetch Kubernetes node metrics from Metrics Server: {}", e.getMessage());
        }

        long clusterTotalCpu = 0;
        long clusterUsedCpu = 0;
        long clusterTotalMemory = 0;
        long clusterUsedMemory = 0;

        List<ClusterMetricsResponse.NodeMetrics> nodeDetails = new ArrayList<>();

        if (!nodeMetricsList.isEmpty()) {
            for (NodeMetrics nodeMetric : nodeMetricsList) {
                String nodeName = nodeMetric.getMetadata() != null ? nodeMetric.getMetadata().getName() : null;
                if (nodeName == null) continue;
                Node node = nodeMap.get(nodeName);

                if (node == null) {
                    log.warn("Node {} found in metrics but not in node list, skipping", nodeName);
                    continue;
                }

                // Get capacity from the node
                Map<String, Quantity> capacity = node.getStatus() != null ? node.getStatus().getCapacity() : null;
                long totalCpuMillis = capacity != null ? toMilliCores(capacity.get("cpu")) : 0;
                long totalMemMi = capacity != null ? toMebibytes(capacity.get("memory")) : 0;

                // Get current usage from the metrics
                Map<String, Quantity> usage = nodeMetric.getUsage();
                long usedCpuMillis = usage != null ? toMilliCores(usage.get("cpu")) : 0;
                long usedMemMi = usage != null ? toMebibytes(usage.get("memory")) : 0;

                clusterTotalCpu += totalCpuMillis;
                clusterUsedCpu += usedCpuMillis;
                clusterTotalMemory += totalMemMi;
                clusterUsedMemory += usedMemMi;

                nodeDetails.add(ClusterMetricsResponse.NodeMetrics.builder()
                        .name(nodeName)
                        .totalCpuMillicores(totalCpuMillis)
                        .usedCpuMillicores(usedCpuMillis)
                        .cpuPercentage(percentage(usedCpuMillis, totalCpuMillis))
                        .totalMemoryMi(totalMemMi)
                        .usedMemoryMi(usedMemMi)
                        .memoryPercentage(percentage(usedMemMi, totalMemMi))
                        .build());
            }
        } else {
            // Fallback: Populate total node capacity even if Metrics Server fails to return live usage
            for (Node node : nodeMap.values()) {
                String nodeName = node.getMetadata().getName();
                Map<String, Quantity> capacity = node.getStatus() != null ? node.getStatus().getCapacity() : null;
                long totalCpuMillis = capacity != null ? toMilliCores(capacity.get("cpu")) : 0;
                long totalMemMi = capacity != null ? toMebibytes(capacity.get("memory")) : 0;

                clusterTotalCpu += totalCpuMillis;
                clusterTotalMemory += totalMemMi;

                nodeDetails.add(ClusterMetricsResponse.NodeMetrics.builder()
                        .name(nodeName)
                        .totalCpuMillicores(totalCpuMillis)
                        .usedCpuMillicores(0)
                        .cpuPercentage(0.0)
                        .totalMemoryMi(totalMemMi)
                        .usedMemoryMi(0)
                        .memoryPercentage(0.0)
                        .build());
            }
        }

        return ClusterMetricsResponse.builder()
                .totalCpuMillicores(clusterTotalCpu)
                .usedCpuMillicores(clusterUsedCpu)
                .cpuPercentage(percentage(clusterUsedCpu, clusterTotalCpu))
                .totalMemoryMi(clusterTotalMemory)
                .usedMemoryMi(clusterUsedMemory)
                .memoryPercentage(percentage(clusterUsedMemory, clusterTotalMemory))
                .nodes(nodeDetails)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Converts a Kubernetes CPU Quantity to millicores.
     * Uses Fabric8's Quantity.getAmountInBytes() to handle all CPU formats safely (cores, millicores, microcores, nanocores).
     */
    private long toMilliCores(Quantity quantity) {
        if (quantity == null) return 0;
        try {
            BigDecimal cores = Quantity.getAmountInBytes(quantity);
            return cores.multiply(BigDecimal.valueOf(1000)).longValue();
        } catch (Exception e) {
            log.error("Failed to parse CPU quantity: {}", quantity, e);
            return 0;
        }
    }

    /**
     * Converts a Kubernetes memory Quantity to Mebibytes.
     * Examples: "4Gi" → 4096, "512Mi" → 512, "1073741824" → 1024
     */
    private long toMebibytes(Quantity quantity) {
        if (quantity == null) return 0;
        try {
            BigDecimal bytes = Quantity.getAmountInBytes(quantity);
            return bytes.divide(BigDecimal.valueOf(1024 * 1024), 0, java.math.RoundingMode.HALF_UP).longValue();
        } catch (Exception e) {
            log.error("Failed to parse memory quantity: {}", quantity, e);
            return 0;
        }
    }

    private double percentage(long used, long total) {
        if (total == 0) return 0.0;
        return Math.round(((double) used / total) * 1000.0) / 10.0; // one decimal place
    }
}
