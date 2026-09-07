package com.duoc.backend.Care;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/care")
public class CareController {

    @Autowired
    private CareRepository careRepository;

    @GetMapping
    public List<Care> getAllCares() {
        return (List<Care>) careRepository.findAll();
    }

    @GetMapping("/{id}")
    public Care getCareById(@PathVariable Long id) {
        return careRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Care saveCare(@RequestBody Care service) {
        return careRepository.save(service);
    }

    @DeleteMapping("/{id}")
    public void deleteCare(@PathVariable Long id) {
        careRepository.deleteById(id);
    }
}