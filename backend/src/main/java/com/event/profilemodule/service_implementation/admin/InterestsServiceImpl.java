package com.event.profilemodule.service_implementation.admin;

import com.event.profilemodule.Repository.InterestsRepository;
import com.event.profilemodule.Services.InterestsService;
import com.event.profilemodule.entity.admin.Interests;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestsServiceImpl implements InterestsService {

    private final InterestsRepository interestsRepository;

    @Override
    public Interests createInterest(Interests interest) {
        log.info("Creating new interest: {}", interest.getInterestName());
        return interestsRepository.save(interest);
    }

    @Override
    public Interests getInterest(Long id) throws ResourceNotFoundException {
        return interestsRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Interest not found with id {}", id);
                    return new ResourceNotFoundException("Interest not found with id " + id);
                });
    }

    @Override
    public List<Interests> getAllInterests() {
        return interestsRepository.findAll();
    }

    @Override
    @Transactional
    public Interests updateInterest(Long id, Interests updatedInterest) {
        updatedInterest.setInterestId(id);
        return interestsRepository.save(updatedInterest);
    }

    @Override
    @Transactional
    public void deleteInterest(Long id) throws ResourceNotFoundException {
        // Spring Data 3+ deleteById silently ignores missing ids, so check explicitly
        if (!interestsRepository.existsById(id)) {
            log.error("Attempted to delete non-existing interest with id {}", id);
            throw new ResourceNotFoundException("Interest not found with id " + id);
        }
        log.warn("Deleting interest with id {}", id);
        interestsRepository.deleteById(id);
    }
}
