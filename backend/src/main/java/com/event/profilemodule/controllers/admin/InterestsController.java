package com.event.profilemodule.controllers.admin;

import com.event.profilemodule.Services.InterestsService;
import com.event.sharedmodule.OverallCheckAvailability;
import com.event.profilemodule.entity.admin.Interests;
import com.event.sharedmodule.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/management")
@RequiredArgsConstructor
public class InterestsController {
    private final InterestsService interestService;
    private final OverallCheckAvailability overallCheckAvailability;
    // CREATE Interest
    @PostMapping("/create/interest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interests> createInterest(@RequestBody Interests interest) throws AccessDeniedException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interestService.createInterest(interest));
    }

    // DELETE Interest
    @GetMapping("/delete/interest")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteInterest(@RequestParam("id") Long id) throws ResourceNotFoundException, AccessDeniedException {
        interestService.deleteInterest(id);
    }


    // GET Single Interest
    @GetMapping("/interest")
    public ResponseEntity<Interests> getInterest(@RequestAttribute("userId") String userId)
            throws ResourceNotFoundException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(interestService.getInterest(Long.valueOf(userId)));
    }
}
