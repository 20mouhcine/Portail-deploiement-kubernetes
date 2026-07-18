package org.example.backend.Application.Service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.backend.Application.DTO.ApplicationResponse;
import org.example.backend.Application.DTO.CreateApplicationRequest;
import org.example.backend.Application.DTO.UpdateApplicationRequest;
import org.example.backend.Application.Entity.Application;
import org.example.backend.Application.Exceptions.ApplicationAlreadyExistsException;
import org.example.backend.Application.Repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author pc
 **/
@Service
@AllArgsConstructor
public class ApplicationService implements IApplicationService {
   private final ApplicationRepository applicationRepository;
    @Override
    public List<ApplicationResponse> getApplications() {

        return applicationRepository.findAll()
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Override
    public Application getApplicationByName(String name) {
        return applicationRepository.findByName(name);
    }

    @Override
    public Application getApplicationById(UUID id) {
        return applicationRepository.findById(id).orElse(null);
    }

    @Override
    public ApplicationResponse createApplication(CreateApplicationRequest request) {
        if(applicationRepository.existsByName(request.getName())){
            throw new ApplicationAlreadyExistsException(
                    "Application " + request.getName() + " already exists."
            );
        }

        Application app = new Application();

        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setRepository(request.getRepository());

        app.setCreatedAt(LocalDateTime.now());

        Application saved = applicationRepository.save(app);

        return ApplicationResponse.from(saved);
    }
@Override
    public void deleteApplicationById(UUID id){
        applicationRepository.deleteById(id);
}

    @Override
    public ApplicationResponse update(UUID id, UpdateApplicationRequest request) {
        Application app = applicationRepository.findById(id).orElse(null);
        Application saved = applicationRepository.save(app);

        return ApplicationResponse.from(saved);

    }
}
