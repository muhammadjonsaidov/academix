package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditUniqueTaskRequest(@NotBlank @Size(max = 20000) String taskContent) {}
