package com.vyshnavi.gamesession.session;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public SessionResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/complete")
    public SessionResponse complete(@PathVariable String id) {
        return service.complete(id);
    }
}
