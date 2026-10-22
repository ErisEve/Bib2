package com.example.loanmanagementservice.service;

import com.example.loanmanagementservice.model.Lending;
import com.example.loanmanagementservice.repository.LendingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LendingService {

    @Autowired
    private LendingRepository lendingRepository;

    public Lending saveLending(Lending lending) {

        return lendingRepository.save(lending);
    }

    public List<Lending> getAllLendings() {
        return lendingRepository.findAll();
    }

    public List<Lending> getLendingsByUserEmail(String email) {
        return lendingRepository.findByUserEmail(email);
    }

    public void deleteById(Long id) {
        lendingRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return lendingRepository.existsById(id);
    }


}
