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
        // Fetch node capacities
        Map<String, Node> nodeMap = kubernetesClient.nodes().list().getItems().stream()
                .collect(Collectors.toMap(
                        node -> node.getMetadata().getName(),
                        node -> node
                ));

        // Fetch current node metrics from Metrics Server
        NodeMetricsList metricsResult = kubernetesClient.top().nodes().metrics();
        List<NodeMetrics> nodeMetricsList = metricsResult.getItems();

        long clusterTotalCpu = 0;
        long clusterUsedCpu = 0;
        long clusterTotalMemory = 0;
        long clusterUsedMemory = 0;

        List<ClusterMetricsResponse.NodeMetrics> nodeDetails = new ArrayList<>();

        for (NodeMetrics nodeMetric : nodeMetricsList) {
            String nodeName = nodeMetric.getMetadata().getName();
            Node node = nodeMap.get(nodeName);

            if (node == null) {
                log.warn("Node {} found in metrics but not in node list, skipping", nodeName);
                continue;
            }

            // Get capacity from the node
            Map<String, Quantity> capacity = node.getStatus().getCapacity();
            long totalCpuMillis = toMilliCores(capacity.get("cpu"));
            long totalMemMi = toMebibytes(capacity.get("memory"));

            // Get current usage from the metrics
            Map<String, Quantity> usage = nodeMetric.getUsage();
            long usedCpuMillis = toMilliCores(usage.get("cpu"));
            long usedMemMi = toMebibytes(usage.get("memory"));

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
     * Handles cores ("2"), millicores ("500m"), microcores ("500000u"), and nanocores ("136621729n").
     */
    private long toMilliCores(Quantity quantity) {
        if (quantity == null) return 0;
        String strValue = quantity.getAmount();
        String format = quantity.getFormat();

        if (strValue == null || strValue.isEmpty()) return 0;

        try {
            BigDecimal value = new BigDecimal(strValue);

            if (format == null || format.isEmpty()) {
                // Cores (e.g. "2" → 2000 millicores)
                return value.multiply(BigDecimal.valueOf(1000)).longValue();
            }

            switch (format.toLowerCase()) {
                case "m":
                    // Millicores (e.g. "500m" → 500 millicores)
                    return value.longValue();
                case "u":
                    // Microcores (e.g. "500000u" → 500 millicores)
                    return value.divide(BigDecimal.valueOf(1_000), 0, java.math.RoundingMode.HALF_UP).longValue();
                case "n":
                    // Nanocores (e.g. "136621729n" → 136 millicores)
                    return value.divide(BigDecimal.valueOf(1_000_000), 0, java.math.RoundingMode.HALF_UP).longValue();
                default:
                    // Fallback using getAmountInBytes
                    BigDecimal cores = Quantity.getAmountInBytes(quantity);
                    return cores.multiply(BigDecimal.valueOf(1000)).longValue();
            }
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
        BigDecimal bytes = Quantity.getAmountInBytes(quantity);
        // Convert bytes to MiB
        return bytes.divide(BigDecimal.valueOf(1024 * 1024), 0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private double percentage(long used, long total) {
        if (total == 0) return 0.0;
        return Math.round(((double) used / total) * 1000.0) / 10.0; // one decimal place
    }
}
