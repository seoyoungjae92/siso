package com.siso.backend.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sources")
public class AdminSourceController {

    private final AdminSourceService adminSourceService;

    public AdminSourceController(AdminSourceService adminSourceService) {
        this.adminSourceService = adminSourceService;
    }

    @GetMapping
    public List<SourceDto> list() {
        return adminSourceService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceDto create(@RequestBody SourceRequest request) {
        return adminSourceService.create(request);
    }

    @PutMapping("/{id}")
    public SourceDto update(@PathVariable Long id, @RequestBody SourceRequest request) {
        return adminSourceService.update(id, request);
    }

    @PostMapping("/{id}/toggle")
    public SourceDto toggle(@PathVariable Long id) {
        return adminSourceService.toggle(id);
    }
}
