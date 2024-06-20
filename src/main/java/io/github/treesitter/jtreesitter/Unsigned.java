package io.github.treesitter.jtreesitter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the value is of an unsigned data type.
 *
 * @see Integer#compareUnsigned
 * @see Integer#toUnsignedString
 * @see Short#compareUnsigned
 * @see Short#toUnsignedInt
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface Unsigned {}
