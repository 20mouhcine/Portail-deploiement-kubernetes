package org.example.Deployment.ServiceImpl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Deployment.DTO.*;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Entity.DeploymentJob;
import org.example.Deployment.Entity.DeploymentRevision;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Enums.JobStatus;
import org.example.Deployment.Repository.DeploymentJobRepository;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Repository.DeploymentRevisionRepository;
import org.example.Deployment.Service.IDeploymentEventService;
import org.example.Deployment.Service.IDeploymentService;
import org.example.Deployment.Service.IKubernetesDeploymentService;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.Projects.Security.ProjectAccessService;
import org.example.auth.entity.User;
import org.example.auth.exception.DuplicateResourceException;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

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
    private final DeploymentJobRepository deploymentJobRepository;
    private final ProjectAccessService projectAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<DeploymentResponse> findAll(Authentication authentication) {
        return deploymentRepository.findAllWithProjectByOrderByCreatedAtDesc()
                .stream()
                .filter(deployment -> projectAccessService.canRead(deployment.getProject(), authentication))
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
                        .orElseGet(() -> deploymentJobRepository
                                .findFirstByDeploymentIdAndStatusOrderByCreatedAtDesc(id, JobStatus.FAILED)
                                .map(DeploymentJob::getErrorMessage)
                                .filter(message -> !message.isBlank())
                                .orElse(null)));

        return DeploymentDetailResponse.from(
                deployment,
                pods,
                events,
                rolloutHistory,
                availableReplicas,
                readyReplicas,
                unavailableReplicas,
                failureCause);
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

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(saved.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.CREATE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        logger.info(() -> "Deployment queued: id=" + saved.getId() + ", namespace=" + saved.getNamespace()
                + ", name=" + saved.getName() + ", jobId=" + job.getId());
        saveRevisionSnapshot(saved);

        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse update(UUID id, UpdateDeploymentRequest request) {
        Deployment deployment = getDeployment(id);
        String name = request.getName().trim();
        String namespace = request.getNamespace().trim();
        if (!deployment.getName().equals(name) || !deployment.getNamespace().equals(namespace)) {
            throw new IllegalArgumentException(
                    "Le nom et le namespace ne peuvent pas être modifiés après la création du déploiement");
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

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(saved.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.UPDATE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        saveRevisionSnapshot(saved);
        return response;
    }

    @Transactional(readOnly = true)
    public String getAccessUrl(UUID id) {
        return kubernetesDeploymentService.getAccessUrl(getDeployment(id));
    }

    @Override
    public DeploymentResponse restart(UUID id) {
        Deployment deployment = getDeployment(id);

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(deployment.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.RESTART)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse stop(UUID id) {
        Deployment deployment = getDeployment(id);

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(deployment.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.SCALE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .targetReplicas(0)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        deployment.setStatus(DeploymentStatus.STOPPED);
        DeploymentResponse response = DeploymentResponse.from(deploymentRepository.save(deployment), job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse scale(UUID id, Integer replicas) {
        Deployment deployment = getDeployment(id);

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(deployment.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.SCALE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .targetReplicas(replicas)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        deployment.setReplicas(replicas);
        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);
        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse rollback(UUID id) {
        Deployment deployment = getDeployment(id);
        List<DeploymentRevision> revisions = deploymentRevisionRepository
                .findByDeploymentIdOrderByRevisionNumberDesc(id);
        if (revisions.size() < 2) {
            throw new IllegalStateException("Aucune revision precedente disponible pour le rollback");
        }

        DeploymentRevision target = revisions.get(1);
        applyRevision(deployment, target);
        deployment.setStatus(DeploymentStatus.PENDING);
        Deployment saved = deploymentRepository.save(deployment);

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(saved.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.UPDATE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        saveRevisionSnapshot(saved);
        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentResponse delete(UUID id) {
        Deployment deployment = getDeployment(id);

        DeploymentJob job = DeploymentJob.builder()
                .deploymentId(deployment.getId())
                .operationType(org.example.Deployment.Enums.JobOperationType.DELETE)
                .status(org.example.Deployment.Enums.JobStatus.QUEUED)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        deploymentJobRepository.save(job);

        deployment.setStatus(DeploymentStatus.STOPPED);
        Deployment saved = deploymentRepository.save(deployment);
        DeploymentResponse response = DeploymentResponse.from(saved, job.getId());
        deploymentEventService.publishDeploymentUpdated(response);
        return response;
    }

    @Override
    public DeploymentJobResponse getJobStatus(UUID jobId) {
        DeploymentJob job = deploymentJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job introuvable avec l'identifiant " + jobId));
        return DeploymentJobResponse.from(job);
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
        deployment.setEnvVariables(validateEnvironmentVariables(envVariables));
        validateSecretReferences(secretVariables);
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

    private void validateSecretReferences(Map<String, String> secretReferences) {
        if (secretReferences == null)
            return;
        for (Map.Entry<String, String> entry : secretReferences.entrySet()) {
            if (!entry.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") || entry.getValue() == null
                    || !entry.getValue().matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?/[A-Za-z0-9._-]+")) {
                throw new IllegalArgumentException("Les secrets doivent utiliser le format nom-du-secret/clé.");
            }
        }
    }

    private Map<String, String> validateEnvironmentVariables(Map<String, String> variables) {
        if (variables == null) {
            return Map.of();
        }
        Map<String, String> validated = new HashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey() == null || !entry.getKey().matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Le nom d'une variable d'environnement est invalide.");
            }
            if (entry.getValue() == null || entry.getValue().length() > 4096) {
                throw new IllegalArgumentException("La valeur d'une variable d'environnement est invalide ou trop longue.");
            }
            validated.put(entry.getKey(), entry.getValue());
        }
        return validated;
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
