package com.event.profilemodule.Services;

import com.event.profilemodule.entity.admin.Interests;
import com.event.sharedmodule.exception.ResourceNotFoundException;

import java.util.List;

public interface InterestsService {

    Interests createInterest(Interests interest);

    Interests getInterest(Long id) throws ResourceNotFoundException;

    List<Interests> getAllInterests();

    Interests updateInterest(Long id, Interests interest);

    void deleteInterest(Long id) throws ResourceNotFoundException;
}
