package uz.academixai.infrastructure.ai;

import java.util.List;

/** Flat, break-annotated character sequence — see {@link CharacterBox}. */
public record DocumentTextLayout(List<CharacterBox> characters) {}
