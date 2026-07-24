package org.example.Deployment.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.example.Deployment.DTO.CreateDeploymentRequest;
import org.example.Deployment.DTO.DeploymentResponse;
import org.example.Deployment.DTO.UpdateDeploymentRequest;
import org.example.Deployment.Entity.Deployment;
import org.example.Deployment.Enums.DeploymentStatus;
import org.example.Deployment.Repository.DeploymentRepository;
import org.example.Deployment.Service.IDeploymentService;
import org.example.Projects.Entity.Project;
import org.example.Projects.Repository.ProjectRepository;
import org.example.auth.entity.User;
import org.example.auth.exception.DuplicateResourceException;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeploymentServiceImpl implements IDeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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
    public DeploymentResponse create(CreateDeploymentRequest request, String username) {
        String name = request.getName().trim();
        String namespace = request.getNamespace().trim();
        ensureUnique(name, namespace, null);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet introuvable avec l'identifiant " + request.getProjectId()
                ));
        User deployedBy = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur authentifié introuvable"
                ));

        Deployment deployment = Deployment.create();
        applyConfiguration(
                deployment,
                name,
                namespace,
                request.getReplicas(),
                request.getImage(),
                request.getPort(),
                request.getCpu(),
                request.getMemory()
        );
        deployment.setProject(project);
        deployment.setDeployedBy(deployedBy);
        deployment.setStatus(DeploymentStatus.PENDING);

        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Override
    public DeploymentResponse update(UUID id, UpdateDeploymentRequest request) {
        Deployment deployment = getDeployment(id);
        String name = request.getName().trim();
        String namespace = request.getNamespace().trim();
        ensureUnique(name, namespace, id);

        applyConfiguration(
                deployment,
                name,
                namespace,
                request.getReplicas(),
                request.getImage(),
                request.getPort(),
                request.getCpu(),
                request.getMemory()
        );
        deployment.setStatus(DeploymentStatus.PENDING);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Override
    public DeploymentResponse restart(UUID id) {
        Deployment deployment = getDeployment(id);
        deployment.setStatus(DeploymentStatus.PENDING);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Override
    public DeploymentResponse stop(UUID id) {
        Deployment deployment = getDeployment(id);
        deployment.setStatus(DeploymentStatus.STOPPED);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Override
    public DeploymentResponse scale(UUID id, Integer replicas) {
        Deployment deployment = getDeployment(id);
        deployment.setReplicas(replicas);
        deployment.setStatus(DeploymentStatus.PENDING);
        return DeploymentResponse.from(deploymentRepository.save(deployment));
    }

    @Override
    public void delete(UUID id) {
        Deployment deployment = getDeployment(id);
        deploymentRepository.delete(deployment);
    }

    private Deployment getDeployment(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Déploiement introuvable avec l'identifiant " + id
                ));
    }

    private void ensureUnique(String name, String namespace, UUID excludedId) {
        boolean exists = excludedId == null
                ? deploymentRepository.existsByNameIgnoreCaseAndNamespaceIgnoreCase(name, namespace)
                : deploymentRepository.existsByNameIgnoreCaseAndNamespaceIgnoreCaseAndIdNot(
                        name, namespace, excludedId
                );
        if (exists) {
            throw new DuplicateResourceException(
                    "Un déploiement nommé " + name + " existe déjà dans le namespace " + namespace
            );
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
            String memory
    ) {
        deployment.setName(name);
        deployment.setNamespace(namespace);
        deployment.setReplicas(replicas);
        deployment.setImage(image.trim());
        deployment.setPort(port);
        deployment.setCpu(cpu.trim());
        deployment.setMemory(memory.trim());
    }
}
