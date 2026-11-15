package uz.academixai.shared.feature;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Registers the annotated bean only while the psychology add-on module is switched on ({@link
 * PsychologyModule#ENABLED_PROPERTY}). Off means the bean does not exist — its endpoints answer 404
 * and its schedules never run.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@ConditionalOnProperty(name = PsychologyModule.ENABLED_PROPERTY, havingValue = "true")
public @interface ConditionalOnPsychologyModule {}
