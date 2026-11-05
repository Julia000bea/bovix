package com.bovix.controller;

import com.bovix.model.Animal;
import com.bovix.service.AnimalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;

    @GetMapping("/fazendas/{fazendaId}/animais")
    public List<Animal> listar(@PathVariable Long fazendaId) {
        return animalService.listarPorFazenda(fazendaId);
    }

    @GetMapping("/animais/{id}")
    public Animal buscar(@PathVariable Long id) {
        return animalService.buscar(id);
    }

    @PostMapping("/fazendas/{fazendaId}/animais")
    public ResponseEntity<Animal> criar(@PathVariable Long fazendaId, @Valid @RequestBody Animal dados) {
        return ResponseEntity.ok(animalService.criar(fazendaId, dados));
    }

    @PutMapping("/animais/{id}")
    public Animal atualizar(@PathVariable Long id, @Valid @RequestBody Animal dados) {
        return animalService.atualizar(id, dados);
    }

    @DeleteMapping("/animais/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        animalService.remover(id);
        return ResponseEntity.noContent().build();
    }

    /** Upload da foto do animal (multipart/form-data, campo "arquivo"). */
    @PostMapping(value = "/animais/{id}/foto", consumes = "multipart/form-data")
    public ResponseEntity<Animal> enviarFoto(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ResponseEntity.ok(animalService.salvarFoto(id, arquivo));
    }
}
