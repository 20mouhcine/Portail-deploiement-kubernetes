package org.example.Deployment.Service;

import org.example.Deployment.DTO.ClusterMetricsResponse;

public interface IKubernetesMetricsService {
    ClusterMetricsResponse getClusterMetrics();
}
