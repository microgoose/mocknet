package net.microgoose.mocknet.interview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.microgoose.mocknet.interview.dto.grade.CreateGradeRequest;
import net.microgoose.mocknet.interview.model.Grade;
import net.microgoose.mocknet.interview.service.GradeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Tag(name = "Грейды")
public class GradeController {

    private final GradeService gradeService;

    @Operation(summary = "Получить грейды")
    @GetMapping
    public List<Grade> getAllGrades() {
        return gradeService.getAll();
    }

    @Operation(summary = "Создать грейд")
    @PostMapping
    public Grade createGrade(@RequestBody @Valid CreateGradeRequest request) {
        return gradeService.save(request);
    }
}