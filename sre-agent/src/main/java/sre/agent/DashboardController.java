package sre.agent;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    @GetMapping(
            value = "/",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Resource> index() {
        Resource resource = new ClassPathResource(
                "static/index.html"
        );
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}