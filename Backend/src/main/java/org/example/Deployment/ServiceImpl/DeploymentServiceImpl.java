package org.example.Deployment.ServiceImpl;

import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentDetailResponse;
import org.example.Deployment.DTO.DeploymentEventResponse;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.DeploymentRevisionResponse;
import org.example.Deployment.DTO.PodResponse;
import org.example.Deployment.DTO.UpdateDeploymentRequest;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentRevision;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Exception.KubernetesOperationException;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Repository.DeploymentRevisionRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.example.Deployment.Service.IDeploymentService;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.auth.entity.User;
import org.example.auth.exception.DuplicateResourceException;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@Transactional
@Slf4j
@AllArgsConstructor
public class DeploymentServiceImpl implements IDeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DeploymentRevisionRepository deploymentRevisionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final IKubernetesDeploymentService kubernetesDeploymentService;
    private final DeploymentStatusSynchronizer deploymentStatusSynchronizer;
    private final IDeploymentEventService deploymentEventService;

    @Override
    @Transactional(readOnly = true)
    public List<DeploymentResponse> findAll() {
        return deploymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(DeploymentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeploymentResponse findById(UUID id) {
        return DeploymentResponse.from(getDeployment(id));
    }

        @Override
        @Transactional(readOnly = true)
        public DeploymentDetailResponse getDetail(UUID id) {
        Deployment deployment = getDeployment(id);
        List<PodResponse> pods = getPods(id);
        List<DeploymentRevisionResponse> rolloutHistory = deploymentRevisionRepository
            .findByDeploymentIdOrderByRevisionNumberDesc(id)
            .stream()
            .map(DeploymentRevisionResponse::from)
            .toList();
        List<DeploymentEventResponse> events = deploymentEventService
            .findByDeploymentIdOrderByTimestampDesc(id)
            .stream()
            .limit(25)
            .map(DeploymentEventResponse::from)
            .toList();

        int availableReplicas = (int) pods.stream().filter(pod -> "Running".equalsIgnoreCase(pod.getStatus())).count();
        int readyReplicas = (int) pods.stream().filter(pod -> Boolean.TRUE.equals(pod.getReady())).count();
        int unavailableReplicas = Math.max(deployment.getReplicas() - availableReplicas, 0);
        String failureCause = events.stream()
            .filter(event -> "ERROR".equalsIgnoreCase(event.getLevel()))
            .map(DeploymentEventResponse::getMessage)
            .findFirst()
            .orElseGet(() -> pods.stream()
                .map(PodResponse::getReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .findFirst()
                .orElse(null));

        return DeploymentDetailResponse.from(
            deployment,
            pods,
            events,
            rolloutHistory,
            availableReplicas,
            readyReplicas,
            unavailableReplicas,
            failureCause
        );
        }

    @Override
    public DeploymentResponse create(CreateDeploymentRequest request, String username) {
        String name = request.getName().trim();
        String namespace = request.getNamespace().trim();
        ensureUnique(name, namespace, null);

        Logger logger = Logger.getLogger(DeploymentServiceImpl.class.getName());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet introuvable avec l'identifiant " + request.getProjectId()));
        User deployedBy = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur authentifié introuvable"));

        Deployment deployment = Deployment.create();
        applyConfiguration(
                deployment,
                name,
                namespace,
                request.getReplicas(),
                request.getImage(),
                request.getPort(),
                request.getCpu(),
                request.getMemory(),
                request.getEnvVariables(),
                request.getSecretVariables(),
                request.getGitRepository(),
                request.getGitBranch(),
                request.getGitCommit(),
                request.getGitTag(),
                request.getRequestedHostname(),
                request.getRequestedPath(),
                request.getTlsEnabled(),
                request.getTlsSecretName());
        deployment.setProject(project);
        deployment.setDeployedBy(deployedBy);
        deployment.setStatus(DeploymentStatus.PENDING);

        Deployment saved = deploymentRepository.save(deployment);
        deploymentStatusSynchronizer.resumeTracking(saved);

        logger.info("Deployment created: " + saved);
        try {
            String accessUrl = kubernetesDeploymentService.deploy(saved);
            saved.setAccessUrl(accessUrl);
            logger.info("Access URL set for deployment {}: {}"+ saved.getName()+ accessUrl);
            deploymentRepository.save(saved);
            saveRevisionSnapshot(saved);
        } catch (KubernetesClientException e) {
            log.error("Échec du déploiement Kubernetes pour {}", saved.getName(), e);
            saved.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(saved);
            throw new KubernetesOperationException("Échec du déploiement sur le cluster: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DeploymentResponse response = DeploymentResponse.from(saved);
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse update(UUID id, UpdateDeploymentRequest request) {
        Deployment deployment = getDeployment(id);
        String name = request.getName().trim();
        String namespace = request.getNamespace().trim();
        if (!deployment.getName().equals(name) || !deployment.getNamespace().equals(namespace)) {
            throw new IllegalArgumentException("Le nom et le namespace ne peuvent pas �tre modifi�s apr�s la cr�ation du d�ploiement");
        }
        ensureUnique(name, namespace, id);

        applyConfiguration(
                deployment,
                name,
                namespace,
                request.getReplicas(),
                request.getImage(),
                request.getPort(),
                request.getCpu(),
                request.getMemory(),
                request.getEnvVariables(),
                request.getSecretVariables(),
                request.getGitRepository(),
                request.getGitBranch(),
                request.getGitCommit(),
                request.getGitTag(),
                request.getRequestedHostname(),
                request.getRequestedPath(),
                request.getTlsEnabled(),
                request.getTlsSecretName());
        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        deploymentStatusSynchronizer.resumeTracking(saved);

        try {
            kubernetesDeploymentService.updateSpec(saved);
        } catch (KubernetesClientException e) {
            log.error("Échec de la mise à jour Kubernetes pour {}", saved.getName(), e);
            saved.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(saved);
            throw new KubernetesOperationException("Échec de la mise à jour sur le cluster: " + e.getMessage());
        }

        DeploymentResponse response = DeploymentResponse.from(saved);
        deploymentEventService.publishDeploymentUpdated(response);
        saveRevisionSnapshot(saved);
        return response;
    }

    // In DeploymentServiceImpl
    @Transactional(readOnly = true)
    public String getAccessUrl(UUID id) {
        return kubernetesDeploymentService.getAccessUrl(getDeployment(id));
    }

    @Override
    public DeploymentResponse restart(UUID id) {
        Deployment deployment = getDeployment(id);

        try {
            kubernetesDeploymentService.restart(deployment);
        } catch (KubernetesClientException e) {
            log.error("Échec du redémarrage Kubernetes pour {}", deployment.getName(), e);
            throw new KubernetesOperationException("Échec du redémarrage: " + e.getMessage());
        }

        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        deploymentStatusSynchronizer.resumeTracking(saved);
        DeploymentResponse response = DeploymentResponse.from(saved);
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse stop(UUID id) {
        Deployment deployment = getDeployment(id);

        try {
            kubernetesDeploymentService.scale(deployment, 0);
        } catch (KubernetesClientException e) {
            log.error("Échec de l'arrêt Kubernetes pour {}", deployment.getName(), e);
            throw new KubernetesOperationException("Échec de l'arrêt: " + e.getMessage());
        }

        deployment.setStatus(DeploymentStatus.STOPPED);
        DeploymentResponse response = DeploymentResponse.from(deploymentRepository.save(deployment));
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse scale(UUID id, Integer replicas) {
        Deployment deployment = getDeployment(id);

        try {
            kubernetesDeploymentService.scale(deployment, replicas);
        } catch (KubernetesClientException e) {
            log.error("Échec de la mise à l'échelle Kubernetes pour {}", deployment.getName(), e);
            throw new KubernetesOperationException("Échec de la mise à l'échelle: " + e.getMessage());
        }

        deployment.setReplicas(replicas);
        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        deploymentStatusSynchronizer.resumeTracking(saved);
        DeploymentResponse response = DeploymentResponse.from(saved);
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse rollback(UUID id) {
        Deployment deployment = getDeployment(id);
        List<DeploymentRevision> revisions = deploymentRevisionRepository.findByDeploymentIdOrderByRevisionNumberDesc(id);
        if (revisions.size() < 2) {
            throw new IllegalStateException("Aucune revision precedente disponible pour le rollback");
        }

        DeploymentRevision target = revisions.get(1);
        applyRevision(deployment, target);
        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        deploymentStatusSynchronizer.resumeTracking(saved);

        try {
            kubernetesDeploymentService.updateSpec(saved);
        } catch (KubernetesClientException e) {
            log.error("Échec du rollback Kubernetes pour {}", saved.getName(), e);
            saved.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(saved);
            throw new KubernetesOperationException("Échec du rollback: " + e.getMessage());
        }

        saveRevisionSnapshot(saved);
        DeploymentResponse response = DeploymentResponse.from(saved);
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public void delete(UUID id) {
        Deployment deployment = getDeployment(id);

        try {
            kubernetesDeploymentService.delete(deployment);
        } catch (KubernetesClientException e) {
            log.error("Échec de la suppression Kubernetes pour {}", deployment.getName(), e);
            throw new KubernetesOperationException("Échec de la suppression sur le cluster: " + e.getMessage());
        }

        deploymentRepository.delete(deployment);
    }

    @Transactional(readOnly = true)
    @Override
    public String getLogs(UUID id) {
        return kubernetesDeploymentService.getLogs(getDeployment(id));
    }

    private Deployment getDeployment(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Déploiement introuvable avec l'identifiant " + id));
    }

    @Transactional(readOnly = true)
    public List<PodResponse> getPods(UUID id) {
        return kubernetesDeploymentService.getPods(getDeployment(id))
                .stream()
                .map(PodResponse::from)
                .toList();
    }

    private void ensureUnique(String name, String namespace, UUID excludedId) {
        boolean exists = excludedId == null
                ? deploymentRepository.existsByNameIgnoreCaseAndNamespaceIgnoreCase(name, namespace)
                : deploymentRepository.existsByNameIgnoreCaseAndNamespaceIgnoreCaseAndIdNot(
                        name, namespace, excludedId);
        if (exists) {
            throw new DuplicateResourceException(
                    "Un déploiement nommé " + name + " existe déjà dans le namespace " + namespace);
        }
    }

    private void applyConfiguration(
            Deployment deployment,
            String name,
            String namespace,
            Integer replicas,
            String image,
            Integer port,
            String cpu,
            String memory,
            Map<String, String> envVariables,
            Map<String, String> secretVariables,
            String gitRepository,
            String gitBranch,
            String gitCommit,
            String gitTag,
            String requestedHostname,
            String requestedPath,
            Boolean tlsEnabled,
            String tlsSecretName) {
        deployment.setName(name);
        deployment.setNamespace(namespace);
        deployment.setReplicas(replicas);
        deployment.setImage(image.trim());
        deployment.setPort(port);
        deployment.setCpu(cpu.trim());
        deployment.setMemory(memory.trim());
        deployment.setEnvVariables(envVariables != null ? envVariables : Map.of());
        deployment.setSecretVariables(secretVariables != null ? secretVariables : Map.of());
        deployment.setGitRepository(gitRepository != null ? gitRepository.trim() : null);
        deployment.setGitBranch(gitBranch != null ? gitBranch.trim() : null);
        deployment.setGitCommit(gitCommit != null ? gitCommit.trim() : null);
        deployment.setGitTag(gitTag != null ? gitTag.trim() : null);
        deployment.setRequestedHostname(requestedHostname != null ? requestedHostname.trim() : null);
        deployment.setRequestedPath(requestedPath != null ? requestedPath.trim() : null);
        deployment.setTlsEnabled(tlsEnabled != null ? tlsEnabled : Boolean.FALSE);
        deployment.setTlsSecretName(tlsSecretName != null ? tlsSecretName.trim() : null);

    }

    private void saveRevisionSnapshot(Deployment deployment) {
        DeploymentRevision revision = DeploymentRevision.builder()
                .deploymentId(deployment.getId())
                .revisionNumber((int) deploymentRevisionRepository.countByDeploymentId(deployment.getId()) + 1)
                .image(deployment.getImage())
                .replicas(deployment.getReplicas())
                .port(deployment.getPort())
                .cpu(deployment.getCpu())
                .memory(deployment.getMemory())
                .envVariables(new HashMap<>(deployment.getEnvVariables()))
                .secretVariables(new HashMap<>(deployment.getSecretVariables()))
                .gitRepository(deployment.getGitRepository())
                .gitBranch(deployment.getGitBranch())
                .gitCommit(deployment.getGitCommit())
                .gitTag(deployment.getGitTag())
                .requestedHostname(deployment.getRequestedHostname())
                .requestedPath(deployment.getRequestedPath())
                .tlsEnabled(deployment.getTlsEnabled())
                .tlsSecretName(deployment.getTlsSecretName())
                .build();
        deploymentRevisionRepository.save(revision);
    }

    private void applyRevision(Deployment deployment, DeploymentRevision revision) {
        deployment.setImage(revision.getImage());
        deployment.setReplicas(revision.getReplicas());
        deployment.setPort(revision.getPort());
        deployment.setCpu(revision.getCpu());
        deployment.setMemory(revision.getMemory());
        deployment.setEnvVariables(new HashMap<>(revision.getEnvVariables()));
        deployment.setSecretVariables(new HashMap<>(revision.getSecretVariables()));
        deployment.setGitRepository(revision.getGitRepository());
        deployment.setGitBranch(revision.getGitBranch());
        deployment.setGitCommit(revision.getGitCommit());
        deployment.setGitTag(revision.getGitTag());
        deployment.setRequestedHostname(revision.getRequestedHostname());
        deployment.setRequestedPath(revision.getRequestedPath());
        deployment.setTlsEnabled(revision.getTlsEnabled());
        deployment.setTlsSecretName(revision.getTlsSecretName());
    }
}
